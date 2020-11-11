package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Basic arguments used for Rolling Restart Cluster commands.
 */
@ApiModel(description = "Basic arguments used for Rolling Restart Cluster commands.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiSimpleRollingRestartClusterArgs   {
  @JsonProperty("slaveBatchSize")
  private BigDecimal slaveBatchSize = null;

  @JsonProperty("sleepSeconds")
  private BigDecimal sleepSeconds = null;

  @JsonProperty("slaveFailCountThreshold")
  private BigDecimal slaveFailCountThreshold = null;

  public ApiSimpleRollingRestartClusterArgs slaveBatchSize(BigDecimal slaveBatchSize) {
    this.slaveBatchSize = slaveBatchSize;
    return this;
  }

  /**
   * Number of hosts with slave roles to restart at a time. Must be greater than zero. Default is 1.
   * @return slaveBatchSize
  **/
  @ApiModelProperty(value = "Number of hosts with slave roles to restart at a time. Must be greater than zero. Default is 1.")

  @Valid

  public BigDecimal getSlaveBatchSize() {
    return slaveBatchSize;
  }

  public void setSlaveBatchSize(BigDecimal slaveBatchSize) {
    this.slaveBatchSize = slaveBatchSize;
  }

  public ApiSimpleRollingRestartClusterArgs sleepSeconds(BigDecimal sleepSeconds) {
    this.sleepSeconds = sleepSeconds;
    return this;
  }

  /**
   * Number of seconds to sleep between restarts of slave host batches. <p> Must be greater than or equal to 0. Default is 0.
   * @return sleepSeconds
  **/
  @ApiModelProperty(value = "Number of seconds to sleep between restarts of slave host batches. <p> Must be greater than or equal to 0. Default is 0.")

  @Valid

  public BigDecimal getSleepSeconds() {
    return sleepSeconds;
  }

  public void setSleepSeconds(BigDecimal sleepSeconds) {
    this.sleepSeconds = sleepSeconds;
  }

  public ApiSimpleRollingRestartClusterArgs slaveFailCountThreshold(BigDecimal slaveFailCountThreshold) {
    this.slaveFailCountThreshold = slaveFailCountThreshold;
    return this;
  }

  /**
   * The threshold for number of slave host batches that are allowed to fail to restart before the entire command is considered failed. <p> Must be greater than or equal to 0. Default is 0. <p> This argument is for ADVANCED users only. </p>
   * @return slaveFailCountThreshold
  **/
  @ApiModelProperty(value = "The threshold for number of slave host batches that are allowed to fail to restart before the entire command is considered failed. <p> Must be greater than or equal to 0. Default is 0. <p> This argument is for ADVANCED users only. </p>")

  @Valid

  public BigDecimal getSlaveFailCountThreshold() {
    return slaveFailCountThreshold;
  }

  public void setSlaveFailCountThreshold(BigDecimal slaveFailCountThreshold) {
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
    ApiSimpleRollingRestartClusterArgs apiSimpleRollingRestartClusterArgs = (ApiSimpleRollingRestartClusterArgs) o;
    return Objects.equals(this.slaveBatchSize, apiSimpleRollingRestartClusterArgs.slaveBatchSize) &&
        Objects.equals(this.sleepSeconds, apiSimpleRollingRestartClusterArgs.sleepSeconds) &&
        Objects.equals(this.slaveFailCountThreshold, apiSimpleRollingRestartClusterArgs.slaveFailCountThreshold);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slaveBatchSize, sleepSeconds, slaveFailCountThreshold);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiSimpleRollingRestartClusterArgs {\n");
    
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

