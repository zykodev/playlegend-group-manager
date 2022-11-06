package net.playlegend.groupmanager.command;

import com.google.common.collect.Maps;
import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.datastore.DataAccessException;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.User;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;

public class RankInfoCommand implements CommandExecutor {

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      if (player.hasPermission("gm.rankinfo")) {
        if (args.length == 0) {
          Bukkit.getScheduler()
              .runTaskAsynchronously(
                  GroupManagerPlugin.getInstance(),
                  () -> {
                    try {
                      User user = UserDao.getUser(player.getUniqueId());
                      if (user != null) {
                        HashMap<String, String> replacements = Maps.newHashMap();
                        replacements.put("%group%", user.getGroup().getName());
                        replacements.put("%prefix%", user.getGroup().getPrefix());
                        replacements.put("%player%", player.getName());
                        if (user.getGroupValidUntil() == -1) {
                          replacements.put(
                              "%duration%",
                              GroupManagerPlugin.getInstance()
                                  .getTextManager()
                                  .getMessage(player, "gm.user.group.duration.infinite", null));
                        } else {
                          String format =
                              GroupManagerPlugin.getInstance()
                                  .getTextManager()
                                  .getMessage(player, "gm.user.group.duration.format", null);
                          DateTimeFormatter formatter = DateTimeFormat.forPattern(format);
                          String formattedTime = formatter.print(user.getGroupValidUntil());
                          replacements.put("%duration%", formattedTime);
                        }
                        GroupManagerPlugin.getInstance()
                            .getTextManager()
                            .sendMessage(sender, "gm.rankinfo.heading", replacements);
                        GroupManagerPlugin.getInstance()
                            .getTextManager()
                            .sendMessage(sender, "gm.rankinfo.rank", replacements);
                        GroupManagerPlugin.getInstance()
                            .getTextManager()
                            .sendMessage(sender, "gm.rankinfo.duration", replacements);
                      } else {
                        throw new IllegalStateException(
                            "User not known in database, but is executing commands");
                      }
                    } catch (DataAccessException | IllegalStateException e) {
                      e.printStackTrace();
                    }
                  });
        } else {
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.rankinfo.help", null);
        }
      } else {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.nopermission", null);
      }
    } else {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.error.invalidcommandsender", null);
    }
    return true;
  }
}
