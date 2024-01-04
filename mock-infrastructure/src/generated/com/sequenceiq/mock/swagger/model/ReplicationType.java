package com.sequenceiq.mock.swagger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 */
public enum ReplicationType {

  BOOTSTRAP("BOOTSTRAP"),

  INCREMENTAL("INCREMENTAL");

  private String value;

  ReplicationType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ReplicationType fromValue(String text) {
    for (ReplicationType b : ReplicationType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

