package net.playlegend.groupmanager.util;

import net.playlegend.groupmanager.GroupManagerPlugin;
import net.playlegend.groupmanager.model.User;
import org.bukkit.command.CommandSender;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;

/** Provides commonly used functions so that they can be shared across multiple commands. */
public class CommandUtil {

  /**
   * From an array of string, combines specified strings into one.
   *
   * @param strings the array of strings
   * @param startIndex the index where to begin combining
   * @param endIndex the index where to stop combining
   * @return the string consisting of the string from the array, seperated by spaces
   */
  public static String combineStringsInArray(String[] strings, int startIndex, int endIndex) {
    StringBuilder s = new StringBuilder();
    for (int i = startIndex; i <= endIndex; i++) {
      s.append(strings[i]).append(" ");
    }
    return s.toString().trim();
  }

  /**
   * Inserts the formatted remaining rank duration into a replacement map.
   *
   * @param user the user whos remaining rank duration to insert
   * @param sender the sender who will eventually see this message
   * @param replacements the replacements map to insert into
   */
  public static void insertDurationReplacement(
      User user, CommandSender sender, HashMap<String, String> replacements) {
    if (user.getGroupValidUntil() == -1) {
      replacements.put(
          "%duration%",
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .getMessage(sender, "gm.user.group.duration.infinite", null));
    } else {
      String format =
          GroupManagerPlugin.getInstance()
              .getTextManager()
              .getMessage(sender, "gm.user.group.duration.format", null);
      DateTimeFormatter formatter = DateTimeFormat.forPattern(format);
      String formattedTime = formatter.print(user.getGroupValidUntil());
      replacements.put("%duration%", formattedTime);
    }
  }
}
