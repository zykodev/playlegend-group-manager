package net.playlegend.groupmanager.command;

import com.google.common.collect.Maps;
import lombok.Getter;
import net.playlegend.groupmanager.GroupManager;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.model.Group;
import net.playlegend.groupmanager.model.Group_;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class GmGroupCommand implements CommandExecutor {

  public enum SubCommand {
    CREATE("gm.group.create"),
    INFO("gm.group.info"),
    PERMADD("gm.group.permadd"),
    USERADD("gm.group.useradd"),
    PERMDEL("gm.group.permdel"),
    USERDEL("gm.group.userdel"),
    PREFIX("gm.group.prefix"),
    PRIORITY("gm.group.priority"),
    DELETE("gm.group.delete");

    @Getter private final String permission;

    SubCommand(String permission) {
      this.permission = permission;
    }
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
        if (player.hasPermission("gm.group.help")) {
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
            GroupManager.getInstance(),
            () -> {
              switch (subCommand) {
                case CREATE:
                  this.executeCreate(sender, args);
                  break;
                case INFO:
                  this.executeInfo(sender, args);
                  break;
                case PERMADD:
                  this.executePermAdd(sender, args);
                  break;
                case USERADD:
                  this.executeUserAdd(sender, args);
                  break;
                case PERMDEL:
                  this.executePermDel(sender, args);
                  break;
                case USERDEL:
                  this.executeUserDel(sender, args);
                  break;
                case PREFIX:
                  this.executePrefix(sender, args);
                  break;
                case PRIORITY:
                  this.executePriority(sender, args);
                  break;
                case DELETE:
                  this.executeDelete(sender, args);
                  break;
                default:
                  break;
              }
            });
  }

  private void displayNoPermissions(CommandSender commandSender) {
    String playerLocale = GroupManager.getInstance().getTextManager().getLocale(commandSender);
    String message =
        GroupManager.getInstance()
            .getTextManager()
            .getMessage(playerLocale, "gm.error.nopermission", null);
    commandSender.sendMessage(GroupManager.getInstance().getPrefix() + message);
  }

  private void displayCommandHelp(CommandSender commandSender) {
    String playerLocale = GroupManager.getInstance().getTextManager().getLocale(commandSender);
    String message =
        GroupManager.getInstance()
            .getTextManager()
            .getMessage(playerLocale, "gm.group.help.heading", null);
    commandSender.sendMessage(GroupManager.getInstance().getPrefix() + message);
    for (SubCommand subCommand : GmGroupCommand.SubCommand.values()) {
      message =
          GroupManager.getInstance()
              .getTextManager()
              .getMessage(playerLocale, "gm.group.help." + subCommand.name().toLowerCase(), null);
      commandSender.sendMessage(GroupManager.getInstance().getPrefix() + message);
    }
  }

  private void executeCreate(CommandSender sender, String[] args) {
    if (args.length >= 3) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      try {
        String[] prefixArgs = new String[args.length - 2];
        System.arraycopy(args, 2, prefixArgs, 0, prefixArgs.length);
        StringBuilder prefixBuilder = new StringBuilder();
        for (String prefixArg : prefixArgs) {
          prefixBuilder.append(prefixArg).append(" ");
        }
        String prefix = prefixBuilder.toString();
        prefix = prefix.replace('&', '§');
        if (prefix.length() > 16) {
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.prefixtoolong", null);
          return;
        }
        List<Group> matchingGroups =
            Dao.forType(Group.class)
                .find(
                    (rootObject, criteriaBuilder, output) -> output.add(criteriaBuilder.equal(rootObject.get(Group_.NAME), groupName)));
        if (matchingGroups.isEmpty()) {
          Group group = new Group();
          group.setName(groupName);
          group.setPrefix(prefix);
          Dao.forType(Group.class).put(group);
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%group%", group.getName());
          replacements.put("%prefix%", group.getPrefix());
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.create.success", replacements);
        } else {
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%group%", groupName);
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupexists", replacements);
        }
      } catch (Exception e) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManager.getInstance().getTextManager().sendMessage(sender, "gm.group.help.create", null);
    }
  }

  private void executePermAdd(CommandSender sender, String[] args) {}

  private void executeUserAdd(CommandSender sender, String[] args) {}

  private void executePermDel(CommandSender sender, String[] args) {}

  private void executeUserDel(CommandSender sender, String[] args) {}

  private void executePrefix(CommandSender sender, String[] args) {
    if (args.length >= 2) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      try {
        String[] prefixArgs = new String[args.length - 2];
        System.arraycopy(args, 2, prefixArgs, 0, prefixArgs.length);
        StringBuilder prefixBuilder = new StringBuilder();
        for (String prefixArg : prefixArgs) {
          prefixBuilder.append(prefixArg).append(" ");
        }
        String prefix = prefixBuilder.toString();
        prefix = prefix.replace('&', '§');
        if (prefix.length() > 16) {
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.prefixtoolong", null);
          return;
        }
        List<Group> matchingGroups =
            Dao.forType(Group.class)
                .find(
                    (rootObject, criteriaBuilder, output) -> output.add(criteriaBuilder.equal(rootObject.get(Group_.NAME), groupName)));
        if (!matchingGroups.isEmpty()) {
          Group group = matchingGroups.get(0);
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%group%", group.getName());
          if (args.length >= 3) {
            group.setPrefix(prefix);
            Dao.forType(Group.class).update(group);
            replacements.put("%prefix%", prefix);
            GroupManager.getInstance()
                .getTextManager()
                .sendMessage(sender, "gm.group.prefix.edit", replacements);
          } else {
            replacements.put("%prefix%", group.getPrefix());
            GroupManager.getInstance()
                .getTextManager()
                .sendMessage(sender, "gm.group.prefix.show", replacements);
          }
        } else {
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%group%", groupName);
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManager.getInstance().getTextManager().sendMessage(sender, "gm.group.help.prefix", null);
    }
  }

  private void executePriority(CommandSender sender, String[] args) {
    if (args.length >= 2 && args.length < 4) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      try {
        List<Group> matchingGroups =
            Dao.forType(Group.class)
                .find(
                    (rootObject, criteriaBuilder, output) -> output.add(criteriaBuilder.equal(rootObject.get(Group_.NAME), groupName)));
        if (!matchingGroups.isEmpty()) {
          Group group = matchingGroups.get(0);
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%group%", group.getName());
          if (args.length == 3) {
            int priority;
            try {
              priority = Integer.parseInt(args[2]);
              if (priority < 0 || priority > 99) throw new IllegalArgumentException();
            } catch (Exception e) {
              GroupManager.getInstance()
                  .getTextManager()
                  .sendMessage(sender, "gm.group.error.priorityinvalid", null);
              return;
            }
            group.setPriority(priority);
            Dao.forType(Group.class).update(group);
            replacements.put("%priority%", "" + priority);
            GroupManager.getInstance()
                .getTextManager()
                .sendMessage(sender, "gm.group.priority.edit", replacements);
          } else {
            replacements.put("%priority%", "" + group.getPriority());
            GroupManager.getInstance()
                .getTextManager()
                .sendMessage(sender, "gm.group.priority.show", replacements);
          }
        } else {
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%group%", groupName);
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManager.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.group.help.priority", null);
    }
  }

  private void executeDelete(CommandSender sender, String[] args) {
    if (args.length == 2) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      if (groupName.equals("default")) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.cannotmodifydefault", null);
        return;
      }
      try {
        List<Group> matchingGroups =
            Dao.forType(Group.class)
                .find(
                    (rootObject, criteriaBuilder, output) -> output.add(criteriaBuilder.equal(rootObject.get(Group_.NAME), groupName)));
        HashMap<String, String> replacements = Maps.newHashMap();
        replacements.put("%group%", groupName);
        if (!matchingGroups.isEmpty()) {
          Group group = matchingGroups.get(0);
          Dao.forType(Group.class).delete(group);
          replacements.put("%group%", group.getName());
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.delete.success", replacements);
        } else {
          replacements.put("%group%", groupName);
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManager.getInstance().getTextManager().sendMessage(sender, "gm.group.help.delete", null);
    }
  }

  private void executeInfo(CommandSender sender, String[] args) {
    if (args.length == 2) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      try {
        List<Group> matchingGroups =
            Dao.forType(Group.class)
                .find(
                    (rootObject, criteriaBuilder, output) -> output.add(criteriaBuilder.equal(rootObject.get(Group_.NAME), groupName)));
        if (!matchingGroups.isEmpty()) {
          Group group = matchingGroups.get(0);
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%group%", group.getName());
          replacements.put("%prefix%", group.getPrefix());
          replacements.put("%priority%", group.getPriority() + "");
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.info.heading", replacements);
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.info.prefix", replacements);
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.info.priority", replacements);
        } else {
          HashMap<String, String> replacements = new HashMap<>();
          replacements.put("%group%", groupName);
          GroupManager.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManager.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManager.getInstance().getTextManager().sendMessage(sender, "gm.group.help.info", null);
    }
  }
}
