package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiRolesToInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used for Rolling Restart Cluster command.
 */
@ApiModel(description = "Arguments used for Rolling Restart Cluster command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRollingRestartClusterArgs   {
  @JsonProperty("slaveBatchSize")
  private Integer slaveBatchSize = null;

  @JsonProperty("sleepSeconds")
  private Integer sleepSeconds = null;

  @JsonProperty("slaveFailCountThreshold")
  private Integer slaveFailCountThreshold = null;

  @JsonProperty("staleConfigsOnly")
  private Boolean staleConfigsOnly = null;

  @JsonProperty("unUpgradedOnly")
  private Boolean unUpgradedOnly = null;

  @JsonProperty("redeployClientConfiguration")
  private Boolean redeployClientConfiguration = null;

  @JsonProperty("rolesToInclude")
  private ApiRolesToInclude rolesToInclude = null;

  @JsonProperty("restartServiceNames")
  @Valid
  private List<String> restartServiceNames = null;

  public ApiRollingRestartClusterArgs slaveBatchSize(Integer slaveBatchSize) {
    this.slaveBatchSize = slaveBatchSize;
    return this;
  }

  /**
   * Number of hosts with slave roles to restart at a time. Must be greater than zero. Default is 1.
   * @return slaveBatchSize
  **/
  @ApiModelProperty(value = "Number of hosts with slave roles to restart at a time. Must be greater than zero. Default is 1.")


  public Integer getSlaveBatchSize() {
    return slaveBatchSize;
  }

  public void setSlaveBatchSize(Integer slaveBatchSize) {
    this.slaveBatchSize = slaveBatchSize;
  }

  public ApiRollingRestartClusterArgs sleepSeconds(Integer sleepSeconds) {
    this.sleepSeconds = sleepSeconds;
    return this;
  }

  /**
   * Number of seconds to sleep between restarts of slave host batches. <p> Must be greater than or equal to 0. Default is 0.
   * @return sleepSeconds
  **/
  @ApiModelProperty(value = "Number of seconds to sleep between restarts of slave host batches. <p> Must be greater than or equal to 0. Default is 0.")


  public Integer getSleepSeconds() {
    return sleepSeconds;
  }

  public void setSleepSeconds(Integer sleepSeconds) {
    this.sleepSeconds = sleepSeconds;
  }

  public ApiRollingRestartClusterArgs slaveFailCountThreshold(Integer slaveFailCountThreshold) {
    this.slaveFailCountThreshold = slaveFailCountThreshold;
    return this;
  }

  /**
   * The threshold for number of slave host batches that are allowed to fail to restart before the entire command is considered failed. <p> Must be greater than or equal to 0. Default is 0. <p> This argument is for ADVANCED users only. </p>
   * @return slaveFailCountThreshold
  **/
  @ApiModelProperty(value = "The threshold for number of slave host batches that are allowed to fail to restart before the entire command is considered failed. <p> Must be greater than or equal to 0. Default is 0. <p> This argument is for ADVANCED users only. </p>")


  public Integer getSlaveFailCountThreshold() {
    return slaveFailCountThreshold;
  }

  public void setSlaveFailCountThreshold(Integer slaveFailCountThreshold) {
    this.slaveFailCountThreshold = slaveFailCountThreshold;
  }

  public ApiRollingRestartClusterArgs staleConfigsOnly(Boolean staleConfigsOnly) {
    this.staleConfigsOnly = staleConfigsOnly;
    return this;
  }

  /**
   * Restart roles with stale configs only.
   * @return staleConfigsOnly
  **/
  @ApiModelProperty(value = "Restart roles with stale configs only.")


  public Boolean isStaleConfigsOnly() {
    return staleConfigsOnly;
  }

  public void setStaleConfigsOnly(Boolean staleConfigsOnly) {
    this.staleConfigsOnly = staleConfigsOnly;
  }

  public ApiRollingRestartClusterArgs unUpgradedOnly(Boolean unUpgradedOnly) {
    this.unUpgradedOnly = unUpgradedOnly;
    return this;
  }

  /**
   * Restart roles that haven't been upgraded yet.
   * @return unUpgradedOnly
  **/
  @ApiModelProperty(value = "Restart roles that haven't been upgraded yet.")


  public Boolean isUnUpgradedOnly() {
    return unUpgradedOnly;
  }

  public void setUnUpgradedOnly(Boolean unUpgradedOnly) {
    this.unUpgradedOnly = unUpgradedOnly;
  }

  public ApiRollingRestartClusterArgs redeployClientConfiguration(Boolean redeployClientConfiguration) {
    this.redeployClientConfiguration = redeployClientConfiguration;
    return this;
  }

  /**
   * Re-deploy client configuration. Available since API v6.
   * @return redeployClientConfiguration
  **/
  @ApiModelProperty(value = "Re-deploy client configuration. Available since API v6.")


  public Boolean isRedeployClientConfiguration() {
    return redeployClientConfiguration;
  }

  public void setRedeployClientConfiguration(Boolean redeployClientConfiguration) {
    this.redeployClientConfiguration = redeployClientConfiguration;
  }

  public ApiRollingRestartClusterArgs rolesToInclude(ApiRolesToInclude rolesToInclude) {
    this.rolesToInclude = rolesToInclude;
    return this;
  }

  /**
   * Role types to restart. Default is slave roles only.
   * @return rolesToInclude
  **/
  @ApiModelProperty(value = "Role types to restart. Default is slave roles only.")

  @Valid

  public ApiRolesToInclude getRolesToInclude() {
    return rolesToInclude;
  }

  public void setRolesToInclude(ApiRolesToInclude rolesToInclude) {
    this.rolesToInclude = rolesToInclude;
  }

  public ApiRollingRestartClusterArgs restartServiceNames(List<String> restartServiceNames) {
    this.restartServiceNames = restartServiceNames;
    return this;
  }

  public ApiRollingRestartClusterArgs addRestartServiceNamesItem(String restartServiceNamesItem) {
    if (this.restartServiceNames == null) {
      this.restartServiceNames = new ArrayList<>();
    }
    this.restartServiceNames.add(restartServiceNamesItem);
    return this;
  }

  /**
   * List of services to restart.
   * @return restartServiceNames
  **/
  @ApiModelProperty(value = "List of services to restart.")


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
    ApiRollingRestartClusterArgs apiRollingRestartClusterArgs = (ApiRollingRestartClusterArgs) o;
    return Objects.equals(this.slaveBatchSize, apiRollingRestartClusterArgs.slaveBatchSize) &&
        Objects.equals(this.sleepSeconds, apiRollingRestartClusterArgs.sleepSeconds) &&
        Objects.equals(this.slaveFailCountThreshold, apiRollingRestartClusterArgs.slaveFailCountThreshold) &&
        Objects.equals(this.staleConfigsOnly, apiRollingRestartClusterArgs.staleConfigsOnly) &&
        Objects.equals(this.unUpgradedOnly, apiRollingRestartClusterArgs.unUpgradedOnly) &&
        Objects.equals(this.redeployClientConfiguration, apiRollingRestartClusterArgs.redeployClientConfiguration) &&
        Objects.equals(this.rolesToInclude, apiRollingRestartClusterArgs.rolesToInclude) &&
        Objects.equals(this.restartServiceNames, apiRollingRestartClusterArgs.restartServiceNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slaveBatchSize, sleepSeconds, slaveFailCountThreshold, staleConfigsOnly, unUpgradedOnly, redeployClientConfiguration, rolesToInclude, restartServiceNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRollingRestartClusterArgs {\n");
    
    sb.append("    slaveBatchSize: ").append(toIndentedString(slaveBatchSize)).append("\n");
    sb.append("    sleepSeconds: ").append(toIndentedString(sleepSeconds)).append("\n");
    sb.append("    slaveFailCountThreshold: ").append(toIndentedString(slaveFailCountThreshold)).append("\n");
    sb.append("    staleConfigsOnly: ").append(toIndentedString(staleConfigsOnly)).append("\n");
    sb.append("    unUpgradedOnly: ").append(toIndentedString(unUpgradedOnly)).append("\n");
    sb.append("    redeployClientConfiguration: ").append(toIndentedString(redeployClientConfiguration)).append("\n");
    sb.append("    rolesToInclude: ").append(toIndentedString(rolesToInclude)).append("\n");
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

