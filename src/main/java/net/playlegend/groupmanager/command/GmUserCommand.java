package net.playlegend.groupmanager.command;

import lombok.Getter;
import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.wrapper.GroupDao;
import net.playlegend.groupmanager.datastore.wrapper.UserDao;
import net.playlegend.groupmanager.model.Group;
import net.playlegend.groupmanager.model.User;
import net.playlegend.groupmanager.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;

public class GmUserCommand implements CommandExecutor {

  public enum SubCommand {
    GROUP("gm.user.group");

    @Getter private final String permission;

    SubCommand(String permission) {
      this.permission = permission;
    }
  }

  private final PeriodFormatter periodFormatter;

  public GmUserCommand() {
    this.periodFormatter =
        new PeriodFormatterBuilder()
            .appendDays()
            .appendSuffix("d ", "d")
            .appendHours()
            .appendSuffix("h ", "h")
            .appendMinutes()
            .appendSuffix("m ", "m")
            .appendSeconds()
            .appendSuffix("s")
            .toFormatter();
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      if (args.length == 0) {
        if (player.hasPermission("gm.user.help")) {
          this.displayCommandHelp(player);
        } else {
          this.displayNoPermissions(player);
        }
      } else {
        String subCommandRaw = args[0];
        SubCommand subCommand = null;
        try {
          subCommand = SubCommand.valueOf(subCommandRaw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
          this.displayCommandHelp(sender);
        }
        if (subCommand != null) {
          if (player.hasPermission(subCommand.getPermission())) {
            this.handleSubCommand(sender, args, subCommand);
          } else {
            this.displayNoPermissions(player);
          }
        }
      }
    } else {
      if (args.length == 0) {
        this.displayCommandHelp(sender);
      } else {
        String subCommandRaw = args[0];
        SubCommand subCommand = null;
        try {
          subCommand = SubCommand.valueOf(subCommandRaw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
          this.displayCommandHelp(sender);
        }
        if (subCommand != null) {
          this.handleSubCommand(sender, args, subCommand);
        }
      }
    }
    return true;
  }

  private void handleSubCommand(
      @NotNull CommandSender sender, @NotNull String[] args, SubCommand subCommand) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            GroupManagerPlugin.getInstance(),
            () -> {
              if (subCommand == SubCommand.GROUP) {
                this.executeGroup(sender, args);
              }
            });
  }

  private void displayNoPermissions(CommandSender commandSender) {
    GroupManagerPlugin.getInstance()
        .getTextManager()
        .sendMessage(commandSender, "gm.error.nopermission", null);
  }

  private void displayCommandHelp(CommandSender commandSender) {
    GroupManagerPlugin.getInstance()
        .getTextManager()
        .sendMessage(commandSender, "gm.user.help.heading", null);
    for (SubCommand subCommand : GmUserCommand.SubCommand.values()) {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(commandSender, "gm.user.help." + subCommand.name().toLowerCase(), null);
    }
  }

  private void executeGroup(CommandSender sender, String[] args) {
    if (args.length >= 2) {
      String playerName = args[1];
      try {
        User user = UserDao.getUser(playerName);
        if (user != null) {
          if (args.length == 2) {
            this.displayUserInfo(sender, user);
          } else {
            this.updateUserGroup(sender, user, args);
          }
        } else {
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%player%", playerName);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.user.error.userdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        GroupManagerPlugin.getInstance().log(Level.WARNING, "Failed to retrieve user data.", e);
      }

    } else {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.user.help.group", null);
    }
  }

  private void displayUserInfo(CommandSender commandSender, User user) {
    HashMap<String, String> replacements = new HashMap<>();
    replacements.put("%prefix%", user.getGroup().getPrefix());
    replacements.put("%player%", user.getName());
    replacements.put("%group%", user.getGroup().getName());

    CommandUtil.insertDurationReplacement(user, commandSender, replacements);

    GroupManagerPlugin.getInstance()
        .getTextManager()
        .sendMessage(commandSender, "gm.user.group.info.heading", replacements);
    GroupManagerPlugin.getInstance()
        .getTextManager()
        .sendMessage(commandSender, "gm.user.group.info.group", replacements);
    GroupManagerPlugin.getInstance()
        .getTextManager()
        .sendMessage(commandSender, "gm.user.group.info.duration", replacements);
  }

  private void updateUserGroup(CommandSender commandSender, User user, String[] args) {
    String groupName = args[2];
    if (args.length == 3) {
      try {
        Group group = GroupDao.getGroup(groupName);
        if (group != null) {
          user.setGroup(group);
          user.setGroupValidUntil(-1);
          Dao.forType(User.class).update(user);
          this.displayUserInfo(commandSender, user);
          GroupManagerPlugin.getInstance().rebuildEverything();
        } else {
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%group%", groupName);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(commandSender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(commandSender, "gm.error.internalerror", null);
        GroupManagerPlugin.getInstance().log(Level.WARNING, "Failed to retrieve group data.", e);
      }
    } else {
      String durationString = CommandUtil.combineStringsInArray(args, 3, args.length - 1);
      long duration;
      try {
        duration =
            this.periodFormatter.parsePeriod(durationString).toStandardDuration().getMillis();
      } catch (IllegalArgumentException e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(commandSender, "gm.user.error.wrongtimeformat", null);
        return;
      }
      try {
        Group group = GroupDao.getGroup(groupName);
        if (group != null) {
          user.setGroup(group);
          user.setGroupValidUntil(System.currentTimeMillis() + duration);
          Dao.forType(User.class).update(user);
          this.displayUserInfo(commandSender, user);
          GroupManagerPlugin.getInstance().rebuildEverything();
        } else {
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%group%", groupName);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(commandSender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(commandSender, "gm.error.internalerror", null);
        GroupManagerPlugin.getInstance().log(Level.WARNING, "Failed to retrieve group data.", e);
      }
    }
  }
}
