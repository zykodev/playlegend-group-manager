package net.playlegend.groupmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import lombok.Getter;
import net.playlegend.groupmanager.command.GmGroupCommand;
import net.playlegend.groupmanager.command.GmUserCommand;
import net.playlegend.groupmanager.command.RankInfoCommand;
import net.playlegend.groupmanager.config.GroupManagerConfig;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.DataAccessException;
import net.playlegend.groupmanager.listener.PlayerChatListener;
import net.playlegend.groupmanager.listener.PlayerConnectionListener;
import net.playlegend.groupmanager.listener.PlayerSignChangeListener;
import net.playlegend.groupmanager.model.Group;
import net.playlegend.groupmanager.model.Group_;
import net.playlegend.groupmanager.permissible.PermissibleManager;
import net.playlegend.groupmanager.tasks.TaskGroupValidityCheck;
import net.playlegend.groupmanager.tasks.TaskRebuild;
import net.playlegend.groupmanager.tasks.TaskSignUpdate;
import net.playlegend.groupmanager.text.TextManager;
import net.playlegend.groupmanager.util.FileUtil;
import net.playlegend.groupmanager.visualization.scoreboard.ScoreboardManager;
import net.playlegend.groupmanager.visualization.sign.SignManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitTask;
import org.hibernate.HibernateError;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin written for the PlayLegend.net application process.
 * This class provides a singleton instance to use for interaction with this plugin.
 *
 * @author Lenny (zykodev)
 */
public class GroupManagerPlugin extends JavaPlugin {

  /*
   * Required for Hibernate setup. (See: https://www.spigotmc.org/threads/setup-jpa-hibernate-for-your-minecraft-plugin.397782/)
   */
  static {
    Thread.currentThread().setContextClassLoader(GroupManagerPlugin.class.getClassLoader());
  }

  @Getter private final Logger logger = Logger.getLogger(this.getClass().getName());

  @Getter private static GroupManagerPlugin instance;

  @Getter private SessionFactory sessionFactory;

  @Getter private Group defaultGroup;

  @Getter private TextManager textManager;

  @Getter private ScoreboardManager scoreboardManager;

  @Getter private SignManager signManager;

  @Getter private BukkitTask rebuildTask;

  @Getter private BukkitTask signUpdateTask;

  @Getter private BukkitTask groupValidCheckTask;

  @Getter private PermissibleManager permissibleManager;

  @Getter private GroupManagerConfig groupManagerConfig;

  @Getter
  private final String prefix =
      ChatColor.RED.toString()
          + ChatColor.BOLD
          + "GroupManagement "
          + ChatColor.DARK_GRAY
          + "\u00BB "
          + ChatColor.RESET
          + ChatColor.GRAY;

  public GroupManagerPlugin() {
    super();
  }

  /**
   * Required for and only for mocking purposes when testing. (// undocumented)
   */
  public GroupManagerPlugin(
      JavaPluginLoader loader, PluginDescriptionFile desc, File dataFolder, File file) {
    super(loader, desc, dataFolder, file);
  }

  @Override
  public void onEnable() {
    instance = this;

    try {
      FileUtil.extractDefaultResources();
      ObjectMapper objectMapper = new ObjectMapper();
      this.groupManagerConfig =
          objectMapper.readValue(
              new File(FileUtil.PLUGIN_ROOT_DIRECTORY, "config.json"), GroupManagerConfig.class);
    } catch (IOException e) {
      this.log(Level.WARNING, "Failed to extract default resources and load config.", e);
    }

    try {
      this.setupHibernate();
      this.checkDefaultGroup();
    } catch (IOException | DataAccessException e) {
      this.log(Level.SEVERE, "Failed to create Hibernate storage backend. Cannot continue.", e);
      Bukkit.shutdown();
    }

    this.textManager = new TextManager();
    this.textManager.loadLocales(this.groupManagerConfig.getFallbackLocale());

    this.registerCommands();
    this.registerListeners();

    this.scoreboardManager = new ScoreboardManager();
    this.scoreboardManager.rebuildPluginScoreboard();

    this.signManager = new SignManager();

    this.startSignUpdateTask();
    this.startGroupValidityCheckTask();
    this.startRebuildTask();

    this.permissibleManager = new PermissibleManager();
  }

  @Override
  public void onDisable() {
    Bukkit.getScheduler().cancelTask(this.rebuildTask.getTaskId());
    Bukkit.getScheduler().cancelTask(this.signUpdateTask.getTaskId());
    Bukkit.getScheduler().cancelTask(this.groupValidCheckTask.getTaskId());
    this.endHibernate();
  }

  /** Registers commands. */
  private void registerCommands() {
    Objects.requireNonNull(Bukkit.getPluginCommand("gmgroup")).setExecutor(new GmGroupCommand());
    Objects.requireNonNull(Bukkit.getPluginCommand("rankinfo")).setExecutor(new RankInfoCommand());
    Objects.requireNonNull(Bukkit.getPluginCommand("gmuser")).setExecutor(new GmUserCommand());
  }

