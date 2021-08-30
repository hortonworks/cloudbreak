package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiOsDistributionType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Definition of an OS distribution.
 */
@ApiModel(description = "Definition of an OS distribution.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiOsDistribution   {
  @JsonProperty("distributionType")
  private ApiOsDistributionType distributionType = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("version")
  private String version = null;

  public ApiOsDistribution distributionType(ApiOsDistributionType distributionType) {
    this.distributionType = distributionType;
    return this;
  }

  /**
   * High-level OS distribution type
   * @return distributionType
  **/
  @ApiModelProperty(value = "High-level OS distribution type")

  @Valid

  public ApiOsDistributionType getDistributionType() {
    return distributionType;
  }

  public void setDistributionType(ApiOsDistributionType distributionType) {
    this.distributionType = distributionType;
  }

  public ApiOsDistribution name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Distribution full name; 'redhat', 'centos', 'ubuntu' etc
   * @return name
  **/
  @ApiModelProperty(value = "Distribution full name; 'redhat', 'centos', 'ubuntu' etc")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiOsDistribution version(String version) {
    this.version = version;
    return this;
  }

  /**
   * Full OS version string; eg. 7.3.1611
   * @return version
  **/
  @ApiModelProperty(value = "Full OS version string; eg. 7.3.1611")


  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiOsDistribution apiOsDistribution = (ApiOsDistribution) o;
    return Objects.equals(this.distributionType, apiOsDistribution.distributionType) &&
        Objects.equals(this.name, apiOsDistribution.name) &&
        Objects.equals(this.version, apiOsDistribution.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(distributionType, name, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiOsDistribution {\n");
    
    sb.append("    distributionType: ").append(toIndentedString(distributionType)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

