package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents of the high-level health status of a subject in the cluster.
 */
public enum ApiHealthSummary {
  
  DISABLED("DISABLED"),
  
  HISTORY_NOT_AVAILABLE("HISTORY_NOT_AVAILABLE"),
  
  NOT_AVAILABLE("NOT_AVAILABLE"),
  
  GOOD("GOOD"),
  
  CONCERNING("CONCERNING"),
  
  BAD("BAD");

  private String value;

  ApiHealthSummary(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiHealthSummary fromValue(String text) {
    for (ApiHealthSummary b : ApiHealthSummary.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

