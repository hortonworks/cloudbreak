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
import com.cloudera.thunderhead.service.environments2api.model.OperationType;
import com.cloudera.thunderhead.service.environments2api.model.SyncOperationDetails;
import com.cloudera.thunderhead.service.environments2api.model.SyncStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Response object for Sync Operation.
 */
@JsonPropertyOrder({
  LastSyncStatusResponse.JSON_PROPERTY_OPERATION_ID,
  LastSyncStatusResponse.JSON_PROPERTY_OPERATION_TYPE,
  LastSyncStatusResponse.JSON_PROPERTY_STATUS,
  LastSyncStatusResponse.JSON_PROPERTY_SUCCESS,
  LastSyncStatusResponse.JSON_PROPERTY_FAILURE,
  LastSyncStatusResponse.JSON_PROPERTY_ERROR,
  LastSyncStatusResponse.JSON_PROPERTY_START_DATE,
  LastSyncStatusResponse.JSON_PROPERTY_END_DATE
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class LastSyncStatusResponse {
  public static final String JSON_PROPERTY_OPERATION_ID = "operationId";
  private String operationId;

  public static final String JSON_PROPERTY_OPERATION_TYPE = "operationType";
  private OperationType operationType;

  public static final String JSON_PROPERTY_STATUS = "status";
  private SyncStatus status;

  public static final String JSON_PROPERTY_SUCCESS = "success";
  private List<SyncOperationDetails> success = new ArrayList<>();

  public static final String JSON_PROPERTY_FAILURE = "failure";
  private List<SyncOperationDetails> failure = new ArrayList<>();

  public static final String JSON_PROPERTY_ERROR = "error";
  private String error;

  public static final String JSON_PROPERTY_START_DATE = "startDate";
  private OffsetDateTime startDate;

  public static final String JSON_PROPERTY_END_DATE = "endDate";
  private OffsetDateTime endDate;

  public LastSyncStatusResponse() {
  }

  public LastSyncStatusResponse operationId(String operationId) {
    
    this.operationId = operationId;
    return this;
  }

   /**
   * Unique operation ID assigned to this command execution. Use this identifier with &#39;get-operation&#39; to track status and retrieve detailed results.
   * @return operationId
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_OPERATION_ID)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getOperationId() {
    return operationId;
  }


  @JsonProperty(JSON_PROPERTY_OPERATION_ID)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setOperationId(String operationId) {
    this.operationId = operationId;
  }


  public LastSyncStatusResponse operationType(OperationType operationType) {
    
    this.operationType = operationType;
    return this;
  }

   /**
   * Get operationType
   * @return operationType
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OPERATION_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public OperationType getOperationType() {
    return operationType;
  }


  @JsonProperty(JSON_PROPERTY_OPERATION_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }


  public LastSyncStatusResponse status(SyncStatus status) {
    
    this.status = status;
    return this;
  }

   /**
   * Get status
   * @return status
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public SyncStatus getStatus() {
    return status;
  }


  @JsonProperty(JSON_PROPERTY_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStatus(SyncStatus status) {
    this.status = status;
  }


  public LastSyncStatusResponse success(List<SyncOperationDetails> success) {
    
    this.success = success;
    return this;
  }

  public LastSyncStatusResponse addSuccessItem(SyncOperationDetails successItem) {
    if (this.success == null) {
      this.success = new ArrayList<>();
    }
    this.success.add(successItem);
    return this;
  }

   /**
   * List of sync operation details for all succeeded environments.
   * @return success
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SUCCESS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<SyncOperationDetails> getSuccess() {
    return success;
  }


  @JsonProperty(JSON_PROPERTY_SUCCESS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSuccess(List<SyncOperationDetails> success) {
    this.success = success;
  }


  public LastSyncStatusResponse failure(List<SyncOperationDetails> failure) {
    
    this.failure = failure;
    return this;
  }

  public LastSyncStatusResponse addFailureItem(SyncOperationDetails failureItem) {
    if (this.failure == null) {
      this.failure = new ArrayList<>();
    }
    this.failure.add(failureItem);
    return this;
  }

   /**
   * List of sync operation details for all failed environments.
   * @return failure
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FAILURE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<SyncOperationDetails> getFailure() {
    return failure;
  }


  @JsonProperty(JSON_PROPERTY_FAILURE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFailure(List<SyncOperationDetails> failure) {
    this.failure = failure;
  }


  public LastSyncStatusResponse error(String error) {
    
    this.error = error;
    return this;
  }

   /**
   * If there is any error associated. The error will be populated on any error and it may be populated when the operation failure details are empty.
   * @return error
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ERROR)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getError() {
    return error;
  }


  @JsonProperty(JSON_PROPERTY_ERROR)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setError(String error) {
    this.error = error;
  }


  public LastSyncStatusResponse startDate(OffsetDateTime startDate) {
    
    this.startDate = startDate;
    return this;
  }

   /**
   * Date when the sync operation started.
   * @return startDate
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_START_DATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public OffsetDateTime getStartDate() {
    return startDate;
  }


  @JsonProperty(JSON_PROPERTY_START_DATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStartDate(OffsetDateTime startDate) {
    this.startDate = startDate;
  }


  public LastSyncStatusResponse endDate(OffsetDateTime endDate) {
    
    this.endDate = endDate;
    return this;
  }

   /**
   * Date when the sync operation ended. Omitted if operation has not ended.
   * @return endDate
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_END_DATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public OffsetDateTime getEndDate() {
    return endDate;
  }


  @JsonProperty(JSON_PROPERTY_END_DATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEndDate(OffsetDateTime endDate) {
    this.endDate = endDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LastSyncStatusResponse lastSyncStatusResponse = (LastSyncStatusResponse) o;
    return Objects.equals(this.operationId, lastSyncStatusResponse.operationId) &&
        Objects.equals(this.operationType, lastSyncStatusResponse.operationType) &&
        Objects.equals(this.status, lastSyncStatusResponse.status) &&
        Objects.equals(this.success, lastSyncStatusResponse.success) &&
        Objects.equals(this.failure, lastSyncStatusResponse.failure) &&
        Objects.equals(this.error, lastSyncStatusResponse.error) &&
        Objects.equals(this.startDate, lastSyncStatusResponse.startDate) &&
        Objects.equals(this.endDate, lastSyncStatusResponse.endDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operationId, operationType, status, success, failure, error, startDate, endDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LastSyncStatusResponse {\n");
    sb.append("    operationId: ").append(toIndentedString(operationId)).append("\n");
    sb.append("    operationType: ").append(toIndentedString(operationType)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    success: ").append(toIndentedString(success)).append("\n");
    sb.append("    failure: ").append(toIndentedString(failure)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
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

