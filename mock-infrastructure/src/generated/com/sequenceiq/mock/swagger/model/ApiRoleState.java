package com.sequenceiq.mock.swagger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the configured run state of a role.
 */
public enum ApiRoleState {

  HISTORY_NOT_AVAILABLE("HISTORY_NOT_AVAILABLE"),

  UNKNOWN("UNKNOWN"),

  STARTING("STARTING"),

  STARTED("STARTED"),

  BUSY("BUSY"),

  STOPPING("STOPPING"),

  STOPPED("STOPPED"),

  NA("NA");

  private String value;

  ApiRoleState(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiRoleState fromValue(String text) {
    for (ApiRoleState b : ApiRoleState.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

