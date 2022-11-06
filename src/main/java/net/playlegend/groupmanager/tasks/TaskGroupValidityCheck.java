package net.playlegend.groupmanager.tasks;

import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.User;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.logging.Level;

public class TaskGroupValidityCheck implements Runnable {

  @Override
  public void run() {
    try {
      List<User> onlineUsers = UserDao.getOnlineUsers();
      if (onlineUsers != null) {
        if (onlineUsers.size() != Bukkit.getOnlinePlayers().size()) {
          GroupManagerPlugin.getInstance()
              .log(
                  Level.WARNING,
                  "Missing data for at least one online player",
                  new IllegalStateException("Player is online but not in database"));
        }
        for (User user : onlineUsers) {
          if (user != null) {
            if (user.getGroupValidUntil() < System.currentTimeMillis()
                && user.getGroupValidUntil() > 0) {
              user.getGroup().getUsers().remove(user);
              user.setGroup(GroupManagerPlugin.getInstance().getDefaultGroup());
              user.setGroupValidUntil(-1);
              Dao.forType(User.class).update(user);
            }
          } else {
            throw new IllegalStateException("User online but not in database");
          }
        }
      }
    } catch (Exception e) {
      GroupManagerPlugin.getInstance().log(Level.WARNING, "Failed to refresh group validity.", e);
    }
  }
}
