package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used for Cluster Restart command.  Since V11: If both restartOnlyStaleServices and restartServiceNames are specified, a service must be specified in restartServiceNames and also be stale, in order to be restarted.
 */
@ApiModel(description = "Arguments used for Cluster Restart command.  Since V11: If both restartOnlyStaleServices and restartServiceNames are specified, a service must be specified in restartServiceNames and also be stale, in order to be restarted.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRestartClusterArgs   {
  @JsonProperty("restartOnlyStaleServices")
  private Boolean restartOnlyStaleServices = null;

  @JsonProperty("redeployClientConfiguration")
  private Boolean redeployClientConfiguration = null;

  @JsonProperty("restartServiceNames")
  @Valid
  private List<String> restartServiceNames = null;

  public ApiRestartClusterArgs restartOnlyStaleServices(Boolean restartOnlyStaleServices) {
    this.restartOnlyStaleServices = restartOnlyStaleServices;
    return this;
  }

  /**
   * Only restart services that have stale configuration and their dependent services.
   * @return restartOnlyStaleServices
  **/
  @ApiModelProperty(value = "Only restart services that have stale configuration and their dependent services.")


  public Boolean isRestartOnlyStaleServices() {
    return restartOnlyStaleServices;
  }

  public void setRestartOnlyStaleServices(Boolean restartOnlyStaleServices) {
    this.restartOnlyStaleServices = restartOnlyStaleServices;
  }

  public ApiRestartClusterArgs redeployClientConfiguration(Boolean redeployClientConfiguration) {
    this.redeployClientConfiguration = redeployClientConfiguration;
    return this;
  }

  /**
   * Re-deploy client configuration for all services in the cluster.
   * @return redeployClientConfiguration
  **/
  @ApiModelProperty(value = "Re-deploy client configuration for all services in the cluster.")


  public Boolean isRedeployClientConfiguration() {
    return redeployClientConfiguration;
  }

  public void setRedeployClientConfiguration(Boolean redeployClientConfiguration) {
    this.redeployClientConfiguration = redeployClientConfiguration;
  }

  public ApiRestartClusterArgs restartServiceNames(List<String> restartServiceNames) {
    this.restartServiceNames = restartServiceNames;
    return this;
  }

  public ApiRestartClusterArgs addRestartServiceNamesItem(String restartServiceNamesItem) {
    if (this.restartServiceNames == null) {
      this.restartServiceNames = new ArrayList<>();
    }
    this.restartServiceNames.add(restartServiceNamesItem);
    return this;
  }

  /**
   * Only restart services that are specified and their dependent services. Available since V11.
   * @return restartServiceNames
  **/
  @ApiModelProperty(value = "Only restart services that are specified and their dependent services. Available since V11.")


  public List<String> getRestartServiceNames() {
    return restartServiceNames;
  }

  public void setRestartServiceNames(List<String> restartServiceNames) {
    this.restartServiceNames = restartServiceNames;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiRestartClusterArgs apiRestartClusterArgs = (ApiRestartClusterArgs) o;
    return Objects.equals(this.restartOnlyStaleServices, apiRestartClusterArgs.restartOnlyStaleServices) &&
        Objects.equals(this.redeployClientConfiguration, apiRestartClusterArgs.redeployClientConfiguration) &&
        Objects.equals(this.restartServiceNames, apiRestartClusterArgs.restartServiceNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(restartOnlyStaleServices, redeployClientConfiguration, restartServiceNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRestartClusterArgs {\n");
    
    sb.append("    restartOnlyStaleServices: ").append(toIndentedString(restartOnlyStaleServices)).append("\n");
    sb.append("    redeployClientConfiguration: ").append(toIndentedString(redeployClientConfiguration)).append("\n");
    sb.append("    restartServiceNames: ").append(toIndentedString(restartServiceNames)).append("\n");
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

