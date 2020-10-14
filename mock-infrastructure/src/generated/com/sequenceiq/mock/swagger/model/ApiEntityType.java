package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents the types of entities.
 */
public enum ApiEntityType {
  
  CLUSTER("CLUSTER"),
  
  SERVICE("SERVICE"),
  
  ROLE("ROLE"),
  
  HOST("HOST");

  private String value;

  ApiEntityType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiEntityType fromValue(String text) {
    for (ApiEntityType b : ApiEntityType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

