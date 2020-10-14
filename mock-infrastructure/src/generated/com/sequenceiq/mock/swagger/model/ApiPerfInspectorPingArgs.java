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
 * Arguments to run ping test.
 */
@ApiModel(description = "Arguments to run ping test.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiPerfInspectorPingArgs   {
  @JsonProperty("pingTimeoutSecs")
  private BigDecimal pingTimeoutSecs = null;

  @JsonProperty("pingCount")
  private BigDecimal pingCount = null;

  @JsonProperty("pingPacketSizeBytes")
  private BigDecimal pingPacketSizeBytes = null;

  public ApiPerfInspectorPingArgs pingTimeoutSecs(BigDecimal pingTimeoutSecs) {
    this.pingTimeoutSecs = pingTimeoutSecs;
    return this;
  }

  /**
   * Timeout in seconds for the ping request to each target host. If not specified, defaults to 10 seconds. Must be a value between 1 and 3600 seconds, inclusive.
   * @return pingTimeoutSecs
  **/
  @ApiModelProperty(example = "10.0", value = "Timeout in seconds for the ping request to each target host. If not specified, defaults to 10 seconds. Must be a value between 1 and 3600 seconds, inclusive.")

  @Valid

  public BigDecimal getPingTimeoutSecs() {
    return pingTimeoutSecs;
  }

  public void setPingTimeoutSecs(BigDecimal pingTimeoutSecs) {
    this.pingTimeoutSecs = pingTimeoutSecs;
  }

  public ApiPerfInspectorPingArgs pingCount(BigDecimal pingCount) {
    this.pingCount = pingCount;
    return this;
  }

  /**
   * Number of iterations of the ping request to each target host. If not specified, defaults to 10 count.
   * @return pingCount
  **/
  @ApiModelProperty(example = "10.0", value = "Number of iterations of the ping request to each target host. If not specified, defaults to 10 count.")

  @Valid

  public BigDecimal getPingCount() {
    return pingCount;
  }

  public void setPingCount(BigDecimal pingCount) {
    this.pingCount = pingCount;
  }

  public ApiPerfInspectorPingArgs pingPacketSizeBytes(BigDecimal pingPacketSizeBytes) {
    this.pingPacketSizeBytes = pingPacketSizeBytes;
    return this;
  }

  /**
   * Packet size in bytes for each ping request. If not specified, defaults to 56 bytes. Must be a value between 1 and 65507 bytes, inclusive.
   * @return pingPacketSizeBytes
  **/
  @ApiModelProperty(example = "56.0", value = "Packet size in bytes for each ping request. If not specified, defaults to 56 bytes. Must be a value between 1 and 65507 bytes, inclusive.")

  @Valid

  public BigDecimal getPingPacketSizeBytes() {
    return pingPacketSizeBytes;
  }

  public void setPingPacketSizeBytes(BigDecimal pingPacketSizeBytes) {
    this.pingPacketSizeBytes = pingPacketSizeBytes;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiPerfInspectorPingArgs apiPerfInspectorPingArgs = (ApiPerfInspectorPingArgs) o;
    return Objects.equals(this.pingTimeoutSecs, apiPerfInspectorPingArgs.pingTimeoutSecs) &&
        Objects.equals(this.pingCount, apiPerfInspectorPingArgs.pingCount) &&
        Objects.equals(this.pingPacketSizeBytes, apiPerfInspectorPingArgs.pingPacketSizeBytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pingTimeoutSecs, pingCount, pingPacketSizeBytes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiPerfInspectorPingArgs {\n");
    
    sb.append("    pingTimeoutSecs: ").append(toIndentedString(pingTimeoutSecs)).append("\n");
    sb.append("    pingCount: ").append(toIndentedString(pingCount)).append("\n");
    sb.append("    pingPacketSizeBytes: ").append(toIndentedString(pingPacketSizeBytes)).append("\n");
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

