package com.sequenceiq.mock.swagger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 */
public enum Status {

  NOT_READY("NOT_READY"),

  READY("READY"),

  RUNNING("RUNNING"),

  WAITING_FOR_SOURCE_RESTART("WAITING_FOR_SOURCE_RESTART"),

  SETUP_INITIATED_ON_REMOTE("SETUP_INITIATED_ON_REMOTE"),

  ERROR("ERROR");

  private String value;

  Status(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static Status fromValue(String text) {
    for (Status b : Status.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

