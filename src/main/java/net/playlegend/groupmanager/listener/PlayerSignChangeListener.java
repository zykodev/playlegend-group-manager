package net.playlegend.groupmanager.listener;

import net.playlegend.groupmanager.GroupManager;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.RankSign;
import net.playlegend.groupmanager.model.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Objects;
import java.util.logging.Level;

public class PlayerSignChangeListener implements Listener {

  @EventHandler
  public void handleSignChangeEvent(SignChangeEvent e) {
    if (e.getBlock().getState() instanceof Sign) {
      if (Objects.requireNonNull(e.getLine(0)).equalsIgnoreCase("[gm:sign]")) {
        String playerName = e.getLine(1);
        Bukkit.getScheduler()
            .runTaskAsynchronously(
                GroupManager.getInstance(),
                () -> {
                  try {
                    User user = UserDao.getUser(playerName);
                    if (user != null) {
                      RankSign rankSign = new RankSign();
                      rankSign.setWorld(e.getBlock().getWorld().getName());
                      rankSign.setPosX(e.getBlock().getX());
                      rankSign.setPosY(e.getBlock().getY());
                      rankSign.setPosZ(e.getBlock().getZ());
                      rankSign.setUser(user);
                      Dao.forType(RankSign.class).put(rankSign);
                      GroupManager.getInstance().getSignManager().reloadSigns();
                    } else {
                      Bukkit.getScheduler()
                          .runTask(
                              GroupManager.getInstance(), () -> e.getBlock().setType(Material.AIR));
                    }
                  } catch (Exception ex) {
                    GroupManager.getInstance()
                        .log(
                            Level.WARNING,
                            "Failed to create rank sign at " + e.getBlock().getLocation(),
                            ex);
                  }
                });
      }
    }
  }
}
