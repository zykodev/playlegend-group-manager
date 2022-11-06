package net.playlegend.groupmanager.permissible;

import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.model.Group;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Proxy object to intercept calls to hasPermission. */
public class GmPermissible extends PermissibleBase {

  private Player associatedPlayer;

  private PermissibleBase oldPermissible;

  public GmPermissible(PermissibleBase oldPermissible) {
    super(oldPermissible);
    this.oldPermissible = oldPermissible;
  }

  public GmPermissible(@Nullable ServerOperator opable) {
    super(opable);
  }

  public GmPermissible(Player associatedPlayer) {
    super(null);
    this.associatedPlayer = associatedPlayer;
  }

  @Override
  public boolean hasPermission(@NotNull String inName) {
    Group group =
        GroupManagerPlugin.getInstance()
            .getPermissibleManager()
            .getCachedPlayerGroups()
            .get(this.associatedPlayer.getUniqueId());
    if (group != null) {
      Map<String, Boolean> groupPermissionCache =
          GroupManagerPlugin.getInstance()
              .getPermissibleManager()
              .getCachedGroupCheckedPermissions()
              .computeIfAbsent(group, k -> new HashMap<>());

      // check if cached and if cached, return the cached value
      if (groupPermissionCache.containsKey(inName)) {
        return groupPermissionCache.get(inName);
      }
      Set<net.playlegend.groupmanager.model.Permission> permissionsSet =
          GroupManagerPlugin.getInstance()
              .getPermissibleManager()
              .getCachedGroupPermissions()
              .get(group);
      Set<net.playlegend.groupmanager.model.Permission> defaultPermissionsSet =
          GroupManagerPlugin.getInstance()
              .getPermissibleManager()
              .getCachedGroupPermissions()
              .get(GroupManagerPlugin.getInstance().getDefaultGroup());
      Set<net.playlegend.groupmanager.model.Permission> combinedSet = new HashSet<>(permissionsSet);
      combinedSet.addAll(defaultPermissionsSet);

      // check for star permission up front
      if (this.isPermissionPresent(combinedSet, "*")) {
        groupPermissionCache.put(inName, true);
        return true;
      }

      // no one can have an empty permission
      if (inName.length() == 0) return false;

      // check against the raw permission
      if (this.isPermissionPresent(combinedSet, inName)) {
        groupPermissionCache.put(inName, true);
        return true;
      }

      // check against possible wildcard permission
      if (inName.contains(".")) {
        String[] parts = inName.split("\\.");
        for (int i = 0; i < parts.length; i++) {
          StringBuilder result = new StringBuilder();
          for (int j = 0; j < i; j++) {
            result.append(parts[j]).append(".");
          }
          result.append("*");
          if (this.isPermissionPresent(combinedSet, result.toString())) {
            groupPermissionCache.put(inName, true);
            return true;
          }
        }
      }
      groupPermissionCache.put(inName, false);
    }
    return false;
  }

  @Override
  public boolean hasPermission(@NotNull Permission perm) {
    return this.hasPermission(perm.getName());
  }

  private boolean isPermissionPresent(
      Set<net.playlegend.groupmanager.model.Permission> permissionSet, String permission) {
    for (net.playlegend.groupmanager.model.Permission p : permissionSet) {
      if (permission.equalsIgnoreCase(p.getPermission())) {
        return true;
      }
    }
    return false;
  }
}
