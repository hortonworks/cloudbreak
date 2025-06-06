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
import com.cloudera.cdp.servicediscovery.model.OpDB;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Response for Operational Databaes (OpDBs) in a specific environment.
 */
@JsonPropertyOrder({
  ListOpdbsForEnvResponse.JSON_PROPERTY_ENVIRONMENT,
  ListOpdbsForEnvResponse.JSON_PROPERTY_LAST_UPDATED,
  ListOpdbsForEnvResponse.JSON_PROPERTY_OP_D_BS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class ListOpdbsForEnvResponse {
  public static final String JSON_PROPERTY_ENVIRONMENT = "environment";
  private String environment;

  public static final String JSON_PROPERTY_LAST_UPDATED = "lastUpdated";
  private Long lastUpdated;

  public static final String JSON_PROPERTY_OP_D_BS = "opDBs";
  private List<OpDB> opDBs = new ArrayList<>();

  public ListOpdbsForEnvResponse() {
  }

  public ListOpdbsForEnvResponse environment(String environment) {
    
    this.environment = environment;
    return this;
  }

   /**
   * The CRN of the environment.
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


  public ListOpdbsForEnvResponse lastUpdated(Long lastUpdated) {
    
    this.lastUpdated = lastUpdated;
    return this;
  }

   /**
   * The RFC3339 timestamp of the last change to the OpDB config.
   * @return lastUpdated
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_LAST_UPDATED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getLastUpdated() {
    return lastUpdated;
  }


  @JsonProperty(JSON_PROPERTY_LAST_UPDATED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setLastUpdated(Long lastUpdated) {
    this.lastUpdated = lastUpdated;
  }


  public ListOpdbsForEnvResponse opDBs(List<OpDB> opDBs) {
    
    this.opDBs = opDBs;
    return this;
  }

  public ListOpdbsForEnvResponse addOpDBsItem(OpDB opDBsItem) {
    if (this.opDBs == null) {
      this.opDBs = new ArrayList<>();
    }
    this.opDBs.add(opDBsItem);
    return this;
  }

   /**
   * The list of OpDBs in the environment.
   * @return opDBs
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OP_D_BS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<OpDB> getOpDBs() {
    return opDBs;
  }


  @JsonProperty(JSON_PROPERTY_OP_D_BS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOpDBs(List<OpDB> opDBs) {
    this.opDBs = opDBs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListOpdbsForEnvResponse listOpdbsForEnvResponse = (ListOpdbsForEnvResponse) o;
    return Objects.equals(this.environment, listOpdbsForEnvResponse.environment) &&
        Objects.equals(this.lastUpdated, listOpdbsForEnvResponse.lastUpdated) &&
        Objects.equals(this.opDBs, listOpdbsForEnvResponse.opDBs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environment, lastUpdated, opDBs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListOpdbsForEnvResponse {\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    lastUpdated: ").append(toIndentedString(lastUpdated)).append("\n");
    sb.append("    opDBs: ").append(toIndentedString(opDBs)).append("\n");
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

