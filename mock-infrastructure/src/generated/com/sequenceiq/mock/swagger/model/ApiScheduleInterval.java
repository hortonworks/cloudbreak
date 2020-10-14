package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents the unit for the repeat interval for schedules.
 */
public enum ApiScheduleInterval {
  
  MINUTE("MINUTE"),
  
  HOUR("HOUR"),
  
  DAY("DAY"),
  
  WEEK("WEEK"),
  
  MONTH("MONTH"),
  
  YEAR("YEAR");

  private String value;

  ApiScheduleInterval(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiScheduleInterval fromValue(String text) {
    for (ApiScheduleInterval b : ApiScheduleInterval.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

