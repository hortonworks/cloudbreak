package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used to install a Private Cloud Control Plane on embedded kubernetes
 */
@ApiModel(description = "Arguments used to install a Private Cloud Control Plane on embedded kubernetes")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiInstallEmbeddedControlPlaneArgs   {
  @JsonProperty("remoteRepoUrl")
  private String remoteRepoUrl = null;

  @JsonProperty("valuesYaml")
  private String valuesYaml = null;

  @JsonProperty("experienceClusterName")
  private String experienceClusterName = null;

  @JsonProperty("datalakeClusterName")
  private String datalakeClusterName = null;

  public ApiInstallEmbeddedControlPlaneArgs remoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
    return this;
  }

  /**
   * The url of the remote repository where the private cloud artifacts to install are hosted
   * @return remoteRepoUrl
  **/
  @ApiModelProperty(value = "The url of the remote repository where the private cloud artifacts to install are hosted")


  public String getRemoteRepoUrl() {
    return remoteRepoUrl;
  }

  public void setRemoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
  }

  public ApiInstallEmbeddedControlPlaneArgs valuesYaml(String valuesYaml) {
    this.valuesYaml = valuesYaml;
    return this;
  }

  /**
   * A yaml file containing configuration parameters for the installation.
   * @return valuesYaml
  **/
  @ApiModelProperty(value = "A yaml file containing configuration parameters for the installation.")


  public String getValuesYaml() {
    return valuesYaml;
  }

  public void setValuesYaml(String valuesYaml) {
    this.valuesYaml = valuesYaml;
  }

  public ApiInstallEmbeddedControlPlaneArgs experienceClusterName(String experienceClusterName) {
    this.experienceClusterName = experienceClusterName;
    return this;
  }

  /**
   * The name of the Experience cluster that will bring up this control plane
   * @return experienceClusterName
  **/
  @ApiModelProperty(value = "The name of the Experience cluster that will bring up this control plane")


  public String getExperienceClusterName() {
    return experienceClusterName;
  }

  public void setExperienceClusterName(String experienceClusterName) {
    this.experienceClusterName = experienceClusterName;
  }

  public ApiInstallEmbeddedControlPlaneArgs datalakeClusterName(String datalakeClusterName) {
    this.datalakeClusterName = datalakeClusterName;
    return this;
  }

  /**
   * The name of the datalake cluster to use for the initial environment in this control plane
   * @return datalakeClusterName
  **/
  @ApiModelProperty(value = "The name of the datalake cluster to use for the initial environment in this control plane")


  public String getDatalakeClusterName() {
    return datalakeClusterName;
  }

  public void setDatalakeClusterName(String datalakeClusterName) {
    this.datalakeClusterName = datalakeClusterName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiInstallEmbeddedControlPlaneArgs apiInstallEmbeddedControlPlaneArgs = (ApiInstallEmbeddedControlPlaneArgs) o;
    return Objects.equals(this.remoteRepoUrl, apiInstallEmbeddedControlPlaneArgs.remoteRepoUrl) &&
        Objects.equals(this.valuesYaml, apiInstallEmbeddedControlPlaneArgs.valuesYaml) &&
        Objects.equals(this.experienceClusterName, apiInstallEmbeddedControlPlaneArgs.experienceClusterName) &&
        Objects.equals(this.datalakeClusterName, apiInstallEmbeddedControlPlaneArgs.datalakeClusterName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(remoteRepoUrl, valuesYaml, experienceClusterName, datalakeClusterName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiInstallEmbeddedControlPlaneArgs {\n");
    
    sb.append("    remoteRepoUrl: ").append(toIndentedString(remoteRepoUrl)).append("\n");
    sb.append("    valuesYaml: ").append(toIndentedString(valuesYaml)).append("\n");
    sb.append("    experienceClusterName: ").append(toIndentedString(experienceClusterName)).append("\n");
    sb.append("    datalakeClusterName: ").append(toIndentedString(datalakeClusterName)).append("\n");
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

