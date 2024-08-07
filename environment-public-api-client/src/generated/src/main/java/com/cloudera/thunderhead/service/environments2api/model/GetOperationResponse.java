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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Response object for tracking the latest (current/last) operation on the environment resource.
 */
@JsonPropertyOrder({
  GetOperationResponse.JSON_PROPERTY_OPERATION_ID,
  GetOperationResponse.JSON_PROPERTY_OPERATION_TYPE,
  GetOperationResponse.JSON_PROPERTY_OPERATION_STATUS,
  GetOperationResponse.JSON_PROPERTY_PROGRESS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class GetOperationResponse {
  public static final String JSON_PROPERTY_OPERATION_ID = "operationId";
  private String operationId;

  public static final String JSON_PROPERTY_OPERATION_TYPE = "operationType";
  private String operationType;

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

  public static final String JSON_PROPERTY_PROGRESS = "progress";
  private Integer progress;

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


  public GetOperationResponse operationType(String operationType) {
    this.operationType = operationType;
    return this;
  }

   /**
   * Type of the operation.
   * @return operationType
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OPERATION_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getOperationType() {
    return operationType;
  }


  @JsonProperty(JSON_PROPERTY_OPERATION_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOperationType(String operationType) {
    this.operationType = operationType;
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


  public GetOperationResponse progress(Integer progress) {
    this.progress = progress;
    return this;
  }

   /**
   * Progress percentage of the operation.
   * @return progress
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PROGRESS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getProgress() {
    return progress;
  }


  @JsonProperty(JSON_PROPERTY_PROGRESS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setProgress(Integer progress) {
    this.progress = progress;
  }


  /**
   * Return true if this GetOperationResponse object is equal to o.
   */
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
        Objects.equals(this.operationType, getOperationResponse.operationType) &&
        Objects.equals(this.operationStatus, getOperationResponse.operationStatus) &&
        Objects.equals(this.progress, getOperationResponse.progress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operationId, operationType, operationStatus, progress);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetOperationResponse {\n");
    sb.append("    operationId: ").append(toIndentedString(operationId)).append("\n");
    sb.append("    operationType: ").append(toIndentedString(operationType)).append("\n");
    sb.append("    operationStatus: ").append(toIndentedString(operationStatus)).append("\n");
    sb.append("    progress: ").append(toIndentedString(progress)).append("\n");
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

  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    // add `operationId` to the URL query string
    if (getOperationId() != null) {
      joiner.add(String.format("%soperationId%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getOperationId()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `operationType` to the URL query string
    if (getOperationType() != null) {
      joiner.add(String.format("%soperationType%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getOperationType()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `operationStatus` to the URL query string
    if (getOperationStatus() != null) {
      joiner.add(String.format("%soperationStatus%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getOperationStatus()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `progress` to the URL query string
    if (getProgress() != null) {
      joiner.add(String.format("%sprogress%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getProgress()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}

