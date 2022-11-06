package net.playlegend.groupmanager.command;

import com.google.common.collect.Maps;
import lombok.Getter;
import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.datastore.Dao;
import net.playlegend.groupmanager.datastore.wrapper.GroupDao;
import net.playlegend.groupmanager.datastore.wrapper.PermissionDao;
import net.playlegend.groupmanager.model.Group;
import net.playlegend.groupmanager.model.Permission;
import net.playlegend.groupmanager.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class GmGroupCommand implements CommandExecutor {

  public enum SubCommand {
    CREATE("gm.group.create"),
    INFO("gm.group.info"),
    PERMADD("gm.group.permadd"),
    PERMDEL("gm.group.permdel"),
    PREFIX("gm.group.prefix"),
    PRIORITY("gm.group.priority"),
    DELETE("gm.group.delete"),
    LIST("gm.group.list");

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
            GroupManagerPlugin.getInstance(),
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
                case PERMDEL:
                  this.executePermDel(sender, args);
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
                case LIST:
                  this.executeList(sender, args);
                  break;
                default:
                  break;
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
        .sendMessage(commandSender, "gm.group.help.heading", null);
    for (SubCommand subCommand : GmGroupCommand.SubCommand.values()) {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(commandSender, "gm.group.help." + subCommand.name().toLowerCase(), null);
    }
  }

  private void executeList(CommandSender sender, String[] args) {
    if (args.length == 1) {
      try {
        List<Group> groups = GroupDao.getAllGroups();
        if (groups != null) {
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.list.heading", null);
          for (Group group : groups) {
            HashMap<String, String> replacements = Maps.newHashMap();
            replacements.put("%group%", group.getName());
            GroupManagerPlugin.getInstance()
                .getTextManager()
                .sendMessage(sender, "gm.group.list.entry", replacements);
          }
        } else {
          throw new IllegalStateException("No groups present but player online");
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        GroupManagerPlugin.getInstance().log(Level.WARNING, "Failed to list groups", e);
      }
    } else {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.group.help.list", null);
    }
  }

  private void executeCreate(CommandSender sender, String[] args) {
    if (args.length >= 3) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      try {
        String prefix = CommandUtil.combineStringsInArray(args, 2, args.length - 1) + " ";
        prefix = prefix.replace('&', '§');
        if (ChatColor.stripColor(prefix).equalsIgnoreCase(" ")) prefix = prefix.trim();
        if (prefix.length() > 16) {
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.prefixtoolong", null);
          return;
        }
        Group group = GroupDao.getGroup(groupName);
        HashMap<String, String> replacements = new HashMap<>();
        if (group == null) {
          group = new Group();
          group.setName(groupName);
          group.setPrefix(prefix);
          Dao.forType(Group.class).put(group);
          replacements.put("%group%", group.getName());
          replacements.put("%prefix%", group.getPrefix());
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.create.success", replacements);

        } else {
          replacements.put("%group%", groupName);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupexists", replacements);
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        GroupManagerPlugin.getInstance().log(Level.WARNING, "Failed to create group", e);
      }
    } else {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.group.help.create", null);
    }
  }

  private void executePermAdd(CommandSender sender, String[] args) {
    if (args.length == 3) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      try {
        String permissionString = args[2];
        Permission permission = PermissionDao.getOrCreatePermission(permissionString);
        Group group = GroupDao.getGroup(groupName);
        HashMap<String, String> replacements = Maps.newHashMap();
        if (group != null) {
          replacements.put("%group%", group.getName());
          replacements.put("%permission%", permission.getPermission());
          for (Permission groupPermission : group.getPermissions()) {
            if (groupPermission.getPermission().equalsIgnoreCase(permission.getPermission())) {
              GroupManagerPlugin.getInstance()
                  .getTextManager()
                  .sendMessage(sender, "gm.group.error.permissionalreadyset", replacements);
              return;
            }
          }
          group.getPermissions().add(permission);
          Dao.forType(Group.class).update(group);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.permadd.success", replacements);

        } else {
          replacements.put("%group%", groupName);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.group.help.permadd", null);
    }
  }

  private void executePermDel(CommandSender sender, String[] args) {
    if (args.length == 3) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      try {
        String permissionString = args[2];
        Permission permission = PermissionDao.getOrCreatePermission(permissionString);
        Group group = GroupDao.getGroup(groupName);
        HashMap<String, String> replacements = new HashMap<>();
        if (group != null) {
          replacements.put("%group%", group.getName());
          replacements.put("%permission%", permission.getPermission());
          for (Permission groupPermission : group.getPermissions()) {
            if (groupPermission.getPermission().equalsIgnoreCase(permission.getPermission())) {
              group.getPermissions().remove(groupPermission);
              Dao.forType(Group.class).update(group);
              GroupManagerPlugin.getInstance()
                  .getTextManager()
                  .sendMessage(sender, "gm.group.permdel.success", replacements);

              return;
            }
          }
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.permissionnotset", replacements);
        } else {
          replacements.put("%group%", groupName);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.group.help.permdel", null);
    }
  }

  private void executePrefix(CommandSender sender, String[] args) {
    if (args.length >= 2) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      try {
        String prefix = CommandUtil.combineStringsInArray(args, 2, args.length - 1) + " ";
        prefix = prefix.replace('&', '§');
        if (ChatColor.stripColor(prefix).equalsIgnoreCase(" ")) prefix = prefix.trim();
        if (prefix.length() > 16) {
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.prefixtoolong", null);
          return;
        }
        if (ChatColor.stripColor(prefix).equalsIgnoreCase(" ")) prefix = prefix.trim();
        Group group = GroupDao.getGroup(groupName);
        HashMap<String, String> replacements = new HashMap<>();
        if (group != null) {
          replacements.put("%group%", group.getName());
          if (args.length >= 3) {
            group.setPrefix(prefix);
            Dao.forType(Group.class).update(group);
            replacements.put("%prefix%", prefix);
            GroupManagerPlugin.getInstance()
                .getTextManager()
                .sendMessage(sender, "gm.group.prefix.edit", replacements);

          } else {
            replacements.put("%prefix%", group.getPrefix());
            GroupManagerPlugin.getInstance()
                .getTextManager()
                .sendMessage(sender, "gm.group.prefix.show", replacements);
          }
        } else {
          replacements.put("%group%", groupName);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.group.help.prefix", null);
    }
  }

  private void executePriority(CommandSender sender, String[] args) {
    if (args.length >= 2 && args.length < 4) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      try {
        Group group = GroupDao.getGroup(groupName);
        HashMap<String, String> replacements = new HashMap<>();
        if (group != null) {
          replacements.put("%group%", group.getName());
          if (args.length == 3) {
            int priority;
            try {
              priority = Integer.parseInt(args[2]);
              if (priority < 0 || priority > 99) throw new IllegalArgumentException();
            } catch (Exception e) {
              GroupManagerPlugin.getInstance()
                  .getTextManager()
                  .sendMessage(sender, "gm.group.error.priorityinvalid", null);
              return;
            }
            group.setPriority(priority);
            Dao.forType(Group.class).update(group);
            replacements.put("%priority%", "" + priority);
            GroupManagerPlugin.getInstance()
                .getTextManager()
                .sendMessage(sender, "gm.group.priority.edit", replacements);

          } else {
            replacements.put("%priority%", "" + group.getPriority());
            GroupManagerPlugin.getInstance()
                .getTextManager()
                .sendMessage(sender, "gm.group.priority.show", replacements);
          }
        } else {
          replacements.put("%group%", groupName);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.group.help.priority", null);
    }
  }

  private void executeDelete(CommandSender sender, String[] args) {
    if (args.length == 2) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      if (groupName.equals("default")) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.cannotmodifydefault", null);
        return;
      }
      try {
        HashMap<String, String> replacements = Maps.newHashMap();
        replacements.put("%group%", groupName);
        Group group = GroupDao.getGroup(groupName);
        if (group != null) {
          GroupDao.deleteGroup(group);
          replacements.put("%group%", group.getName());
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.delete.success", replacements);

        } else {
          replacements.put("%group%", groupName);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.group.help.delete", null);
    }
  }

  private void executeInfo(CommandSender sender, String[] args) {
    if (args.length == 2) {
      String groupName = args[1];
      if (groupName.length() > 14) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.group.error.nametoolong", null);
        return;
      }
      try {
        Group group = GroupDao.getGroup(groupName);
        HashMap<String, String> replacements = new HashMap<>();
        if (group != null) {
          replacements.put("%group%", group.getName());
          replacements.put("%prefix%", group.getPrefix());
          replacements.put("%priority%", group.getPriority() + "");
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.info.heading", replacements);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.info.prefix", replacements);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.info.priority", replacements);
        } else {
          replacements.put("%group%", groupName);
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .sendMessage(sender, "gm.group.error.groupdoesnotexist", replacements);
        }
      } catch (Exception e) {
        GroupManagerPlugin.getInstance()
            .getTextManager()
            .sendMessage(sender, "gm.error.internalerror", null);
        e.printStackTrace();
      }
    } else {
      GroupManagerPlugin.getInstance()
          .getTextManager()
          .sendMessage(sender, "gm.group.help.info", null);
    }
  }
}
