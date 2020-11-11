package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum for Cloudera Manager DB type. Note that DERBY and SQLITE3 are not supported DBs
 */
public enum ScmDbType {
  
  MYSQL("MYSQL"),
  
  POSTGRESQL("POSTGRESQL"),
  
  HSQL("HSQL"),
  
  ORACLE("ORACLE"),
  
  DERBY("DERBY"),
  
  SQLITE3("SQLITE3");

  private String value;

  ScmDbType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ScmDbType fromValue(String text) {
    for (ScmDbType b : ScmDbType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

