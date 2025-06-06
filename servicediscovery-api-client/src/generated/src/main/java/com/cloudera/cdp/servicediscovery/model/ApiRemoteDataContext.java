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
import com.cloudera.cdp.servicediscovery.model.ApiEndPoint;
import com.cloudera.cdp.servicediscovery.model.ApiMapEntry;
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
 * This object contains enough information for a Workload cluster to connect to an SDX cluster.
 */
@JsonPropertyOrder({
  ApiRemoteDataContext.JSON_PROPERTY_END_POINT_ID,
  ApiRemoteDataContext.JSON_PROPERTY_END_POINTS,
  ApiRemoteDataContext.JSON_PROPERTY_CONFIGS,
  ApiRemoteDataContext.JSON_PROPERTY_CLUSTER_VERSION
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class ApiRemoteDataContext {
  public static final String JSON_PROPERTY_END_POINT_ID = "endPointId";
  private String endPointId;

  public static final String JSON_PROPERTY_END_POINTS = "endPoints";
  private List<ApiEndPoint> endPoints = new ArrayList<>();

  public static final String JSON_PROPERTY_CONFIGS = "configs";
  private List<ApiMapEntry> configs = new ArrayList<>();

  public static final String JSON_PROPERTY_CLUSTER_VERSION = "clusterVersion";
  private String clusterVersion;

  public ApiRemoteDataContext() {
  }

  public ApiRemoteDataContext endPointId(String endPointId) {
    
    this.endPointId = endPointId;
    return this;
  }

   /**
   * A string to uniquely identify the SDX cluster.
   * @return endPointId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_END_POINT_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getEndPointId() {
    return endPointId;
  }


  @JsonProperty(JSON_PROPERTY_END_POINT_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEndPointId(String endPointId) {
    this.endPointId = endPointId;
  }


  public ApiRemoteDataContext endPoints(List<ApiEndPoint> endPoints) {
    
    this.endPoints = endPoints;
    return this;
  }

  public ApiRemoteDataContext addEndPointsItem(ApiEndPoint endPointsItem) {
    if (this.endPoints == null) {
      this.endPoints = new ArrayList<>();
    }
    this.endPoints.add(endPointsItem);
    return this;
  }

   /**
   * List of endPoints exported by this SDX cluster.
   * @return endPoints
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_END_POINTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<ApiEndPoint> getEndPoints() {
    return endPoints;
  }


  @JsonProperty(JSON_PROPERTY_END_POINTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEndPoints(List<ApiEndPoint> endPoints) {
    this.endPoints = endPoints;
  }


  public ApiRemoteDataContext configs(List<ApiMapEntry> configs) {
    
    this.configs = configs;
    return this;
  }

  public ApiRemoteDataContext addConfigsItem(ApiMapEntry configsItem) {
    if (this.configs == null) {
      this.configs = new ArrayList<>();
    }
    this.configs.add(configsItem);
    return this;
  }

   /**
   * SDX cluster specifc options.
   * @return configs
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CONFIGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<ApiMapEntry> getConfigs() {
    return configs;
  }


  @JsonProperty(JSON_PROPERTY_CONFIGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setConfigs(List<ApiMapEntry> configs) {
    this.configs = configs;
  }


  public ApiRemoteDataContext clusterVersion(String clusterVersion) {
    
    this.clusterVersion = clusterVersion;
    return this;
  }

   /**
   * Cluster version
   * @return clusterVersion
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CLUSTER_VERSION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getClusterVersion() {
    return clusterVersion;
  }


  @JsonProperty(JSON_PROPERTY_CLUSTER_VERSION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setClusterVersion(String clusterVersion) {
    this.clusterVersion = clusterVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiRemoteDataContext apiRemoteDataContext = (ApiRemoteDataContext) o;
    return Objects.equals(this.endPointId, apiRemoteDataContext.endPointId) &&
        Objects.equals(this.endPoints, apiRemoteDataContext.endPoints) &&
        Objects.equals(this.configs, apiRemoteDataContext.configs) &&
        Objects.equals(this.clusterVersion, apiRemoteDataContext.clusterVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endPointId, endPoints, configs, clusterVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRemoteDataContext {\n");
    sb.append("    endPointId: ").append(toIndentedString(endPointId)).append("\n");
    sb.append("    endPoints: ").append(toIndentedString(endPoints)).append("\n");
    sb.append("    configs: ").append(toIndentedString(configs)).append("\n");
    sb.append("    clusterVersion: ").append(toIndentedString(clusterVersion)).append("\n");
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

