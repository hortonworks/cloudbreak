package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum for CM peer types.
 */
public enum ApiCmPeerType {
  
  REPLICATION("REPLICATION"),
  
  STATUS_AGGREGATION("STATUS_AGGREGATION");

  private String value;

  ApiCmPeerType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiCmPeerType fromValue(String text) {
    for (ApiCmPeerType b : ApiCmPeerType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

