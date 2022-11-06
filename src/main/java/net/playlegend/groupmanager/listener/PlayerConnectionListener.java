package net.playlegend.groupmanager.listener;

import com.google.common.collect.Maps;
import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.logging.Level;

public class PlayerConnectionListener implements Listener {

  @EventHandler
  public void handlePlayerJoinEvent(PlayerJoinEvent playerJoinEvent) {
    try {
      GroupManagerPlugin.getInstance()
          .getPermissibleManager()
          .injectPermissible(playerJoinEvent.getPlayer());
    } catch (Exception e) {
      GroupManagerPlugin.getInstance()
          .log(
              Level.WARNING,
              "Failed to inject permissible for player " + playerJoinEvent.getPlayer().getName());
    }
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            GroupManagerPlugin.getInstance(),
            () -> {
              User user = this.updatePlayerUserData(playerJoinEvent.getPlayer());
              if (user != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                  String locale = player.getLocale();
                  HashMap<String, String> replacements = Maps.newHashMap();
                  replacements.put("%player%", user.getName());
                  replacements.put("%prefix%", user.getGroup().getPrefix());
                  playerJoinEvent.setJoinMessage(null);
                  player.sendMessage(
                      GroupManagerPlugin.getInstance()
                          .getTextManager()
                          .getMessage(locale, "gm.join", replacements));
                }
                GroupManagerPlugin.getInstance()
                    .getScoreboardManager()
                    .addPlayerToScoreboard(
                        GroupManagerPlugin.getInstance()
                            .getScoreboardManager()
                            .getPluginScoreboard(),
                        playerJoinEvent.getPlayer(),
                        user);
                GroupManagerPlugin.getInstance().getPermissibleManager().cacheUserData(user);
              }
            });
    GroupManagerPlugin.getInstance()
        .getScoreboardManager()
        .updateScoreboard(playerJoinEvent.getPlayer());
  }

  @EventHandler
  public void handlePlayerQuitEvent(PlayerQuitEvent playerQuitEvent) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            GroupManagerPlugin.getInstance(),
            () -> {
              User user = this.updatePlayerUserData(playerQuitEvent.getPlayer());
              if (user != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                  String locale = player.getLocale();
                  HashMap<String, String> replacements = Maps.newHashMap();
                  replacements.put("%player%", user.getName());
                  replacements.put("%prefix%", user.getGroup().getPrefix());
                  playerQuitEvent.setQuitMessage(null);
                  player.sendMessage(
                      GroupManagerPlugin.getInstance()
                          .getTextManager()
                          .getMessage(locale, "gm.quit", replacements));
                }
              }
            });
  }

  /**
   * Updates a players' user data.
   *
   * @param player the player whose data is to be updated
   * @return the updated user data
   */
  private User updatePlayerUserData(Player player) {
    try {
      User userData = UserDao.getUser(player.getUniqueId());
      if (userData != null) {
        userData.setName(player.getName());
        if (userData.getGroup() == null) {
          userData.setGroup(GroupManagerPlugin.getInstance().getDefaultGroup());
        }
        Dao.forType(User.class).update(userData);
      } else {
        userData = new User();
        userData.setUuid(player.getUniqueId());
        userData.setName(player.getName());
        userData.setGroup(GroupManagerPlugin.getInstance().getDefaultGroup());
        Dao.forType(User.class).put(userData);
      }
      return userData;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
