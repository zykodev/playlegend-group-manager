package net.playlegend.groupmanager.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public class GroupManagerConfig {

  @JsonProperty("fallback_locale")
  @Getter
  private String fallbackLocale = "de-DE";

  @JsonProperty("cache_rebuild_interval_ticks")
  @Getter
  private int cacheRebuildInterval = 6000;

  @JsonProperty("sign_update_interval_ticks")
  @Getter
  private int signUpdateInterval = 100;

  @JsonProperty("group_validity_check_interval_ticks")
  @Getter
  private int groupValidityCheckInterval = 100;
}
