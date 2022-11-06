package net.playlegend.groupmanager.visualization.sign;

import com.google.common.collect.Lists;
import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.DataAccessException;
import net.playlegend.groupmanager.datastore.wrapper.RankSignDao;
import net.playlegend.groupmanager.model.RankSign;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class SignManager {

  private final List<RankSign> rankSigns = Collections.synchronizedList(Lists.newArrayList());
  private volatile boolean signsLoaded = false;

  /**
   * Reloads sign data from database.
   *
   * @throws DataAccessException if something goes wrong trying to receive sign information from
   *     database
   */
  public void reloadSigns() throws DataAccessException {
    this.rankSigns.clear();
    List<RankSign> rankSigns = RankSignDao.getAllSigns();
    if (rankSigns != null) {
      this.rankSigns.addAll(rankSigns);
    }
    this.updateSigns();
  }

  /**
   * Updates all locally cached rank signs.
   *
   * @throws DataAccessException if something goes wrong trying to receive sign information from
   *     database
   */
  public void updateSigns() throws DataAccessException {
    if (!this.signsLoaded) {
      Bukkit.getScheduler()
          .runTaskAsynchronously(
              GroupManagerPlugin.getInstance(),
              () -> {
                try {
                  this.reloadSigns();
                  signsLoaded = true;
                } catch (DataAccessException e) {
                  GroupManagerPlugin.getInstance()
                      .log(Level.WARNING, "Failed to reload signs from database.", e);
                }
              });
      return;
    }
    for (RankSign rankSign : this.rankSigns) {
      World world = Bukkit.getWorld(rankSign.getWorld());
      if (world != null) {
        Location location =
            new Location(world, rankSign.getPosX(), rankSign.getPosY(), rankSign.getPosZ());
        double closestDistance = this.getClosestPlayerDistance(location);
        if (closestDistance > 25) continue;
        Bukkit.getScheduler()
            .runTask(
                GroupManagerPlugin.getInstance(),
                () -> updateRankSignAtLocation(rankSign, location));

      } else {
        this.removeSign(rankSign);
      }
    }
  }

  /**
   * Updates a rank sign at a given location.
   *
   * @param rankSign the rank sign to update
   * @param location the location of the rank sign
   */
  private void updateRankSignAtLocation(RankSign rankSign, Location location) {
    if (location.getBlock().getState() instanceof Sign) {
      Sign sign = (Sign) location.getBlock().getState();
      sign.setLine(0, ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "--------");
      sign.setLine(1, ChatColor.WHITE + rankSign.getUser().getName());
      sign.setLine(
          2, ChatColor.WHITE + "" + ChatColor.BOLD + rankSign.getUser().getGroup().getName());
      sign.setLine(3, ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "--------");
      sign.update(true);
    } else {
      this.removeSignAsync(rankSign);
    }
  }

  /**
   * Deletes rank sign asynchronously.
   *
   * @param rankSign the rank sign to delete
   */
  private void removeSignAsync(RankSign rankSign) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            GroupManagerPlugin.getInstance(),
            () -> {
              try {
                this.removeSign(rankSign);
              } catch (Exception e) {
                GroupManagerPlugin.getInstance()
                    .log(Level.WARNING, "Failed to delete rank sign", e);
              }
            });
  }

  /**
   * Deletes rank sign.
   *
   * @param rankSign the rank sign to delete
   * @throws DataAccessException if something goes wrong trying to receive sign information from
   *     database
   */
  public void removeSign(RankSign rankSign) throws DataAccessException {
    rankSign.getUser().getRankSigns().remove(rankSign);
    Dao.forType(RankSign.class).delete(rankSign);
    this.rankSigns.remove(rankSign);
    GroupManagerPlugin.getInstance().rebuildEverything();
  }

  private double getClosestPlayerDistance(Location location) {
    double distance = Double.MAX_VALUE;
    for (Player player : Bukkit.getOnlinePlayers()) {
      double playerDistance = player.getLocation().distance(location);
      if (playerDistance < distance) distance = playerDistance;
    }
    return distance;
  }
}
