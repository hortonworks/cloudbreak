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
 * Arguments used for Rolling Upgrade command.
 */
@ApiModel(description = "Arguments used for Rolling Upgrade command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRollingUpgradeServicesArgs   {
  @JsonProperty("upgradeFromCdhVersion")
  private String upgradeFromCdhVersion = null;

  @JsonProperty("upgradeToCdhVersion")
  private String upgradeToCdhVersion = null;

  @JsonProperty("slaveBatchSize")
  private Integer slaveBatchSize = null;

  @JsonProperty("sleepSeconds")
  private Integer sleepSeconds = null;

  @JsonProperty("slaveFailCountThreshold")
  private Integer slaveFailCountThreshold = null;

  @JsonProperty("upgradeServiceNames")
  @Valid
  private List<String> upgradeServiceNames = null;

  public ApiRollingUpgradeServicesArgs upgradeFromCdhVersion(String upgradeFromCdhVersion) {
    this.upgradeFromCdhVersion = upgradeFromCdhVersion;
    return this;
  }

  /**
   * Current CDH Version of the services. Example versions are: \"5.1.0\", \"5.2.2\" or \"5.4.0\"
   * @return upgradeFromCdhVersion
  **/
  @ApiModelProperty(value = "Current CDH Version of the services. Example versions are: \"5.1.0\", \"5.2.2\" or \"5.4.0\"")


  public String getUpgradeFromCdhVersion() {
    return upgradeFromCdhVersion;
  }

  public void setUpgradeFromCdhVersion(String upgradeFromCdhVersion) {
    this.upgradeFromCdhVersion = upgradeFromCdhVersion;
  }

  public ApiRollingUpgradeServicesArgs upgradeToCdhVersion(String upgradeToCdhVersion) {
    this.upgradeToCdhVersion = upgradeToCdhVersion;
    return this;
  }

  /**
   * Target CDH Version for the services. The CDH version should already be present and activated on the nodes. Example versions are: \"5.1.0\", \"5.2.2\" or \"5.4.0\"
   * @return upgradeToCdhVersion
  **/
  @ApiModelProperty(value = "Target CDH Version for the services. The CDH version should already be present and activated on the nodes. Example versions are: \"5.1.0\", \"5.2.2\" or \"5.4.0\"")


  public String getUpgradeToCdhVersion() {
    return upgradeToCdhVersion;
  }

  public void setUpgradeToCdhVersion(String upgradeToCdhVersion) {
    this.upgradeToCdhVersion = upgradeToCdhVersion;
  }

  public ApiRollingUpgradeServicesArgs slaveBatchSize(Integer slaveBatchSize) {
    this.slaveBatchSize = slaveBatchSize;
    return this;
  }

  /**
   * Number of hosts with slave roles to upgrade at a time. Must be greater than zero. Default is 1.
   * @return slaveBatchSize
  **/
  @ApiModelProperty(value = "Number of hosts with slave roles to upgrade at a time. Must be greater than zero. Default is 1.")


  public Integer getSlaveBatchSize() {
    return slaveBatchSize;
  }

  public void setSlaveBatchSize(Integer slaveBatchSize) {
    this.slaveBatchSize = slaveBatchSize;
  }

  public ApiRollingUpgradeServicesArgs sleepSeconds(Integer sleepSeconds) {
    this.sleepSeconds = sleepSeconds;
    return this;
  }

  /**
   * Number of seconds to sleep between restarts of slave host batches.  Must be greater than or equal to 0. Default is 0.
   * @return sleepSeconds
  **/
  @ApiModelProperty(value = "Number of seconds to sleep between restarts of slave host batches.  Must be greater than or equal to 0. Default is 0.")


  public Integer getSleepSeconds() {
    return sleepSeconds;
  }

  public void setSleepSeconds(Integer sleepSeconds) {
    this.sleepSeconds = sleepSeconds;
  }

  public ApiRollingUpgradeServicesArgs slaveFailCountThreshold(Integer slaveFailCountThreshold) {
    this.slaveFailCountThreshold = slaveFailCountThreshold;
    return this;
  }

  /**
   * The threshold for number of slave host batches that are allowed to fail to restart before the entire command is considered failed.  Must be greater than or equal to 0. Default is 0. <p> This argument is for ADVANCED users only. </p>
   * @return slaveFailCountThreshold
  **/
  @ApiModelProperty(value = "The threshold for number of slave host batches that are allowed to fail to restart before the entire command is considered failed.  Must be greater than or equal to 0. Default is 0. <p> This argument is for ADVANCED users only. </p>")


  public Integer getSlaveFailCountThreshold() {
    return slaveFailCountThreshold;
  }

  public void setSlaveFailCountThreshold(Integer slaveFailCountThreshold) {
    this.slaveFailCountThreshold = slaveFailCountThreshold;
  }

  public ApiRollingUpgradeServicesArgs upgradeServiceNames(List<String> upgradeServiceNames) {
    this.upgradeServiceNames = upgradeServiceNames;
    return this;
  }

  public ApiRollingUpgradeServicesArgs addUpgradeServiceNamesItem(String upgradeServiceNamesItem) {
    if (this.upgradeServiceNames == null) {
      this.upgradeServiceNames = new ArrayList<>();
    }
    this.upgradeServiceNames.add(upgradeServiceNamesItem);
    return this;
  }

  /**
   * List of services to upgrade. Only the services that support rolling upgrade should be included.
   * @return upgradeServiceNames
  **/
  @ApiModelProperty(value = "List of services to upgrade. Only the services that support rolling upgrade should be included.")


  public List<String> getUpgradeServiceNames() {
    return upgradeServiceNames;
  }

  public void setUpgradeServiceNames(List<String> upgradeServiceNames) {
    this.upgradeServiceNames = upgradeServiceNames;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiRollingUpgradeServicesArgs apiRollingUpgradeServicesArgs = (ApiRollingUpgradeServicesArgs) o;
    return Objects.equals(this.upgradeFromCdhVersion, apiRollingUpgradeServicesArgs.upgradeFromCdhVersion) &&
        Objects.equals(this.upgradeToCdhVersion, apiRollingUpgradeServicesArgs.upgradeToCdhVersion) &&
        Objects.equals(this.slaveBatchSize, apiRollingUpgradeServicesArgs.slaveBatchSize) &&
        Objects.equals(this.sleepSeconds, apiRollingUpgradeServicesArgs.sleepSeconds) &&
        Objects.equals(this.slaveFailCountThreshold, apiRollingUpgradeServicesArgs.slaveFailCountThreshold) &&
        Objects.equals(this.upgradeServiceNames, apiRollingUpgradeServicesArgs.upgradeServiceNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(upgradeFromCdhVersion, upgradeToCdhVersion, slaveBatchSize, sleepSeconds, slaveFailCountThreshold, upgradeServiceNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRollingUpgradeServicesArgs {\n");
    
    sb.append("    upgradeFromCdhVersion: ").append(toIndentedString(upgradeFromCdhVersion)).append("\n");
    sb.append("    upgradeToCdhVersion: ").append(toIndentedString(upgradeToCdhVersion)).append("\n");
    sb.append("    slaveBatchSize: ").append(toIndentedString(slaveBatchSize)).append("\n");
    sb.append("    sleepSeconds: ").append(toIndentedString(sleepSeconds)).append("\n");
    sb.append("    slaveFailCountThreshold: ").append(toIndentedString(slaveFailCountThreshold)).append("\n");
    sb.append("    upgradeServiceNames: ").append(toIndentedString(upgradeServiceNames)).append("\n");
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

