package com.sequenceiq.mock.swagger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * This will decide how cloud replication will take place
 */
public enum ReplicationOption {

  METADATA_ONLY("METADATA_ONLY"),

  METADATA_AND_DATA("METADATA_AND_DATA"),

  KEEP_DATA_IN_CLOUD("KEEP_DATA_IN_CLOUD");

  private String value;

  ReplicationOption(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ReplicationOption fromValue(String text) {
    for (ReplicationOption b : ReplicationOption.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

