package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiEndPoint;
import com.sequenceiq.mock.swagger.model.ApiMapEntry;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This object contains enough information for a Workload cluster to connect to an SDX cluster. This object contains no secrets.
 */
@ApiModel(description = "This object contains enough information for a Workload cluster to connect to an SDX cluster. This object contains no secrets.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRemoteDataContext   {
  @JsonProperty("endPointId")
  private String endPointId = null;

  @JsonProperty("endPoints")
  @Valid
  private List<ApiEndPoint> endPoints = null;

  @JsonProperty("configs")
  @Valid
  private List<ApiMapEntry> configs = null;

  @JsonProperty("clusterVersion")
  private String clusterVersion = null;

  public ApiRemoteDataContext endPointId(String endPointId) {
    this.endPointId = endPointId;
    return this;
  }

  /**
   * A string to uniquely identify the SDX cluster.
   * @return endPointId
  **/
  @ApiModelProperty(value = "A string to uniquely identify the SDX cluster.")


  public String getEndPointId() {
    return endPointId;
  }

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
  @ApiModelProperty(value = "List of endPoints exported by this SDX cluster.")

  @Valid

  public List<ApiEndPoint> getEndPoints() {
    return endPoints;
  }

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
  @ApiModelProperty(value = "SDX cluster specifc options.")

  @Valid

  public List<ApiMapEntry> getConfigs() {
    return configs;
  }

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
  @ApiModelProperty(value = "Cluster version")


  public String getClusterVersion() {
    return clusterVersion;
  }

  public void setClusterVersion(String clusterVersion) {
    this.clusterVersion = clusterVersion;
  }


  @Override
  public boolean equals(java.lang.Object o) {
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
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

