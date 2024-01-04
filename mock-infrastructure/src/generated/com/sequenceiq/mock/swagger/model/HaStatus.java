package com.sequenceiq.mock.swagger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 */
public enum HaStatus {

  ACTIVE("ACTIVE"),

  STANDBY("STANDBY"),

  UNKNOWN("UNKNOWN");

  private String value;

  HaStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static HaStatus fromValue(String text) {
    for (HaStatus b : HaStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

