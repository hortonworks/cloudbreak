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
public enum ApiEventCategory {
  
  UNKNOWN("UNKNOWN"),
  
  HEALTH_EVENT("HEALTH_EVENT"),
  
  LOG_EVENT("LOG_EVENT"),
  
  AUDIT_EVENT("AUDIT_EVENT"),
  
  ACTIVITY_EVENT("ACTIVITY_EVENT"),
  
  HBASE("HBASE"),
  
  SYSTEM("SYSTEM");

  private String value;

  ApiEventCategory(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiEventCategory fromValue(String text) {
    for (ApiEventCategory b : ApiEventCategory.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

