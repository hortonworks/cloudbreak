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
 * Rolling upgrade arguments used in the CDH Upgrade Command. Part of ApiCdhUpgradeArgs.
 */
@ApiModel(description = "Rolling upgrade arguments used in the CDH Upgrade Command. Part of ApiCdhUpgradeArgs.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRollingUpgradeClusterArgs   {
  @JsonProperty("slaveBatchSize")
  private Integer slaveBatchSize = null;

  @JsonProperty("sleepSeconds")
  private Integer sleepSeconds = null;

  @JsonProperty("slaveFailCountThreshold")
  private Integer slaveFailCountThreshold = null;

  public ApiRollingUpgradeClusterArgs slaveBatchSize(Integer slaveBatchSize) {
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

  public ApiRollingUpgradeClusterArgs sleepSeconds(Integer sleepSeconds) {
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

  public ApiRollingUpgradeClusterArgs slaveFailCountThreshold(Integer slaveFailCountThreshold) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiRollingUpgradeClusterArgs apiRollingUpgradeClusterArgs = (ApiRollingUpgradeClusterArgs) o;
    return Objects.equals(this.slaveBatchSize, apiRollingUpgradeClusterArgs.slaveBatchSize) &&
        Objects.equals(this.sleepSeconds, apiRollingUpgradeClusterArgs.sleepSeconds) &&
        Objects.equals(this.slaveFailCountThreshold, apiRollingUpgradeClusterArgs.slaveFailCountThreshold);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slaveBatchSize, sleepSeconds, slaveFailCountThreshold);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRollingUpgradeClusterArgs {\n");
    
    sb.append("    slaveBatchSize: ").append(toIndentedString(slaveBatchSize)).append("\n");
    sb.append("    sleepSeconds: ").append(toIndentedString(sleepSeconds)).append("\n");
    sb.append("    slaveFailCountThreshold: ").append(toIndentedString(slaveFailCountThreshold)).append("\n");
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

