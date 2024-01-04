package com.sequenceiq.mock.swagger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 */
public enum PolicyStatus {

  ENABLED("ENABLED"),

  DISABLED("DISABLED"),

  FAILED_ADMIN("FAILED_ADMIN");

  private String value;

  PolicyStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static PolicyStatus fromValue(String text) {
    for (PolicyStatus b : PolicyStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

