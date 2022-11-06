package net.playlegend.groupmanager.permissible;

import lombok.Getter;
import net.playlegend.groupmanager.GroupManagerPlugin;
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
import java.util.logging.Level;

/** Takes care of caching user/group permissions and permissible injection. */
public class PermissibleManager {

  @Getter
  private final Map<Group, Set<Permission>> cachedGroupPermissions =
      Collections.synchronizedMap(new HashMap<>());

  @Getter
  private final Map<Group, Map<String, Boolean>> cachedGroupCheckedPermissions =
      Collections.synchronizedMap(new HashMap<>());

  @Getter
  private final Map<UUID, Group> cachedPlayerGroups = Collections.synchronizedMap(new HashMap<>());

  private Field permissibleField;

  /**
   * Creates/Recreates all caches. Inherently complex.
   *
   * @throws DataAccessException if the cached could not be constructed due to database error
   */
  public void createCaches() throws DataAccessException {
    this.cachedPlayerGroups.clear();
    this.cachedGroupPermissions.clear();
    this.cachedGroupCheckedPermissions.clear();
    List<Group> groupList = GroupDao.getAllGroups();
    if (groupList == null) return;
    for (Group g : Objects.requireNonNull(groupList)) {
      Set<Permission> groupPerms = g.getPermissions();
      cachedGroupPermissions.put(g, groupPerms);
    }
    List<User> onlineUsers = UserDao.getOnlineUsers();
    for (User user : Objects.requireNonNull(onlineUsers)) {
      this.cacheUserData(user);
    }
    GroupManagerPlugin.getInstance().log(Level.INFO, "Permissions caches rebuilt.");
  }

  /**
   * Caches a users' data to reduce complexity when actually checking permissions.
   *
   * @param user the user whose data is to be cached
   */
  public void cacheUserData(User user) {
    cachedGroupPermissions.remove(user.getGroup());
    cachedGroupPermissions.put(user.getGroup(), user.getGroup().getPermissions());
    cachedPlayerGroups.remove(user.getUuid());
    cachedPlayerGroups.put(user.getUuid(), user.getGroup());
  }

  /**
   * Injects a permissible proxy object into a players CraftHumanEntity in order to intercept
   * #hasPermission() invocations.
   *
   * @param player the player to inject into
   * @throws ClassNotFoundException if the CraftHumanEntity class could not be found -> possibly
   *     look for a more generic approach
   * @throws NoSuchFieldException if the permissible field inside CraftHumanEntity could not be
   *     found. -> possibly look for a more generic approach
   * @throws IllegalAccessException if the permissible field could not be accessed
   */
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
