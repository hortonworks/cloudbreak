package com.sequenceiq.mock.swagger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 */
public enum HTTPMethod {

  GET("GET"),

  POST("POST"),

  PUT("PUT"),

  DELETE("DELETE");

  private String value;

  HTTPMethod(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static HTTPMethod fromValue(String text) {
    for (HTTPMethod b : HTTPMethod.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

