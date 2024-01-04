package com.sequenceiq.mock.swagger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 */
public enum ApiActivityStatus {

  UNKNOWN("UNKNOWN"),

  SUBMITTED("SUBMITTED"),

  STARTED("STARTED"),

  SUSPENDED("SUSPENDED"),

  FAILED("FAILED"),

  KILLED("KILLED"),

  SUCCEEDED("SUCCEEDED"),

  ASSUMED_SUCCEEDED("ASSUMED_SUCCEEDED");

  private String value;

  ApiActivityStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiActivityStatus fromValue(String text) {
    for (ApiActivityStatus b : ApiActivityStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

