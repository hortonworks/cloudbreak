package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 
 */
public enum ApiActivityType {
  
  UNKNOWN("UNKNOWN"),
  
  OOZIE("OOZIE"),
  
  PIG("PIG"),
  
  HIVE("HIVE"),
  
  MR("MR"),
  
  STREAMING("STREAMING");

  private String value;

  ApiActivityType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiActivityType fromValue(String text) {
    for (ApiActivityType b : ApiActivityType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