  /** Registers event listeners. */
  private void registerListeners() {
    PluginManager pluginManager = Bukkit.getPluginManager();
    pluginManager.registerEvents(new PlayerConnectionListener(), this);
    pluginManager.registerEvents(new PlayerChatListener(), this);
    pluginManager.registerEvents(new PlayerSignChangeListener(), this);
  }

  /**
   * Sets up the Hibernate backend. Expects a hibernate.properties file containing valid k/v-pairs
   * specifying Hibernate configuration parameters. (Location:
   * plugins/GroupManager/hibernate.properties)
   *
   * @throws IOException if the "hibernate.properties" file is missing or malformed
   * @throws HibernateError if the given properties are invalid
   */
  private void setupHibernate() throws IOException, HibernateError {
    this.log(Level.INFO, "Setting up Hibernate backend...");
    Properties hibernateProperties =
        FileUtil.loadPropertiesFromFile(new File("plugins/GroupManager/hibernate.properties"));
    Configuration configuration = new Configuration();
    configuration.setProperties(hibernateProperties);
    Reflections reflections =
        new Reflections("net.playlegend.groupmanager.model", Scanners.TypesAnnotated);
    reflections.getTypesAnnotatedWith(Entity.class).forEach(configuration::addAnnotatedClass);
    this.sessionFactory = configuration.buildSessionFactory();
    this.log(Level.INFO, "Hibernate setup successful.");
  }

  /** Shuts down the Hibernate backend. */
  private void endHibernate() {
    if (this.sessionFactory == null || !this.sessionFactory.isOpen()) return;
    this.log(Level.INFO, "Shutting down Hibernate backend...");
    Dao.destroyDaoCache();
    this.sessionFactory.close();
    this.log(Level.INFO, "Hibernate backend shut down.");
  }

  /**
   * Checks whether the default group exists and if it does not exist, it creates one.
   *
   * @throws DataAccessException if there is an error during database read/manipulation
   */
  private void checkDefaultGroup() throws DataAccessException {
    List<Group> defaultGroupResult =
        Dao.forType(Group.class)
            .find(
                (rootObject, criteriaBuilder, output) ->
                    output.add(criteriaBuilder.equal(rootObject.get(Group_.NAME), "default")));
    if (defaultGroupResult.isEmpty()) {
      Group defaultGroup = new Group();
      defaultGroup.setName("default");
      Dao.forType(Group.class).put(defaultGroup);
      this.log(Level.INFO, "Initialized default group.");
      this.defaultGroup = defaultGroup;
    } else {
      this.defaultGroup = defaultGroupResult.get(0);
    }
  }

  /**
   * Can be used to force clearing all caches, regenerate the scoreboard, etc. This is called
   * automatically every 5 minutes and once for every data manipulation.
   */
  public void rebuildEverything() {
    Dao.destroyDaoCache();
    this.scoreboardManager.rebuildPluginScoreboard();
    try {
      this.signManager.reloadSigns();
      this.permissibleManager.createCaches();
    } catch (DataAccessException e) {
      this.log(Level.WARNING, "Failed to reload signs from database.", e);
    }
  }

  /**
   * Logs a message to console.
   *
   * @param level the level this message corresponds with
   * @param message the actual message
   * @param exception possibly an exception in order to print errors to console
   */
  public void log(Level level, String message, @Nullable Throwable exception) {
    message = ChatColor.stripColor(this.prefix) + message;
    if (exception != null) {
      this.logger.log(level, message, exception);
      return;
    }
    this.logger.log(level, message);
  }

  /**
   * Logs a message to console.
   *
   * @param level the level this message corresponds with
   * @param message the actual message
   */
  public void log(Level level, String message) {
    this.log(level, message, null);
  }

  /**
   * Starts the rebuild task thread. Flushes all caches and rebuilds scoreboard, etc. Runs once
   * every 5 minutes.
   */
  private void startRebuildTask() {
    this.rebuildTask =
        Bukkit.getScheduler()
            .runTaskTimer(
                this, new TaskRebuild(), 0, this.groupManagerConfig.getCacheRebuildInterval());
  }

  /**
   * Starts the sign update thread. Runs once every 5 seconds and updates all rank signs. (Also,
   * deletes the ones that are not present anymore.)
   */
  private void startSignUpdateTask() {
    this.signUpdateTask =
        Bukkit.getScheduler()
            .runTaskTimerAsynchronously(
                this, new TaskSignUpdate(), 0, this.groupManagerConfig.getSignUpdateInterval());
  }

  /**
   * Starts the group validity check thread. Runs once every 5 seconds and updates the rank for
   * every online player, in case it is expired. If it actually is expired, the user will be reset
   * to the default group.
   */
  private void startGroupValidityCheckTask() {
    this.groupValidCheckTask =
        Bukkit.getScheduler()
            .runTaskTimerAsynchronously(
                this,
                new TaskGroupValidityCheck(),
                0,
                this.groupManagerConfig.getGroupValidityCheckInterval());
  }
}
