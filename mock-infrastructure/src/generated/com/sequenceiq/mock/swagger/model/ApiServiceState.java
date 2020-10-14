package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents the configured run state of a service.
 */
public enum ApiServiceState {
  
  HISTORY_NOT_AVAILABLE("HISTORY_NOT_AVAILABLE"),
  
  UNKNOWN("UNKNOWN"),
  
  STARTING("STARTING"),
  
  STARTED("STARTED"),
  
  STOPPING("STOPPING"),
  
  STOPPED("STOPPED"),
  
  NA("NA");

  private String value;

  ApiServiceState(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiServiceState fromValue(String text) {
    for (ApiServiceState b : ApiServiceState.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

