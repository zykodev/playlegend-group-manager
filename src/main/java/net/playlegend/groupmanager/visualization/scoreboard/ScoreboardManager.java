package net.playlegend.groupmanager.visualization.scoreboard;

import lombok.Getter;
import net.playlegend.groupmanager.GroupManager;
import net.playlegend.groupmanager.datastore.DataAccessException;
import net.playlegend.groupmanager.datastore.wrapper.GroupDao;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.Group;
import net.playlegend.groupmanager.model.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class ScoreboardManager {

  @Getter private Scoreboard pluginScoreboard;

  /**
   * Rebuilds the plugin scoreboard used for tab list sorting and colored names. Time: O(n + m)
   * where n is the number of groups and m is the number of online players. -> Linear
   */
  public void rebuildPluginScoreboard() {
    Bukkit.getScheduler()
        .runTask(
            GroupManager.getInstance(),
            () -> {
              this.pluginScoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();

              Bukkit.getScheduler()
                  .runTaskAsynchronously(
                      GroupManager.getInstance(),
                      () -> {
                        try {
                          List<Group> allGroups = GroupDao.getAllGroups();
                          if (allGroups != null) {
                            Bukkit.getScheduler()
                                .runTask(
                                    GroupManager.getInstance(),
                                    () -> {
                                      for (Group group : allGroups) {
                                        Team team =
                                            this.pluginScoreboard.getTeam(
                                                this.patchPriority(group.getPriority())
                                                    + group.getName());
                                        if (team != null) team.unregister();
                                        team =
                                            this.pluginScoreboard.registerNewTeam(
                                                this.patchPriority(group.getPriority())
                                                    + group.getName());
                                        team.setPrefix(group.getPrefix());
                                        team.setDisplayName(group.getName());
                                        team.setOption(
                                            Team.Option.NAME_TAG_VISIBILITY,
                                            Team.OptionStatus.ALWAYS);
                                        team.setColor(this.getLastColor(group.getPrefix()));
                                      }
                                      for (Player player : Bukkit.getOnlinePlayers()) {
                                        this.addPlayerToScoreboard(this.pluginScoreboard, player);
                                      }
                                      this.updateScoreboardForAllPlayers();
                                    });
                          } else {
                            throw new IllegalStateException("No group data present");
                          }
                        } catch (DataAccessException | IllegalStateException e) {
                          GroupManager.getInstance()
                              .log(Level.WARNING, "Failed to rebuild plugin scoreboard.", e);
                        }
                      });
            });
  }

  public void updateScoreboardForAllPlayers() {
    Bukkit.getOnlinePlayers().forEach(this::updateScoreboard);
  }

  public void updateScoreboard(Player player) {
    player.setScoreboard(this.pluginScoreboard);
  }

  /**
   * Adds a player to the plugin scoreboard and refreshes it for every player.
   *
   * @param scoreboard the scoreboard to which to add the player
   * @param player the player to add
   */
  public void addPlayerToScoreboard(Scoreboard scoreboard, Player player) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            GroupManager.getInstance(),
            () -> {
              try {
                User userData = UserDao.getUser(player.getUniqueId());
                if (userData != null) {
                  Group group = userData.getGroup();
                  Bukkit.getScheduler()
                      .runTask(
                          GroupManager.getInstance(),
                          () -> {
                            Team team =
                                scoreboard.getTeam(
                                    this.patchPriority(group.getPriority()) + group.getName());
                            if (team == null) {
                              this.rebuildPluginScoreboard();
                              this.addPlayerToScoreboard(this.pluginScoreboard, player);
                              return;
                            }
                            team.addEntry(player.getName());
                            player.setDisplayName(team.getPrefix() + player.getName());
                          });

                } else {
                  throw new IllegalStateException("User not in database, but is online");
                }
              } catch (DataAccessException | IllegalStateException e) {
                GroupManager.getInstance()
                    .log(Level.WARNING, "Failed to rebuild scoreboard plugin.", e);
              }
            });
  }

  /**
   * Reformats single digit numbers like '5' or '7' to numbers like '05' and '07' in order to keep
   * the tab list sorted properly.
   *
   * @param priority the number to reformat
   * @return the reformatted number as a string
   */
  private String patchPriority(int priority) {
    return priority < 10 ? "0" + priority : priority + "";
  }

  private ChatColor getLastColor(String s) {
    int lastIndex = s.lastIndexOf('§');
    if (lastIndex == -1) return ChatColor.RESET;
    return ChatColor.getByChar(s.charAt(lastIndex + 1));
  }
}
