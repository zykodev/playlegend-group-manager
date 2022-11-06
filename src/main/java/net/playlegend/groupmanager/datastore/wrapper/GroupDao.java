package net.playlegend.groupmanager.datastore.wrapper;

import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.DataAccessException;
import net.playlegend.groupmanager.model.Group;
import net.playlegend.groupmanager.model.Group_;

import java.util.List;

public class GroupDao {

  /**
   * Returns the group object using the specified group name.
   *
   * @param groupName the group name to look for
   * @return the associated group object or null if not found
   * @throws DataAccessException if there is a database error
   */
  public static Group getGroup(String groupName) throws DataAccessException {
    List<Group> matchingGroups =
        Dao.forType(Group.class)
            .find(
                (rootObject, criteriaBuilder, output) ->
                    output.add(criteriaBuilder.equal(rootObject.get(Group_.NAME), groupName)));
    if (matchingGroups.isEmpty()) return null;
    return matchingGroups.get(0);
  }

  /**
   * Returns all groups from the database.
   *
   * @return a list of all groups found in the database or null if none are found
   * @throws DataAccessException if there is a database error
   */
  public static List<Group> getAllGroups() throws DataAccessException {
    List<Group> matchingGroups =
        Dao.forType(Group.class).find((rootObject, criteriaBuilder, output) -> {});
    if (matchingGroups.isEmpty()) return null;
    return matchingGroups;
  }

  /**
   * Deletes a group without touching associated users. Note: Group deletion is a possibly slow
   * process for groups with many members because of a time complexity of O(n + m) where n is the
   * amount of players in the group and m is the amount of permissions the group has.
   *
   * @param group the group to delete
   * @throws DataAccessException if there is a database error
   */
  public static void deleteGroup(Group group) throws DataAccessException {
    group
        .getUsers()
        .forEach(
            u -> {
              u.setGroup(GroupManagerPlugin.getInstance().getDefaultGroup());
              u.setGroupValidUntil(-1);
            });
    group.getPermissions().forEach(p -> p.getGroups().remove(group));
    group.getPermissions().clear();
    group.getUsers().clear();
    Dao.forType(Group.class).delete(group);
  }
}
