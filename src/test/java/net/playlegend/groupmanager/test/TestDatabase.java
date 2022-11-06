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
public class TestDatabase {

  private ServerMock server;
  private GroupManagerPlugin groupManagerPlugin;
  private File hibernateConfigFile;

  @BeforeAll
  public void prepareTest() throws Exception {
    this.extractHibernateConfig();
    this.server = MockBukkit.mock();
    this.groupManagerPlugin = MockBukkit.load(GroupManagerPlugin.class);
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
    hibernateConfig.put("hibernate.show_sql", "true");
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
    group.setPrefix(groupPrefix + "2");
    group = Dao.forType(Group.class).update(group);
    Assertions.assertEquals(group.getPrefix(), groupPrefix + "2", "Group prefix mismatch. (2)");

    String playerName = "TestPlayer";
    UUID playerId = UUID.randomUUID();
    User user = new User();
    user.setName(playerName);
    user.setUuid(playerId);
    user.setGroup(GroupDao.getGroup("default"));
    user.setGroupValidUntil(-1);
    Dao.forType(User.class).put(user);

    this.server.dispatchCommand(
        Bukkit.getConsoleSender(), "gmuser group " + user.getName() + " " + group.getName());
    Thread.sleep(150);
    user = UserDao.getUser(playerName);
    Assertions.assertEquals(user.getGroup().getName(), group.getName(), "Group user mismatch.");
    user.setGroup(GroupDao.getGroup("default"));
    user = Dao.forType(User.class).update(user);

    Dao.forType(Group.class).delete(group);
    group = GroupDao.getGroup(groupName);
    Assertions.assertNull(group, "Group was not deleted.");
  }

  @AfterAll
  public void callCleaningCrew() {
    this.hibernateConfigFile.getParentFile().getParentFile().delete();
    this.server.shutdown();
  }
}
