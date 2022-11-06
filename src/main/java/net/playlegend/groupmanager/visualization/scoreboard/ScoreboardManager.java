package net.playlegend.groupmanager.visualization.scoreboard;

import com.google.common.collect.Maps;
import lombok.Getter;
import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.datastore.wrapper.GroupDao;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.Group;
import net.playlegend.groupmanager.model.User;
import net.playlegend.groupmanager.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Takes care of everything tab-list related such as managing scoreboard teams and apply them to
 * players.
 */
public class ScoreboardManager {

  @Getter
  private final ConcurrentHashMap<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();

  /**
   * Updates the scoreboard for every online users. Caution: This method has a time complexity
   * higher than n^2. Also, there is one scoreboard per player containing every online player and
   * every existing group, resulting in a space complexity of over n^2 as well.
   */
  public void updateScoreboards() {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            GroupManagerPlugin.getInstance(),
            () -> {
              try {
                List<User> onlineUserData = UserDao.getOnlineUsers();
                List<Group> groupData = GroupDao.getAllGroups();
                if (onlineUserData != null && groupData != null) {
                  for (User user : onlineUserData) {
                    Player player = Bukkit.getPlayer(user.getUuid());
                    if (player == null) continue;
                    Bukkit.getScheduler()
                        .runTask(
                            GroupManagerPlugin.getInstance(),
                            () -> {
                              // Create scoreboard base
                              Scoreboard scoreboard;
                              if (this.playerScoreboards.containsKey(player.getUniqueId())) {
                                scoreboard = this.playerScoreboards.get(player.getUniqueId());
                              } else {
                                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                                this.playerScoreboards.put(player.getUniqueId(), scoreboard);
                              }
                              for (Group g : groupData) {
                                Team team =
                                    scoreboard.getTeam(
                                        this.patchPriority(g.getPriority()) + g.getName());
                                if (team == null) {
                                  team =
                                      scoreboard.registerNewTeam(
                                          this.patchPriority(g.getPriority()) + g.getName());
                                } else {
                                  for (String s : team.getEntries()) {
                                    team.removeEntry(s);
                                  }
                                }
                                for (User tempUserData : onlineUserData) {
                                  Player tempPlayer = Bukkit.getPlayer(tempUserData.getUuid());
                                  if (tempPlayer == null) continue;
                                  if (tempUserData.getGroup().getId().equals(g.getId())) {
                                    team.addEntry(tempPlayer.getName());
                                  }
                                }
                                team.setPrefix(g.getPrefix());
                                team.setColor(this.getLastColor(g.getPrefix()));
                              }

                              // Create sidebar base
                              HashMap<String, String> replacements = Maps.newHashMap();
                              replacements.put("%player%", user.getName());
                              replacements.put("%id%", user.getUuid().toString());
                              replacements.put("%group%", user.getGroup().getName());
                              replacements.put("%prefix%", user.getGroup().getPrefix());
                              CommandUtil.insertDurationReplacement(user, player, replacements);
                              Objective playerSidebar = scoreboard.getObjective("playerObjective");
                              if (playerSidebar == null) {
                                playerSidebar =
                                    scoreboard.registerNewObjective(
                                        "playerObjective",
                                        Criteria.DUMMY,
                                        GroupManagerPlugin.getInstance()
                                            .getTextManager()
                                            .getMessage(
                                                player, "gm.sidebar.line.title", replacements));
                                playerSidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
                              }

                              // Create sidebar lines
                              int lineScore = 14;
                              for (int i = 0; i < 15; i++) {
                                String name = this.getLineEntryName(i);
                                Team sidebarTeam = scoreboard.getTeam(name);
                                if (sidebarTeam == null) {
                                  sidebarTeam = scoreboard.registerNewTeam(name);
                                }
                                String lineText =
                                    GroupManagerPlugin.getInstance()
                                        .getTextManager()
                                        .getMessage(player, "gm.sidebar.line." + i, replacements);
                                if (lineText.length() > 32) {
                                  lineText = lineText.substring(0, 32);
                                }
                                if (lineText.length() > 0) {
                                  if (lineText.length() > 16) {
                                    String prefix = lineText.substring(0, 16);
                                    boolean colorFlag = prefix.endsWith("§");
                                    String suffix =
                                        this.getLastColor(prefix)
                                            + (colorFlag ? "§" : "")
                                            + lineText.substring(16);
                                    if (colorFlag) {
                                      prefix = prefix.substring(0, prefix.length() - 1);
                                      if (suffix.length() > 16) {
                                        suffix = suffix.substring(0, 16);
                                      }
                                    }
                                    sidebarTeam.setPrefix(prefix);
                                    sidebarTeam.setSuffix(suffix);
                                  } else {
                                    sidebarTeam.setPrefix(lineText);
                                    sidebarTeam.setSuffix("");
                                  }

                                  sidebarTeam.addEntry(name);
                                  playerSidebar.getScore(name).setScore(lineScore);
                                  lineScore--;
                                } else {
                                  sidebarTeam.removeEntry(name);
                                }
                              }
                              player.setScoreboard(scoreboard);
                            });
                  }
                }
              } catch (Exception e) {
                GroupManagerPlugin.getInstance()
                    .log(Level.WARNING, "Failed to rebuild scoreboards", e);
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

  /**
   * Returns the last color, not format, used in a string.
   *
   * @param s the string to search through
   * @return either a valid color code or RESET (§r) if no ChatColor was present or the last used
   *     color code was invalid, §y for example.
   */
  private ChatColor getLastColor(String s) {
    int lastIndex = s.lastIndexOf('§');
    if (lastIndex == -1) return ChatColor.RESET;
    if (lastIndex + 1 > s.length() - 1) return ChatColor.RESET;
    ChatColor color = ChatColor.getByChar(s.charAt(lastIndex + 1));
    if (color == null) return ChatColor.RESET;
    if (color.isFormat()) {
      return this.getLastColor(s.substring(0, lastIndex));
    }
    return color;
  }

  /**
   * Returns the local line name, by mapping an index to it.
   *
   * @param index the name index
   * @return the name associated with the given index
   */
  private String getLineEntryName(int index) {
    char[] names =
        new char[] {'1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'r'};
    return "§" + names[index];
  }
}
