package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum for external user mapping types
 */
public enum ApiExternalUserMappingType {
  
  LDAP("LDAP"),
  
  SAML_SCRIPT("SAML_SCRIPT"),
  
  SAML_ATTRIBUTE("SAML_ATTRIBUTE"),
  
  EXTERNAL_PROGRAM("EXTERNAL_PROGRAM");

  private String value;

  ApiExternalUserMappingType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ApiExternalUserMappingType fromValue(String text) {
    for (ApiExternalUserMappingType b : ApiExternalUserMappingType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

