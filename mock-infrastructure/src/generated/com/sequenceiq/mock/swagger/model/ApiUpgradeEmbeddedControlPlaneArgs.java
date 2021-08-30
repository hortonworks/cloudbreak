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
 * Arguments used to upgrade a Private Cloud Control Plane on embedded kubernetes and the associated experience cluster.
 */
@ApiModel(description = "Arguments used to upgrade a Private Cloud Control Plane on embedded kubernetes and the associated experience cluster.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiUpgradeEmbeddedControlPlaneArgs   {
  @JsonProperty("remoteRepoUrl")
  private String remoteRepoUrl = null;

  @JsonProperty("valuesYaml")
  private String valuesYaml = null;

  @JsonProperty("experienceClusterName")
  private String experienceClusterName = null;

  public ApiUpgradeEmbeddedControlPlaneArgs remoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
    return this;
  }

  /**
   * The url of the remote repository where the private cloud artifacts to upgrade to are hosted
   * @return remoteRepoUrl
  **/
  @ApiModelProperty(value = "The url of the remote repository where the private cloud artifacts to upgrade to are hosted")


  public String getRemoteRepoUrl() {
    return remoteRepoUrl;
  }

  public void setRemoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
  }

  public ApiUpgradeEmbeddedControlPlaneArgs valuesYaml(String valuesYaml) {
    this.valuesYaml = valuesYaml;
    return this;
  }

  /**
   * A yaml file containing configuration parameters for the upgrade.
   * @return valuesYaml
  **/
  @ApiModelProperty(value = "A yaml file containing configuration parameters for the upgrade.")


  public String getValuesYaml() {
    return valuesYaml;
  }

  public void setValuesYaml(String valuesYaml) {
    this.valuesYaml = valuesYaml;
  }

  public ApiUpgradeEmbeddedControlPlaneArgs experienceClusterName(String experienceClusterName) {
    this.experienceClusterName = experienceClusterName;
    return this;
  }

  /**
   * The name of the existing Experience cluster to upgrade.
   * @return experienceClusterName
  **/
  @ApiModelProperty(value = "The name of the existing Experience cluster to upgrade.")


  public String getExperienceClusterName() {
    return experienceClusterName;
  }

  public void setExperienceClusterName(String experienceClusterName) {
    this.experienceClusterName = experienceClusterName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiUpgradeEmbeddedControlPlaneArgs apiUpgradeEmbeddedControlPlaneArgs = (ApiUpgradeEmbeddedControlPlaneArgs) o;
    return Objects.equals(this.remoteRepoUrl, apiUpgradeEmbeddedControlPlaneArgs.remoteRepoUrl) &&
        Objects.equals(this.valuesYaml, apiUpgradeEmbeddedControlPlaneArgs.valuesYaml) &&
        Objects.equals(this.experienceClusterName, apiUpgradeEmbeddedControlPlaneArgs.experienceClusterName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(remoteRepoUrl, valuesYaml, experienceClusterName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiUpgradeEmbeddedControlPlaneArgs {\n");
    
    sb.append("    remoteRepoUrl: ").append(toIndentedString(remoteRepoUrl)).append("\n");
    sb.append("    valuesYaml: ").append(toIndentedString(valuesYaml)).append("\n");
    sb.append("    experienceClusterName: ").append(toIndentedString(experienceClusterName)).append("\n");
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

