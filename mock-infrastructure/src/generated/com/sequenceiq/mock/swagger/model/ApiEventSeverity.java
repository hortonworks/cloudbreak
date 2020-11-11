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
public enum ApiEventSeverity {
  
  UNKNOWN("UNKNOWN"),
  
  INFORMATIONAL("INFORMATIONAL"),
  
  IMPORTANT("IMPORTANT"),
  
  CRITICAL("CRITICAL");

  private String value;

  ApiEventSeverity(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiEventSeverity fromValue(String text) {
    for (ApiEventSeverity b : ApiEventSeverity.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

