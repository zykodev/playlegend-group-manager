package net.playlegend.groupmanager.datastore.wrapper;

import jakarta.persistence.criteria.Expression;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.DataAccessException;
import net.playlegend.groupmanager.model.User;
import net.playlegend.groupmanager.model.User_;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserDao {

  /**
   * Searches for user data using a given UUID.
   *
   * @param uuid the UUID of the searched user data, same as the associated players' UUID
   * @return the corresponding user data or null if not found
   * @throws DataAccessException if there is an exception when communicating with the database.
   */
  public static User getUser(UUID uuid) throws DataAccessException {
    List<User> playerUserData =
        Dao.forType(User.class)
            .find(
                (rootObject, criteriaBuilder, output) ->
                    output.add(criteriaBuilder.equal(rootObject.get(User_.UUID), uuid)));
    if (playerUserData.isEmpty()) return null;
    return playerUserData.get(0);
  }

  /**
   * Collects all online players' user data using one query. Generally recommended instead of
   * calling {@code getUser(onlinePlayer.getUniqueId())} over and over again.
   *
   * @return the corresponding user data or null if not found
   * @throws DataAccessException if there is an exception when communicating with the database.
   */
  public static List<User> getOnlineUsers() throws DataAccessException {
    List<UUID> onlineUuids =
        Bukkit.getOnlinePlayers().stream().map(Entity::getUniqueId).collect(Collectors.toList());
    List<User> playerUserData =
        Dao.forType(User.class)
            .find(
                (rootObject, criteriaBuilder, output) -> {
                  Expression<UUID> uuidExp = rootObject.get(User_.UUID);
                  output.add(uuidExp.in(onlineUuids));
                });
    if (onlineUuids.isEmpty()) return new ArrayList<>();
    if (playerUserData.isEmpty()) return null;
    return playerUserData;
  }

  /**
   * Searches for user data using a given name.
   *
   * @param name the name associated with the searched user data, usually the same as the associated
   *     players' name if he has rejoined after renaming.
   * @return the corresponding user data or null if not found
   * @throws DataAccessException if there is an exception when communicating with the database.
   */
  public static User getUser(String name) throws DataAccessException {
    List<User> playerUserData =
        Dao.forType(User.class)
            .find(
                (rootObject, criteriaBuilder, output) ->
                    output.add(criteriaBuilder.equal(rootObject.get(User_.NAME), name)));
    if (playerUserData.isEmpty()) return null;
    return playerUserData.get(0);
  }
}
