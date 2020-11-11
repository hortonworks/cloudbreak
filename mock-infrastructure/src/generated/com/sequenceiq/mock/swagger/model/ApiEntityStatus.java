package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The single value used by the Cloudera Manager UI to represent the status of the entity. It is computed from a variety of other entity-specific states, not all values apply to all entities. For example, STARTING/STOPPING do not apply to a host.
 */
public enum ApiEntityStatus {
  
  UNKNOWN("UNKNOWN"),
  
  NONE("NONE"),
  
  STOPPED("STOPPED"),
  
  DOWN("DOWN"),
  
  UNKNOWN_HEALTH("UNKNOWN_HEALTH"),
  
  DISABLED_HEALTH("DISABLED_HEALTH"),
  
  CONCERNING_HEALTH("CONCERNING_HEALTH"),
  
  BAD_HEALTH("BAD_HEALTH"),
  
  GOOD_HEALTH("GOOD_HEALTH"),
  
  STARTING("STARTING"),
  
  STOPPING("STOPPING"),
  
  HISTORY_NOT_AVAILABLE("HISTORY_NOT_AVAILABLE");

  private String value;

  ApiEntityStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiEntityStatus fromValue(String text) {
    for (ApiEntityStatus b : ApiEntityStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

