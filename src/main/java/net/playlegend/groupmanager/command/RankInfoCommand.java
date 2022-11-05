package net.playlegend.groupmanager.command;

import com.google.common.collect.Maps;
import net.playlegend.groupmanager.GroupManager;
import net.playlegend.groupmanager.datastore.DataAccessException;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.User;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
                  GroupManager.getInstance(),
                  () -> {
                    try {
                      User user = UserDao.getUser(player.getUniqueId());
                      if (user != null) {
                        HashMap<String, String> replacements = Maps.newHashMap();
                        replacements.put("%group%", user.getGroup().getName());
                        replacements.put("%prefix%", user.getGroup().getPrefix());
                        replacements.put("%player%", player.getName());
                        GroupManager.getInstance()
                            .getTextManager()
                            .sendMessage(sender, "gm.rankinfo.heading", replacements);
                        GroupManager.getInstance()
                            .getTextManager()
                            .sendMessage(sender, "gm.rankinfo.rank", replacements);
                      } else {
                        throw new IllegalStateException(
                            "User not known in database, but is executing commands");
                      }
                    } catch (DataAccessException | IllegalStateException e) {
                      e.printStackTrace();
                    }
                  });
        } else {
          GroupManager.getInstance().getTextManager().sendMessage(sender, "gm.rankinfo.help", null);
        }
      } else {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.nopermission", null);
      }
    } else {
      GroupManager.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.error.invalidcommandsender", null);
    }
    return true;
  }
}
