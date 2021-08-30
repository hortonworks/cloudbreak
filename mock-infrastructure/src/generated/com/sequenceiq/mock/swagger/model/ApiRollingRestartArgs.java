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
 * Arguments used for Rolling Restart commands.
 */
@ApiModel(description = "Arguments used for Rolling Restart commands.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRollingRestartArgs   {
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

  @JsonProperty("restartRoleTypes")
  @Valid
  private List<String> restartRoleTypes = null;

  @JsonProperty("restartRoleNames")
  @Valid
  private List<String> restartRoleNames = null;

  public ApiRollingRestartArgs slaveBatchSize(Integer slaveBatchSize) {
    this.slaveBatchSize = slaveBatchSize;
    return this;
  }

  /**
   * Number of slave roles to restart at a time. Must be greater than zero. Default is 1.  Please note that for HDFS, this number should be less than the replication factor (default 3) to ensure data availability during rolling restart.
   * @return slaveBatchSize
  **/
  @ApiModelProperty(value = "Number of slave roles to restart at a time. Must be greater than zero. Default is 1.  Please note that for HDFS, this number should be less than the replication factor (default 3) to ensure data availability during rolling restart.")


  public Integer getSlaveBatchSize() {
    return slaveBatchSize;
  }

  public void setSlaveBatchSize(Integer slaveBatchSize) {
    this.slaveBatchSize = slaveBatchSize;
  }

  public ApiRollingRestartArgs sleepSeconds(Integer sleepSeconds) {
    this.sleepSeconds = sleepSeconds;
    return this;
  }

  /**
   * Number of seconds to sleep between restarts of slave role batches.  Must be greater than or equal to 0. Default is 0.
   * @return sleepSeconds
  **/
  @ApiModelProperty(value = "Number of seconds to sleep between restarts of slave role batches.  Must be greater than or equal to 0. Default is 0.")


  public Integer getSleepSeconds() {
    return sleepSeconds;
  }

  public void setSleepSeconds(Integer sleepSeconds) {
    this.sleepSeconds = sleepSeconds;
  }

  public ApiRollingRestartArgs slaveFailCountThreshold(Integer slaveFailCountThreshold) {
    this.slaveFailCountThreshold = slaveFailCountThreshold;
    return this;
  }

  /**
   * The threshold for number of slave batches that are allowed to fail to restart before the entire command is considered failed.  Must be greather than or equal to 0. Default is 0. <p> This argument is for ADVANCED users only. </p>
   * @return slaveFailCountThreshold
  **/
  @ApiModelProperty(value = "The threshold for number of slave batches that are allowed to fail to restart before the entire command is considered failed.  Must be greather than or equal to 0. Default is 0. <p> This argument is for ADVANCED users only. </p>")


  public Integer getSlaveFailCountThreshold() {
    return slaveFailCountThreshold;
  }

  public void setSlaveFailCountThreshold(Integer slaveFailCountThreshold) {
    this.slaveFailCountThreshold = slaveFailCountThreshold;
  }

  public ApiRollingRestartArgs staleConfigsOnly(Boolean staleConfigsOnly) {
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

  public ApiRollingRestartArgs unUpgradedOnly(Boolean unUpgradedOnly) {
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

  public ApiRollingRestartArgs restartRoleTypes(List<String> restartRoleTypes) {
    this.restartRoleTypes = restartRoleTypes;
    return this;
  }

  public ApiRollingRestartArgs addRestartRoleTypesItem(String restartRoleTypesItem) {
    if (this.restartRoleTypes == null) {
      this.restartRoleTypes = new ArrayList<>();
    }
    this.restartRoleTypes.add(restartRoleTypesItem);
    return this;
  }

  /**
   * Role types to restart. If not specified, all startable roles are restarted.  Both role types and role names should not be specified.
   * @return restartRoleTypes
  **/
  @ApiModelProperty(value = "Role types to restart. If not specified, all startable roles are restarted.  Both role types and role names should not be specified.")


  public List<String> getRestartRoleTypes() {
    return restartRoleTypes;
  }

  public void setRestartRoleTypes(List<String> restartRoleTypes) {
    this.restartRoleTypes = restartRoleTypes;
  }

  public ApiRollingRestartArgs restartRoleNames(List<String> restartRoleNames) {
    this.restartRoleNames = restartRoleNames;
    return this;
  }

  public ApiRollingRestartArgs addRestartRoleNamesItem(String restartRoleNamesItem) {
    if (this.restartRoleNames == null) {
      this.restartRoleNames = new ArrayList<>();
    }
    this.restartRoleNames.add(restartRoleNamesItem);
    return this;
  }

  /**
   * List of specific roles to restart. If none are specified, then all roles of specified role types are restarted.  Both role types and role names should not be specified.
   * @return restartRoleNames
  **/
  @ApiModelProperty(value = "List of specific roles to restart. If none are specified, then all roles of specified role types are restarted.  Both role types and role names should not be specified.")


  public List<String> getRestartRoleNames() {
    return restartRoleNames;
  }

  public void setRestartRoleNames(List<String> restartRoleNames) {
    this.restartRoleNames = restartRoleNames;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiRollingRestartArgs apiRollingRestartArgs = (ApiRollingRestartArgs) o;
    return Objects.equals(this.slaveBatchSize, apiRollingRestartArgs.slaveBatchSize) &&
        Objects.equals(this.sleepSeconds, apiRollingRestartArgs.sleepSeconds) &&
        Objects.equals(this.slaveFailCountThreshold, apiRollingRestartArgs.slaveFailCountThreshold) &&
        Objects.equals(this.staleConfigsOnly, apiRollingRestartArgs.staleConfigsOnly) &&
        Objects.equals(this.unUpgradedOnly, apiRollingRestartArgs.unUpgradedOnly) &&
        Objects.equals(this.restartRoleTypes, apiRollingRestartArgs.restartRoleTypes) &&
        Objects.equals(this.restartRoleNames, apiRollingRestartArgs.restartRoleNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slaveBatchSize, sleepSeconds, slaveFailCountThreshold, staleConfigsOnly, unUpgradedOnly, restartRoleTypes, restartRoleNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRollingRestartArgs {\n");
    
    sb.append("    slaveBatchSize: ").append(toIndentedString(slaveBatchSize)).append("\n");
    sb.append("    sleepSeconds: ").append(toIndentedString(sleepSeconds)).append("\n");
    sb.append("    slaveFailCountThreshold: ").append(toIndentedString(slaveFailCountThreshold)).append("\n");
    sb.append("    staleConfigsOnly: ").append(toIndentedString(staleConfigsOnly)).append("\n");
    sb.append("    unUpgradedOnly: ").append(toIndentedString(unUpgradedOnly)).append("\n");
    sb.append("    restartRoleTypes: ").append(toIndentedString(restartRoleTypes)).append("\n");
    sb.append("    restartRoleNames: ").append(toIndentedString(restartRoleNames)).append("\n");
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

