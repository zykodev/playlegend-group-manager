package net.playlegend.groupmanager.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.playlegend.groupmanager.config.GroupManagerConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Properties;

public class FileUtil {

  /** Root directory used to store all configuration files in. */
  public static final File PLUGIN_ROOT_DIRECTORY = new File("plugins/GroupManager");

  /*
   * Check default file and folder structure.
   */
  static {
    try {
      if (!FileUtil.PLUGIN_ROOT_DIRECTORY.exists()) {
        if (!FileUtil.PLUGIN_ROOT_DIRECTORY.mkdir()) {
          throw new RuntimeException("Failed to create plugin root directory.");
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This method reads in a file and converts it into a Properties object.
   *
   * @param file the file to read in and convert
   * @return a properties object containing all the keys and values the file specified
   * @throws IOException if for example the file is inaccessible or contains invalid characters
   */
  public static Properties loadPropertiesFromFile(File file) throws IOException {
    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      Properties properties = new Properties();
      properties.load(fileInputStream);
      return properties;
    }
  }

  /**
   * Extracts the default resources to their corresponding places.
   *
   * @throws IOException if the resources could not be extracted
   */
  public static void extractDefaultResources() throws IOException {
    File hibernateConfig = new File(FileUtil.PLUGIN_ROOT_DIRECTORY, "hibernate.properties");
    if (!hibernateConfig.exists()) {
      Files.copy(
          Objects.requireNonNull(FileUtil.class.getResourceAsStream("assets/hibernate.properties")),
          hibernateConfig.toPath());
    }
    File deLocale = new File(FileUtil.PLUGIN_ROOT_DIRECTORY, "locales/de_de.json");
    if (!deLocale.getParentFile().exists()) deLocale.getParentFile().mkdir();
    if (!deLocale.exists()) {
      Files.copy(
          Objects.requireNonNull(FileUtil.class.getResourceAsStream("assets/locales/de_de.json")),
          deLocale.toPath());
    }
    File configFile = new File(FileUtil.PLUGIN_ROOT_DIRECTORY, "config.json");
    if (!configFile.exists()) {
      ObjectMapper objectMapper = new ObjectMapper();
      String fileContent = objectMapper.writeValueAsString(new GroupManagerConfig());
      Files.writeString(
          configFile.toPath(), fileContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
  }
}
