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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * The response object for FreeIPA vertical scaling.
 */
@JsonPropertyOrder({
  StartFreeIpaVerticalScalingResponse.JSON_PROPERTY_RESULT,
  StartFreeIpaVerticalScalingResponse.JSON_PROPERTY_OPERATION_ID
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class StartFreeIpaVerticalScalingResponse {
  public static final String JSON_PROPERTY_RESULT = "result";
  private String result;

  public static final String JSON_PROPERTY_OPERATION_ID = "operationId";
  private String operationId;

  public StartFreeIpaVerticalScalingResponse() {
  }

  public StartFreeIpaVerticalScalingResponse result(String result) {
    
    this.result = result;
    return this;
  }

   /**
   * The result of the operation.
   * @return result
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_RESULT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getResult() {
    return result;
  }


  @JsonProperty(JSON_PROPERTY_RESULT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setResult(String result) {
    this.result = result;
  }


  public StartFreeIpaVerticalScalingResponse operationId(String operationId) {
    
    this.operationId = operationId;
    return this;
  }

   /**
   * Unique operation ID assigned to this command execution. Use this identifier with &#39;get-operation&#39; to track status and retrieve detailed results.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StartFreeIpaVerticalScalingResponse startFreeIpaVerticalScalingResponse = (StartFreeIpaVerticalScalingResponse) o;
    return Objects.equals(this.result, startFreeIpaVerticalScalingResponse.result) &&
        Objects.equals(this.operationId, startFreeIpaVerticalScalingResponse.operationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(result, operationId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StartFreeIpaVerticalScalingResponse {\n");
    sb.append("    result: ").append(toIndentedString(result)).append("\n");
    sb.append("    operationId: ").append(toIndentedString(operationId)).append("\n");
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

