package com.sequenceiq.mock.swagger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the configuration staleness status of an entity.
 */
public enum ApiConfigStalenessStatus {

  FRESH("FRESH"),

  STALE_REFRESHABLE("STALE_REFRESHABLE"),

  STALE("STALE");

  private String value;

  ApiConfigStalenessStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiConfigStalenessStatus fromValue(String text) {
    for (ApiConfigStalenessStatus b : ApiConfigStalenessStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

