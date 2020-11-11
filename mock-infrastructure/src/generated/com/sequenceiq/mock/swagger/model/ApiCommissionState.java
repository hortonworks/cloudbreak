package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents the Commission state of an entity.
 */
public enum ApiCommissionState {
  
  COMMISSIONED("COMMISSIONED"),
  
  DECOMMISSIONING("DECOMMISSIONING"),
  
  DECOMMISSIONED("DECOMMISSIONED"),
  
  UNKNOWN("UNKNOWN"),
  
  OFFLINING("OFFLINING"),
  
  OFFLINED("OFFLINED");

  private String value;

  ApiCommissionState(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiCommissionState fromValue(String text) {
    for (ApiCommissionState b : ApiCommissionState.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

