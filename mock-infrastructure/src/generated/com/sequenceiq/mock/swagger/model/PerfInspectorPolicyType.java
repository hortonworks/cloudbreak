package com.sequenceiq.mock.swagger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum to pick the type of Performance Inspector Diagnostics to run
 */
public enum PerfInspectorPolicyType {

  FULL("FULL"),

  QUICK("QUICK");

  private String value;

  PerfInspectorPolicyType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static PerfInspectorPolicyType fromValue(String text) {
    for (PerfInspectorPolicyType b : PerfInspectorPolicyType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

