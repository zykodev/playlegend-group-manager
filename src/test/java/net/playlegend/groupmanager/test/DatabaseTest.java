package net.playlegend.groupmanager.test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.wrapper.GroupDao;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.Group;
import net.playlegend.groupmanager.model.User;
import net.playlegend.groupmanager.util.FileUtil;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;
import java.util.UUID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseTest {

  private ServerMock server;
  private GroupManagerPlugin groupManagerPlugin;
  private File hibernateConfigFile;

  @BeforeAll
  public void prepareTest() throws Exception {
    System.out.println("Test preparation...");
    this.extractHibernateConfig();
    this.server = MockBukkit.mock();
    this.groupManagerPlugin = MockBukkit.load(GroupManagerPlugin.class);
    System.out.println("Done!");
  }

  public void extractHibernateConfig() throws Exception {
    Properties hibernateConfig = new Properties();
    hibernateConfig.put("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
    hibernateConfig.put(
        "hibernate.connection.url",
        "jdbc:mysql://192.168.2.45:3306/gmanagertest?autoReconnect=true"); // CHANGE ACCORDINGLY
    // !!!!!
    hibernateConfig.put("hibernate.connection.username", "gmanager");
    hibernateConfig.put("hibernate.connection.password", "gmanager");
    hibernateConfig.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
    hibernateConfig.put("hibernate.hbm2ddl.auto", "update");
    hibernateConfig.put("hibernate.show_sql", "false");
    new File("plugins").mkdir();
    this.hibernateConfigFile = new File(FileUtil.PLUGIN_ROOT_DIRECTORY, "hibernate.properties");
    if (hibernateConfigFile.exists()) hibernateConfigFile.delete();
    hibernateConfigFile.createNewFile();
    hibernateConfig.store(new FileWriter(hibernateConfigFile), "Hibernate Test Configuration");
  }

  @Test
  public void testDatabase() throws Exception {
    System.out.println("Test!");
    String groupName = "TestGroup";
    String groupPrefix = "TestPrefix";
    Group testGroup = new Group();
    testGroup.setName(groupName);
    testGroup.setPrefix(groupPrefix);
    Dao.forType(Group.class).put(testGroup);
    Group group = GroupDao.getGroup(groupName);
    Assertions.assertNotNull(group, "Group is null.");
    Assertions.assertEquals(group.getName(), groupName, "Group name mismatch.");
    Assertions.assertEquals(group.getPrefix(), groupPrefix, "Group prefix mismatch.");
    System.out.println("Group creation test successful.");

    group.setPrefix(groupPrefix + "2");
    group = Dao.forType(Group.class).update(group);
    Assertions.assertEquals(group.getPrefix(), groupPrefix + "2", "Group prefix mismatch. (2)");
    System.out.println("Group update test successful.");

    String playerName = "TestPlayer";
    UUID playerId = UUID.randomUUID();
    User user = new User();
    user.setName(playerName);
    user.setUuid(playerId);
    user.setGroup(GroupDao.getGroup("default"));
    user.setGroupValidUntil(-1);
    Dao.forType(User.class).put(user);
    System.out.println("Player creation test successful.");

    this.server.dispatchCommand(
        Bukkit.getConsoleSender(), "gmuser group " + user.getName() + " " + group.getName());
    Thread.sleep(150);
    user = UserDao.getUser(playerName);
    Assertions.assertEquals(user.getGroup().getName(), group.getName(), "Group user mismatch.");
    System.out.println("Player async group change test successful.");

    user.setGroup(GroupDao.getGroup("default"));
    user = Dao.forType(User.class).update(user);
    System.out.println("Player group change test successful.");

    group.getUsers().clear();
    Dao.forType(Group.class).delete(group);
    group = GroupDao.getGroup(groupName);
    Assertions.assertNull(group, "Group was not deleted.");
    System.out.println("Group delete test successful.");

    user.getGroup().getUsers().remove(user);
    Dao.forType(Group.class).update(user.getGroup());
    user.setGroup(null);
    user = Dao.forType(User.class).update(user);
    Assertions.assertNull(user.getGroup(), "User group was not detached.");
    System.out.println("User group detach test successful.");

    Dao.forType(User.class).delete(user);
    user = UserDao.getUser(playerId);
    Assertions.assertNull(user, "User was not deleted.");
    System.out.println("User delete test successful.");

    group = GroupDao.getGroup("default");
    Dao.forType(Group.class).delete(group);
    group = GroupDao.getGroup("default");
    Assertions.assertNull(group, "Default group was not deleted.");
    System.out.println("Group delete test successful. (2)");
  }

  @AfterAll
  public void callCleaningCrew() {
    this.hibernateConfigFile.getParentFile().getParentFile().delete();
  }
}
