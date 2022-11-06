package net.playlegend.groupmanager.permissible;

import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.model.Group;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Proxy object to intercept calls to hasPermission.
 */
public class GmPermissible extends PermissibleBase {

  private Player associatedPlayer;

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
      Set<net.playlegend.groupmanager.model.Permission> permissionsSet =
          GroupManagerPlugin.getInstance()
              .getPermissibleManager()
              .getCachedGroupPermissions()
              .get(group);
      if (inName.length() == 0) return false;
      if (this.isPermissionPresent(permissionsSet, "*")) return true;
      if (this.isPermissionPresent(permissionsSet, inName)) return true;
      if (inName.contains(".")) {
        String[] parts = inName.split("\\.");
        for (int i = 0; i < parts.length; i++) {
          StringBuilder result = new StringBuilder();
          for (int j = 0; j < i; j++) {
            result.append(parts[j]).append(".");
          }
          result.append("*");
          if (this.isPermissionPresent(permissionsSet, result.toString())) return true;
        }
      }
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
