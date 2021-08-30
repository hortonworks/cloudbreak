package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiPerfInspectorBandwidthArgs;
import com.sequenceiq.mock.swagger.model.ApiPerfInspectorPingArgs;
import com.sequenceiq.mock.swagger.model.PerfInspectorPolicyType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used for the Cloudera Manager level performance inspector. Network diagnostics will be run from every host in sourceCluster to every host in targetCluster.
 */
@ApiModel(description = "Arguments used for the Cloudera Manager level performance inspector. Network diagnostics will be run from every host in sourceCluster to every host in targetCluster.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClustersPerfInspectorArgs   {
  @JsonProperty("sourceCluster")
  private String sourceCluster = null;

  @JsonProperty("targetCluster")
  private String targetCluster = null;

  @JsonProperty("pingArgs")
  private ApiPerfInspectorPingArgs pingArgs = null;

  @JsonProperty("bandwidthArgs")
  private ApiPerfInspectorBandwidthArgs bandwidthArgs = null;

  @JsonProperty("policyType")
  private PerfInspectorPolicyType policyType = null;

  public ApiClustersPerfInspectorArgs sourceCluster(String sourceCluster) {
    this.sourceCluster = sourceCluster;
    return this;
  }

  /**
   * Required name of the source cluster to run network diagnostics test.
   * @return sourceCluster
  **/
  @ApiModelProperty(value = "Required name of the source cluster to run network diagnostics test.")


  public String getSourceCluster() {
    return sourceCluster;
  }

  public void setSourceCluster(String sourceCluster) {
    this.sourceCluster = sourceCluster;
  }

  public ApiClustersPerfInspectorArgs targetCluster(String targetCluster) {
    this.targetCluster = targetCluster;
    return this;
  }

  /**
   * Required name of the target cluster to run network diagnostics test.
   * @return targetCluster
  **/
  @ApiModelProperty(value = "Required name of the target cluster to run network diagnostics test.")


  public String getTargetCluster() {
    return targetCluster;
  }

  public void setTargetCluster(String targetCluster) {
    this.targetCluster = targetCluster;
  }

  public ApiClustersPerfInspectorArgs pingArgs(ApiPerfInspectorPingArgs pingArgs) {
    this.pingArgs = pingArgs;
    return this;
  }

  /**
   * Optional ping request arguments. If not specified, default arguments will be used for ping test.
   * @return pingArgs
  **/
  @ApiModelProperty(value = "Optional ping request arguments. If not specified, default arguments will be used for ping test.")

  @Valid

  public ApiPerfInspectorPingArgs getPingArgs() {
    return pingArgs;
  }

  public void setPingArgs(ApiPerfInspectorPingArgs pingArgs) {
    this.pingArgs = pingArgs;
  }

  public ApiClustersPerfInspectorArgs bandwidthArgs(ApiPerfInspectorBandwidthArgs bandwidthArgs) {
    this.bandwidthArgs = bandwidthArgs;
    return this;
  }

  /**
   * Optional bandwidth test request arguments. If not specified, default arguments will be used for bandwidth test. Applicable since version v32.
   * @return bandwidthArgs
  **/
  @ApiModelProperty(value = "Optional bandwidth test request arguments. If not specified, default arguments will be used for bandwidth test. Applicable since version v32.")

  @Valid

  public ApiPerfInspectorBandwidthArgs getBandwidthArgs() {
    return bandwidthArgs;
  }

  public void setBandwidthArgs(ApiPerfInspectorBandwidthArgs bandwidthArgs) {
    this.bandwidthArgs = bandwidthArgs;
  }

  public ApiClustersPerfInspectorArgs policyType(PerfInspectorPolicyType policyType) {
    this.policyType = policyType;
    return this;
  }

  /**
   * Optional type of performance diagnostics to run. If not specified, defaults to FULL policy type. Applicable since version v32.
   * @return policyType
  **/
  @ApiModelProperty(value = "Optional type of performance diagnostics to run. If not specified, defaults to FULL policy type. Applicable since version v32.")

  @Valid

  public PerfInspectorPolicyType getPolicyType() {
    return policyType;
  }

  public void setPolicyType(PerfInspectorPolicyType policyType) {
    this.policyType = policyType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClustersPerfInspectorArgs apiClustersPerfInspectorArgs = (ApiClustersPerfInspectorArgs) o;
    return Objects.equals(this.sourceCluster, apiClustersPerfInspectorArgs.sourceCluster) &&
        Objects.equals(this.targetCluster, apiClustersPerfInspectorArgs.targetCluster) &&
        Objects.equals(this.pingArgs, apiClustersPerfInspectorArgs.pingArgs) &&
        Objects.equals(this.bandwidthArgs, apiClustersPerfInspectorArgs.bandwidthArgs) &&
        Objects.equals(this.policyType, apiClustersPerfInspectorArgs.policyType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceCluster, targetCluster, pingArgs, bandwidthArgs, policyType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClustersPerfInspectorArgs {\n");
    
    sb.append("    sourceCluster: ").append(toIndentedString(sourceCluster)).append("\n");
    sb.append("    targetCluster: ").append(toIndentedString(targetCluster)).append("\n");
    sb.append("    pingArgs: ").append(toIndentedString(pingArgs)).append("\n");
    sb.append("    bandwidthArgs: ").append(toIndentedString(bandwidthArgs)).append("\n");
    sb.append("    policyType: ").append(toIndentedString(policyType)).append("\n");
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

