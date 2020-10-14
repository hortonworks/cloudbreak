package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum for Cloudera Manager shutdown readiness state.
 */
public enum ShutdownReadinessState {
  
  READY("READY"),
  
  NOT_READY("NOT_READY");

  private String value;

  ShutdownReadinessState(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ShutdownReadinessState fromValue(String text) {
    for (ShutdownReadinessState b : ShutdownReadinessState.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

