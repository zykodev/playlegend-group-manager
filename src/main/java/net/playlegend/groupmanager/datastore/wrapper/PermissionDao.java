package net.playlegend.groupmanager.datastore.wrapper;

import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.DataAccessException;
import net.playlegend.groupmanager.model.Permission;
import net.playlegend.groupmanager.model.Permission_;

import java.util.List;

public class PermissionDao {

  /**
   * Returns the permission object of a given permission string.
   *
   * @param permission the permission string to look for
   * @return the permission object associated with the permission string or null if not found
   * @throws DataAccessException if there is a database error
   */
  public static Permission getPermission(String permission) throws DataAccessException {
    List<Permission> result =
        Dao.forType(Permission.class)
            .find(
                (rootObject, criteriaBuilder, output) ->
                    output.add(
                        criteriaBuilder.equal(rootObject.get(Permission_.PERMISSION), permission)));
    if (result.isEmpty()) return null;
    return result.get(0);
  }

  /**
   * Creates a permission object using a given permission string.
   *
   * @param permission the permission string to use for creation
   * @return the permission object created or null if already present
   * @throws DataAccessException if there is a database error
   */
  public static Permission createPermission(String permission) throws DataAccessException {
    if (PermissionDao.getPermission(permission) == null) {
      Permission permissionObject = new Permission();
      permissionObject.setPermission(permission);
      Dao.forType(Permission.class).put(permissionObject);
      return permissionObject;
    } else {
      return null;
    }
  }

  /**
   * Creates or fetches the permission object of a given permission string.
   *
   * @param permission the permission string to look for/create
   * @return the permission object (to be) associated with the permission string
   * @throws DataAccessException if there is a database error
   */
  public static Permission getOrCreatePermission(String permission) throws DataAccessException {
    Permission permissionObject = PermissionDao.createPermission(permission);
    if (permissionObject == null) permissionObject = PermissionDao.getPermission(permission);
    return permissionObject;
  }
}
