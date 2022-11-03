package net.playlegend.groupmanager.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class FileUtil {

    /**
     * This method reads in a file and converts it into a Properties object.
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

}
