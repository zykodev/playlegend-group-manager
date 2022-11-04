package net.playlegend.groupmanager;

import jakarta.persistence.Entity;
import lombok.Getter;
import net.playlegend.groupmanager.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.HibernateError;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin written for the PlayLegend.net application process.
 *
 * @author Lenny (zykodev)
 */
public class GroupManager extends JavaPlugin {

  /*
   * Required for Hibernate setup. (See: https://www.spigotmc.org/threads/setup-jpa-hibernate-for-your-minecraft-plugin.397782/)
   */
  static {
    Thread.currentThread().setContextClassLoader(GroupManager.class.getClassLoader());
  }

  @Getter private final Logger logger = Logger.getLogger(this.getClass().getName());

  @Getter private static GroupManager instance;

  @Getter private SessionFactory sessionFactory;

  @Getter
  private final String prefix =
      ChatColor.RED.toString()
          + ChatColor.BOLD
          + "Group Management "
          + ChatColor.DARK_GRAY
          + "| "
          + ChatColor.RESET
          + ChatColor.GRAY;

  @Override
  public void onEnable() {
    instance = this;

    try {
      this.setupHibernate();
    } catch (IOException e) {
      this.logger.log(
          Level.SEVERE, "Failed to create Hibernate storage backend. Cannot continue.", e);
      Bukkit.shutdown();
    }
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
    this.logger.log(Level.INFO, "Setting up Hibernate backend...");
    Properties hibernateProperties =
        FileUtil.loadPropertiesFromFile(new File("plugins/GroupManager/hibernate.properties"));
    Configuration configuration = new Configuration();
    configuration.setProperties(hibernateProperties);
    Reflections reflections =
        new Reflections("net.playlegend.groupmanager.model", Scanners.TypesAnnotated);
    reflections.getTypesAnnotatedWith(Entity.class).forEach(configuration::addAnnotatedClass);
    this.sessionFactory = configuration.buildSessionFactory();
    this.logger.log(Level.INFO, "Hibernate setup successful.");
  }

  @Override
  public void onDisable() {
    this.endHibernate();
  }

  /** Shuts down the Hibernate backend. */
  private void endHibernate() {
    if (this.sessionFactory == null || !this.sessionFactory.isOpen()) return;
    this.logger.log(Level.INFO, "Shutting down Hibernate backend...");
    this.sessionFactory.close();
    this.logger.log(Level.INFO, "Hibernate backend shut down.");
  }
}
