package net.playlegend.groupmanager.text;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.HashMap;

public class LocaleConfiguration {

  @JsonProperty("locale.name")
  @Getter
  private String localeName;

  @JsonProperty("locale.texts")
  @Getter
  private HashMap<String, String> messageMap;
}
