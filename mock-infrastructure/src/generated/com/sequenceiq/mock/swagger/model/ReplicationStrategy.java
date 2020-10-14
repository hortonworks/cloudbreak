package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The strategy for distributing the file replication tasks among the mappers of the MR job associated with a replication.
 */
public enum ReplicationStrategy {
  
  STATIC("STATIC"),
  
  DYNAMIC("DYNAMIC");

  private String value;

  ReplicationStrategy(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ReplicationStrategy fromValue(String text) {
    for (ReplicationStrategy b : ReplicationStrategy.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

