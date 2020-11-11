package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Roles to include during a cluster rolling restart.
 */
public enum ApiRolesToInclude {
  
  SLAVES_ONLY("SLAVES_ONLY"),
  
  NON_SLAVES_ONLY("NON_SLAVES_ONLY"),
  
  ALL_ROLES("ALL_ROLES");

  private String value;

  ApiRolesToInclude(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiRolesToInclude fromValue(String text) {
    for (ApiRolesToInclude b : ApiRolesToInclude.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

