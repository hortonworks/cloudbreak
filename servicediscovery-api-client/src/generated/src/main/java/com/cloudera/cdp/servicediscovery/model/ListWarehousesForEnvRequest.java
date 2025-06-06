/*
 * CDP Service Discovery
 * CDP Service Discovery is a web service that provides information to a workload service
 *
 * The version of the OpenAPI document: __API_VERSION__
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.cloudera.cdp.servicediscovery.model;

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
 * Request for the warehouses for a specific envrionment, deprecated.
 */
@JsonPropertyOrder({
  ListWarehousesForEnvRequest.JSON_PROPERTY_ENVIRONMENT
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class ListWarehousesForEnvRequest {
  public static final String JSON_PROPERTY_ENVIRONMENT = "environment";
  private String environment;

  public ListWarehousesForEnvRequest() {
  }

  public ListWarehousesForEnvRequest environment(String environment) {
    
    this.environment = environment;
    return this;
  }

   /**
   * The environment Id to get the warehouses for.
   * @return environment
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENVIRONMENT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getEnvironment() {
    return environment;
  }


  @JsonProperty(JSON_PROPERTY_ENVIRONMENT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListWarehousesForEnvRequest listWarehousesForEnvRequest = (ListWarehousesForEnvRequest) o;
    return Objects.equals(this.environment, listWarehousesForEnvRequest.environment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environment);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListWarehousesForEnvRequest {\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
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

