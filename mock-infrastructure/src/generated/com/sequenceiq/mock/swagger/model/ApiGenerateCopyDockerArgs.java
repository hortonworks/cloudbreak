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
 * Arguments used for the command to generate the docker copy script.
 */
@ApiModel(description = "Arguments used for the command to generate the docker copy script.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiGenerateCopyDockerArgs   {
  @JsonProperty("remoteRepoUrl")
  private String remoteRepoUrl = null;

  @JsonProperty("dockerRegistry")
  private String dockerRegistry = null;

  @JsonProperty("controlPlaneUuid")
  private String controlPlaneUuid = null;

  public ApiGenerateCopyDockerArgs remoteRepoUrl(String remoteRepoUrl) {
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

  public ApiGenerateCopyDockerArgs dockerRegistry(String dockerRegistry) {
    this.dockerRegistry = dockerRegistry;
    return this;
  }

  /**
   * The url of the Docker Registry where images required for install will be copied to
   * @return dockerRegistry
  **/
  @ApiModelProperty(value = "The url of the Docker Registry where images required for install will be copied to")


  public String getDockerRegistry() {
    return dockerRegistry;
  }

  public void setDockerRegistry(String dockerRegistry) {
    this.dockerRegistry = dockerRegistry;
  }

  public ApiGenerateCopyDockerArgs controlPlaneUuid(String controlPlaneUuid) {
    this.controlPlaneUuid = controlPlaneUuid;
    return this;
  }

  /**
   * Optional. The uuid of the control plane, if copying docker images for an upgrade
   * @return controlPlaneUuid
  **/
  @ApiModelProperty(value = "Optional. The uuid of the control plane, if copying docker images for an upgrade")


  public String getControlPlaneUuid() {
    return controlPlaneUuid;
  }

  public void setControlPlaneUuid(String controlPlaneUuid) {
    this.controlPlaneUuid = controlPlaneUuid;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiGenerateCopyDockerArgs apiGenerateCopyDockerArgs = (ApiGenerateCopyDockerArgs) o;
    return Objects.equals(this.remoteRepoUrl, apiGenerateCopyDockerArgs.remoteRepoUrl) &&
        Objects.equals(this.dockerRegistry, apiGenerateCopyDockerArgs.dockerRegistry) &&
        Objects.equals(this.controlPlaneUuid, apiGenerateCopyDockerArgs.controlPlaneUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(remoteRepoUrl, dockerRegistry, controlPlaneUuid);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiGenerateCopyDockerArgs {\n");
    
    sb.append("    remoteRepoUrl: ").append(toIndentedString(remoteRepoUrl)).append("\n");
    sb.append("    dockerRegistry: ").append(toIndentedString(dockerRegistry)).append("\n");
    sb.append("    controlPlaneUuid: ").append(toIndentedString(controlPlaneUuid)).append("\n");
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

