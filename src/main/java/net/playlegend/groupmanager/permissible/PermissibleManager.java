package net.playlegend.groupmanager.permissible;

import lombok.Getter;
import net.playlegend.groupmanager.datastore.DataAccessException;
import net.playlegend.groupmanager.datastore.wrapper.GroupDao;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.Group;
import net.playlegend.groupmanager.model.Permission;
import net.playlegend.groupmanager.model.User;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;
import java.util.*;

public class PermissibleManager {

  @Getter
  private final Map<Group, Set<Permission>> cachedGroupPermissions =
      Collections.synchronizedMap(new HashMap<>());

  @Getter
  private final Map<UUID, Group> cachedPlayerGroups = Collections.synchronizedMap(new HashMap<>());

  private Field permissibleField;

  public void createCaches() throws DataAccessException {
    this.cachedPlayerGroups.clear();
    this.cachedGroupPermissions.clear();
    List<Group> groupList = GroupDao.getAllGroups();
    for (Group g : Objects.requireNonNull(groupList)) {
      Set<Permission> groupPerms = g.getPermissions();
      cachedGroupPermissions.put(g, groupPerms);
    }
    List<User> onlineUsers = UserDao.getOnlineUsers();
    for (User user : Objects.requireNonNull(onlineUsers)) {
      this.cacheUserData(user);
    }
  }

  public void cacheUserData(User user) {
    cachedGroupPermissions.remove(user.getGroup());
    cachedGroupPermissions.put(user.getGroup(), user.getGroup().getPermissions());
    cachedPlayerGroups.remove(user.getUuid());
    cachedPlayerGroups.put(user.getUuid(), user.getGroup());
  }

  public void injectPermissible(Player player)
      throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
    Class<?> craftPlayerClass = player.getClass();
    Class<?> humanEntityClass =
        Class.forName(craftPlayerClass.getPackage().getName() + ".CraftHumanEntity");
    if (permissibleField == null) {
      this.permissibleField = humanEntityClass.getDeclaredField("perm");
      this.permissibleField.setAccessible(true);
    }
    PermissibleBase currentPermissible = (PermissibleBase) this.permissibleField.get(player);
    if (currentPermissible instanceof GmPermissible) return;
    GmPermissible gmPermissible = new GmPermissible(player);
    this.permissibleField.set(player, gmPermissible);
  }
}
