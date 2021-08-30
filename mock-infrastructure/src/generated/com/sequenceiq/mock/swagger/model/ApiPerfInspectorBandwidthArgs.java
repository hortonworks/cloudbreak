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
 * Arguments to run bandwidth diagnostics as part of performance inspector. Requires iperf3 package installed on hosts.
 */
@ApiModel(description = "Arguments to run bandwidth diagnostics as part of performance inspector. Requires iperf3 package installed on hosts.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiPerfInspectorBandwidthArgs   {
  @JsonProperty("runBandwidthDiagnostics")
  private Boolean runBandwidthDiagnostics = null;

  @JsonProperty("bandwidthTimeoutSecs")
  private Integer bandwidthTimeoutSecs = null;

  public ApiPerfInspectorBandwidthArgs runBandwidthDiagnostics(Boolean runBandwidthDiagnostics) {
    this.runBandwidthDiagnostics = runBandwidthDiagnostics;
    return this;
  }

  /**
   * Optional flag to run bandwidth diagnostics test. Exercise caution, running bandwidth test will have an impact on currently running workloads. If not specified, defaults to false.
   * @return runBandwidthDiagnostics
  **/
  @ApiModelProperty(example = "false", value = "Optional flag to run bandwidth diagnostics test. Exercise caution, running bandwidth test will have an impact on currently running workloads. If not specified, defaults to false.")


  public Boolean isRunBandwidthDiagnostics() {
    return runBandwidthDiagnostics;
  }

  public void setRunBandwidthDiagnostics(Boolean runBandwidthDiagnostics) {
    this.runBandwidthDiagnostics = runBandwidthDiagnostics;
  }

  public ApiPerfInspectorBandwidthArgs bandwidthTimeoutSecs(Integer bandwidthTimeoutSecs) {
    this.bandwidthTimeoutSecs = bandwidthTimeoutSecs;
    return this;
  }

  /**
   * Timeout in seconds for the bandwidth request to each target host. If not specified, defaults to 10 seconds.
   * @return bandwidthTimeoutSecs
  **/
  @ApiModelProperty(example = "10", value = "Timeout in seconds for the bandwidth request to each target host. If not specified, defaults to 10 seconds.")


  public Integer getBandwidthTimeoutSecs() {
    return bandwidthTimeoutSecs;
  }

  public void setBandwidthTimeoutSecs(Integer bandwidthTimeoutSecs) {
    this.bandwidthTimeoutSecs = bandwidthTimeoutSecs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiPerfInspectorBandwidthArgs apiPerfInspectorBandwidthArgs = (ApiPerfInspectorBandwidthArgs) o;
    return Objects.equals(this.runBandwidthDiagnostics, apiPerfInspectorBandwidthArgs.runBandwidthDiagnostics) &&
        Objects.equals(this.bandwidthTimeoutSecs, apiPerfInspectorBandwidthArgs.bandwidthTimeoutSecs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(runBandwidthDiagnostics, bandwidthTimeoutSecs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiPerfInspectorBandwidthArgs {\n");
    
    sb.append("    runBandwidthDiagnostics: ").append(toIndentedString(runBandwidthDiagnostics)).append("\n");
    sb.append("    bandwidthTimeoutSecs: ").append(toIndentedString(bandwidthTimeoutSecs)).append("\n");
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

