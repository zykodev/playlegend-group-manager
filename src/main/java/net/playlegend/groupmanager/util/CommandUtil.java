package net.playlegend.groupmanager.util;

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
}
