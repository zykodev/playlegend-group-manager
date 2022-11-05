package net.playlegend.groupmanager.listener;

import net.playlegend.groupmanager.GroupManager;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.Group;
import net.playlegend.groupmanager.model.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.logging.Level;

public class PlayerChatListener implements Listener {

  @EventHandler
  public void handleAsyncPlayerChatEvent(AsyncPlayerChatEvent asyncPlayerChatEvent) {
    try {
      User user = UserDao.getUser(asyncPlayerChatEvent.getPlayer().getUniqueId());
      if(user != null) {
        Group group = user.getGroup();
        if (group == null) {
          group = GroupManager.getInstance().getDefaultGroup();
        }
        String prefix = group.getPrefix();
        String message =
                prefix
                        + asyncPlayerChatEvent.getPlayer().getName()
                        + ChatColor.DARK_GRAY
                        + ": "
                        + ChatColor.GRAY
                        + asyncPlayerChatEvent.getMessage();
        if (message.contains("%")) {
          Bukkit.broadcastMessage(message);
          asyncPlayerChatEvent.setCancelled(true);
          return;
        }
        asyncPlayerChatEvent.setFormat(message);
      } else {
        throw new IllegalStateException("Player online but not in database");
      }
    } catch (Exception e) {
      GroupManager.getInstance().log(Level.WARNING, "Failed to handle chat message for player " + asyncPlayerChatEvent.getPlayer(), e);
    }
  }
}
