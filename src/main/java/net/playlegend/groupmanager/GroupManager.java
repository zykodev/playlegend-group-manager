package net.playlegend.groupmanager;

import lombok.Getter;
import net.playlegend.groupmanager.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.HibernateError;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
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

  @Getter private EntityManagerFactory entityManagerFactory;

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
    Properties hibernateProperties =
        FileUtil.loadPropertiesFromFile(new File("plugins/GroupManager/hibernate.properties"));
    this.entityManagerFactory =
        Persistence.createEntityManagerFactory("persistence-unit", hibernateProperties);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (this.entityManagerFactory == null || !this.entityManagerFactory.isOpen())
                    return;
                  this.entityManagerFactory.close();
                }));
  }

  @Override
  public void onDisable() {}
}
