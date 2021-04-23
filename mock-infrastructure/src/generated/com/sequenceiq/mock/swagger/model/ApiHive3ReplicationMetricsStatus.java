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
public enum ApiHive3ReplicationMetricsStatus {
  
  SUCCESS("SUCCESS"),
  
  FAILED("FAILED"),
  
  IN_PROGRESS("IN_PROGRESS"),
  
  FAILED_ADMIN("FAILED_ADMIN");

  private String value;

  ApiHive3ReplicationMetricsStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiHive3ReplicationMetricsStatus fromValue(String text) {
    for (ApiHive3ReplicationMetricsStatus b : ApiHive3ReplicationMetricsStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

