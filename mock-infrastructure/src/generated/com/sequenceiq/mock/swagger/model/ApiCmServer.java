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
 * This is the model for CM server information from v41.  The class models the host environment that CM server is installed. Each CM server instance has an entry of CmServer entity and updates it periodically.
 */
@ApiModel(description = "This is the model for CM server information from v41.  The class models the host environment that CM server is installed. Each CM server instance has an entry of CmServer entity and updates it periodically.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiCmServer   {
  @JsonProperty("cmServerId")
  private String cmServerId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("ipAddress")
  private String ipAddress = null;

  @JsonProperty("createdTime")
  private String createdTime = null;

  @JsonProperty("lastUpdatedTime")
  private String lastUpdatedTime = null;

  public ApiCmServer cmServerId(String cmServerId) {
    this.cmServerId = cmServerId;
    return this;
  }

  /**
   * The CM server ID.
   * @return cmServerId
  **/
  @ApiModelProperty(value = "The CM server ID.")


  public String getCmServerId() {
    return cmServerId;
  }

  public void setCmServerId(String cmServerId) {
    this.cmServerId = cmServerId;
  }

  public ApiCmServer name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The CM server hostname.
   * @return name
  **/
  @ApiModelProperty(value = "The CM server hostname.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiCmServer ipAddress(String ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }

  /**
   * The IP address.
   * @return ipAddress
  **/
  @ApiModelProperty(value = "The IP address.")


  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public ApiCmServer createdTime(String createdTime) {
    this.createdTime = createdTime;
    return this;
  }

  /**
   * The created time.
   * @return createdTime
  **/
  @ApiModelProperty(value = "The created time.")


  public String getCreatedTime() {
    return createdTime;
  }

  public void setCreatedTime(String createdTime) {
    this.createdTime = createdTime;
  }

  public ApiCmServer lastUpdatedTime(String lastUpdatedTime) {
    this.lastUpdatedTime = lastUpdatedTime;
    return this;
  }

  /**
   * The last updated time.
   * @return lastUpdatedTime
  **/
  @ApiModelProperty(value = "The last updated time.")


  public String getLastUpdatedTime() {
    return lastUpdatedTime;
  }

  public void setLastUpdatedTime(String lastUpdatedTime) {
    this.lastUpdatedTime = lastUpdatedTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCmServer apiCmServer = (ApiCmServer) o;
    return Objects.equals(this.cmServerId, apiCmServer.cmServerId) &&
        Objects.equals(this.name, apiCmServer.name) &&
        Objects.equals(this.ipAddress, apiCmServer.ipAddress) &&
        Objects.equals(this.createdTime, apiCmServer.createdTime) &&
        Objects.equals(this.lastUpdatedTime, apiCmServer.lastUpdatedTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cmServerId, name, ipAddress, createdTime, lastUpdatedTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCmServer {\n");
    
    sb.append("    cmServerId: ").append(toIndentedString(cmServerId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    ipAddress: ").append(toIndentedString(ipAddress)).append("\n");
    sb.append("    createdTime: ").append(toIndentedString(createdTime)).append("\n");
    sb.append("    lastUpdatedTime: ").append(toIndentedString(lastUpdatedTime)).append("\n");
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

