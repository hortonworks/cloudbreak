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
 * Arguments used to install CDP a Private Cloud Control Plane
 */
@ApiModel(description = "Arguments used to install CDP a Private Cloud Control Plane")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiCreateEnvironment   {
  @JsonProperty("cmUser")
  private String cmUser = null;

  @JsonProperty("cmPass")
  private String cmPass = null;

  @JsonProperty("cmAddress")
  private String cmAddress = null;

  @JsonProperty("clusterName")
  private String clusterName = null;

  @JsonProperty("envName")
  private String envName = null;

  public ApiCreateEnvironment cmUser(String cmUser) {
    this.cmUser = cmUser;
    return this;
  }

  /**
   * 
   * @return cmUser
  **/
  @ApiModelProperty(value = "")


  public String getCmUser() {
    return cmUser;
  }

  public void setCmUser(String cmUser) {
    this.cmUser = cmUser;
  }

  public ApiCreateEnvironment cmPass(String cmPass) {
    this.cmPass = cmPass;
    return this;
  }

  /**
   * 
   * @return cmPass
  **/
  @ApiModelProperty(value = "")


  public String getCmPass() {
    return cmPass;
  }

  public void setCmPass(String cmPass) {
    this.cmPass = cmPass;
  }

  public ApiCreateEnvironment cmAddress(String cmAddress) {
    this.cmAddress = cmAddress;
    return this;
  }

  /**
   * 
   * @return cmAddress
  **/
  @ApiModelProperty(value = "")


  public String getCmAddress() {
    return cmAddress;
  }

  public void setCmAddress(String cmAddress) {
    this.cmAddress = cmAddress;
  }

  public ApiCreateEnvironment clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  /**
   * 
   * @return clusterName
  **/
  @ApiModelProperty(value = "")


  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public ApiCreateEnvironment envName(String envName) {
    this.envName = envName;
    return this;
  }

  /**
   * 
   * @return envName
  **/
  @ApiModelProperty(value = "")


  public String getEnvName() {
    return envName;
  }

  public void setEnvName(String envName) {
    this.envName = envName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCreateEnvironment apiCreateEnvironment = (ApiCreateEnvironment) o;
    return Objects.equals(this.cmUser, apiCreateEnvironment.cmUser) &&
        Objects.equals(this.cmPass, apiCreateEnvironment.cmPass) &&
        Objects.equals(this.cmAddress, apiCreateEnvironment.cmAddress) &&
        Objects.equals(this.clusterName, apiCreateEnvironment.clusterName) &&
        Objects.equals(this.envName, apiCreateEnvironment.envName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cmUser, cmPass, cmAddress, clusterName, envName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCreateEnvironment {\n");
    
    sb.append("    cmUser: ").append(toIndentedString(cmUser)).append("\n");
    sb.append("    cmPass: ").append(toIndentedString(cmPass)).append("\n");
    sb.append("    cmAddress: ").append(toIndentedString(cmAddress)).append("\n");
    sb.append("    clusterName: ").append(toIndentedString(clusterName)).append("\n");
    sb.append("    envName: ").append(toIndentedString(envName)).append("\n");
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

