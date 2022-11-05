package net.playlegend.groupmanager.text;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import net.playlegend.groupmanager.GroupManager;
import net.playlegend.groupmanager.util.FileUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

public class TextManager {

  private final HashMap<String, LocaleConfiguration> localesMap = Maps.newHashMap();
  private LocaleConfiguration fallbackLocaleConfiguration;

  /**
   * Loads the locales file found in /plugins/GroupManager/locales.
   *
   * @param fallbackLocaleName the locale to use when a users' locale is not present
   */
  public void loadLocales(String fallbackLocaleName) {
    GroupManager.getInstance().log(Level.INFO, "Loading locales...");
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      for (File file :
          Objects.requireNonNull(new File(FileUtil.PLUGIN_ROOT_DIRECTORY, "locales").listFiles())) {
        try {
          LocaleConfiguration localeConfiguration =
              objectMapper.readValue(file, LocaleConfiguration.class);
          this.localesMap.put(localeConfiguration.getLocaleName(), localeConfiguration);
          GroupManager.getInstance()
              .log(Level.INFO, "Locale \"" + localeConfiguration.getLocaleName() + "\" loaded.");
          if (localeConfiguration.getLocaleName().equalsIgnoreCase(fallbackLocaleName)) {
            this.fallbackLocaleConfiguration = localeConfiguration;
          }
        } catch (Exception e) {
          GroupManager.getInstance()
              .log(
                  Level.WARNING,
                  "Failed to load locales data from file \""
                      + file.getAbsolutePath()
                      + "\"... is it possibly misplaced?",
                  e);
        }
      }
    } catch (NullPointerException e) {
      GroupManager.getInstance()
          .log(Level.WARNING, "Failed to load locales data. Falling back to placeholders.", e);
    }
    if (this.fallbackLocaleConfiguration == null) {
      GroupManager.getInstance()
          .log(
              Level.WARNING,
              "Default locales file \""
                  + fallbackLocaleName
                  + ".json\" not found in locales directory. Falling back to placeholders.");
    }
    GroupManager.getInstance().log(Level.INFO, "Finished loading locales.");
  }

  /**
   * Returns a message from the specified locale or the fallback locale if not present. Also inserts
   * values for placeholders.
   *
   * @param locale the locale to use
   * @param messageKey the message key under which the wanted message is stored
   * @param replacements a map of replacements consisting of tuples (placeholder, value)
   * @return the resulting reformatted message
   */
  public String getMessage(
      String locale, String messageKey, @Nullable HashMap<String, String> replacements) {
    LocaleConfiguration localeConfig =
        this.localesMap.getOrDefault(
            locale.toLowerCase(Locale.ROOT), this.fallbackLocaleConfiguration);
    return getTextEntryAndReplace(messageKey, replacements, localeConfig);
  }

  /**
   * Returns a message from the specified locale or the fallback locale if not present. Also inserts
   * values for placeholders.
   *
   * @param sender the sender to grab the locale from
   * @param messageKey the message key under which the wanted message is stored
   * @param replacements a map of replacements consisting of tuples (placeholder, value)
   * @return the resulting reformatted message
   * @see TextManager#getMessage(String, String, HashMap)
   */
  public String getMessage(
      CommandSender sender, String messageKey, @Nullable HashMap<String, String> replacements) {
    LocaleConfiguration localeConfig;
    if (sender instanceof Player) {
      localeConfig =
          this.localesMap.getOrDefault(
              ((Player) sender).getLocale().toLowerCase(Locale.ROOT),
              this.fallbackLocaleConfiguration);
    } else {
      localeConfig = this.fallbackLocaleConfiguration;
    }
    return getTextEntryAndReplace(messageKey, replacements, localeConfig);
  }

  /**
   * Returns a message from the specified locale configuration or messageKey if not found. Also
   * inserts values for placeholders.
   *
   * @param localeConfig the locale to use
   * @param messageKey the message key under which the wanted message is stored
   * @param replacements a map of replacements consisting of tuples (placeholder, value)
   * @return the resulting reformatted message
   * @see TextManager#getMessage(String, String, HashMap)
   */
  private String getTextEntryAndReplace(
      String messageKey,
      @Nullable HashMap<String, String> replacements,
      LocaleConfiguration localeConfig) {
    if (localeConfig != null) {
      String text = localeConfig.getMessageMap().getOrDefault(messageKey, messageKey);
      if (replacements != null) {
        for (String s : replacements.keySet()) {
          text = text.replace(s, replacements.get(s));
        }
      }
      return text;
    } else {
      return messageKey;
    }
  }

  /**
   * Returns the locale of a player or 'fallback' if console.
   *
   * @param commandSender the command sender (either a player or console)
   * @return the locale string or 'fallback' if console.
   */
  public String getLocale(CommandSender commandSender) {
    String playerLocale;
    if (commandSender instanceof Player) {
      playerLocale = ((Player) commandSender).getLocale();
    } else {
      playerLocale = "fallback";
    }
    return playerLocale;
  }

  /**
   * Sends a message to a command sender after formatting it.
   *
   * @param sender the sender to grab the locale from
   * @param messageKey the message key under which the wanted message is stored
   * @param replacements a map of replacements consisting of tuples (placeholder, value)
   */
  public void sendMessage(
      CommandSender sender, String messageKey, @Nullable HashMap<String, String> replacements) {
    String locale = this.getLocale(sender);
    String message = this.getMessage(locale, messageKey, replacements);
    if (sender instanceof Player) {
      sender.sendMessage(GroupManager.getInstance().getPrefix() + message);
    } else {
      GroupManager.getInstance().log(Level.INFO, ChatColor.stripColor(message));
    }
  }
}
