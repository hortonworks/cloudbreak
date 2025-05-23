/*
 * Cloudera Environments Service
 * Cloudera Environments Service is a web service that manages cloud provider access.
 *
 * The version of the OpenAPI document: __API_VERSION__
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.cloudera.thunderhead.service.environments2api.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Response object for tracking the latest (current/last) operation on the environment resource.
 */
@JsonPropertyOrder({
  GetOperationResponse.JSON_PROPERTY_OPERATION_ID,
  GetOperationResponse.JSON_PROPERTY_OPERATION_NAME,
  GetOperationResponse.JSON_PROPERTY_OPERATION_STATUS,
  GetOperationResponse.JSON_PROPERTY_STARTED,
  GetOperationResponse.JSON_PROPERTY_ENDED
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class GetOperationResponse {
  public static final String JSON_PROPERTY_OPERATION_ID = "operationId";
  private String operationId;

  public static final String JSON_PROPERTY_OPERATION_NAME = "operationName";
  private String operationName;

  /**
   * Status of the operation.
   */
  public enum OperationStatusEnum {
    UNKNOWN("UNKNOWN"),
    
    RUNNING("RUNNING"),
    
    FAILED("FAILED"),
    
    FINISHED("FINISHED"),
    
    CANCELLED("CANCELLED");

    private String value;

    OperationStatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static OperationStatusEnum fromValue(String value) {
      for (OperationStatusEnum b : OperationStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_OPERATION_STATUS = "operationStatus";
  private OperationStatusEnum operationStatus;

  public static final String JSON_PROPERTY_STARTED = "started";
  private OffsetDateTime started;

  public static final String JSON_PROPERTY_ENDED = "ended";
  private OffsetDateTime ended;

  public GetOperationResponse() {
  }

  public GetOperationResponse operationId(String operationId) {
    
    this.operationId = operationId;
    return this;
  }

   /**
   * Identifier of the operation.
   * @return operationId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OPERATION_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getOperationId() {
    return operationId;
  }


  @JsonProperty(JSON_PROPERTY_OPERATION_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOperationId(String operationId) {
    this.operationId = operationId;
  }


  public GetOperationResponse operationName(String operationName) {
    
    this.operationName = operationName;
    return this;
  }

   /**
   * Name of the operation.
   * @return operationName
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OPERATION_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getOperationName() {
    return operationName;
  }


  @JsonProperty(JSON_PROPERTY_OPERATION_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }


  public GetOperationResponse operationStatus(OperationStatusEnum operationStatus) {
    
    this.operationStatus = operationStatus;
    return this;
  }

   /**
   * Status of the operation.
   * @return operationStatus
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OPERATION_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public OperationStatusEnum getOperationStatus() {
    return operationStatus;
  }


  @JsonProperty(JSON_PROPERTY_OPERATION_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOperationStatus(OperationStatusEnum operationStatus) {
    this.operationStatus = operationStatus;
  }


  public GetOperationResponse started(OffsetDateTime started) {
    
    this.started = started;
    return this;
  }

   /**
   * Start time of the operation.
   * @return started
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_STARTED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public OffsetDateTime getStarted() {
    return started;
  }


  @JsonProperty(JSON_PROPERTY_STARTED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStarted(OffsetDateTime started) {
    this.started = started;
  }


  public GetOperationResponse ended(OffsetDateTime ended) {
    
    this.ended = ended;
    return this;
  }

   /**
   * End time of the operation.
   * @return ended
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENDED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public OffsetDateTime getEnded() {
    return ended;
  }


  @JsonProperty(JSON_PROPERTY_ENDED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnded(OffsetDateTime ended) {
    this.ended = ended;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetOperationResponse getOperationResponse = (GetOperationResponse) o;
    return Objects.equals(this.operationId, getOperationResponse.operationId) &&
        Objects.equals(this.operationName, getOperationResponse.operationName) &&
        Objects.equals(this.operationStatus, getOperationResponse.operationStatus) &&
        Objects.equals(this.started, getOperationResponse.started) &&
        Objects.equals(this.ended, getOperationResponse.ended);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operationId, operationName, operationStatus, started, ended);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetOperationResponse {\n");
    sb.append("    operationId: ").append(toIndentedString(operationId)).append("\n");
    sb.append("    operationName: ").append(toIndentedString(operationName)).append("\n");
    sb.append("    operationStatus: ").append(toIndentedString(operationStatus)).append("\n");
    sb.append("    started: ").append(toIndentedString(started)).append("\n");
    sb.append("    ended: ").append(toIndentedString(ended)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

