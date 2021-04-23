package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Command args for HBaseReplicationSetupCommand
 */
@ApiModel(description = "Command args for HBaseReplicationSetupCommand")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiHBaseReplicationSetupCommandArgs   {
  @JsonProperty("keystorePassword")
  private String keystorePassword = null;

  @JsonProperty("replicationUser")
  private String replicationUser = null;

  @JsonProperty("sourceRef")
  private ApiServiceRef sourceRef = null;

  @JsonProperty("cmPeerDefinitionName")
  private String cmPeerDefinitionName = null;

  public ApiHBaseReplicationSetupCommandArgs keystorePassword(String keystorePassword) {
    this.keystorePassword = keystorePassword;
    return this;
  }

  /**
   * 
   * @return keystorePassword
  **/
  @ApiModelProperty(value = "")


  public String getKeystorePassword() {
    return keystorePassword;
  }

  public void setKeystorePassword(String keystorePassword) {
    this.keystorePassword = keystorePassword;
  }

  public ApiHBaseReplicationSetupCommandArgs replicationUser(String replicationUser) {
    this.replicationUser = replicationUser;
    return this;
  }

  /**
   * 
   * @return replicationUser
  **/
  @ApiModelProperty(value = "")


  public String getReplicationUser() {
    return replicationUser;
  }

  public void setReplicationUser(String replicationUser) {
    this.replicationUser = replicationUser;
  }

  public ApiHBaseReplicationSetupCommandArgs sourceRef(ApiServiceRef sourceRef) {
    this.sourceRef = sourceRef;
    return this;
  }

  /**
   * 
   * @return sourceRef
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiServiceRef getSourceRef() {
    return sourceRef;
  }

  public void setSourceRef(ApiServiceRef sourceRef) {
    this.sourceRef = sourceRef;
  }

  public ApiHBaseReplicationSetupCommandArgs cmPeerDefinitionName(String cmPeerDefinitionName) {
    this.cmPeerDefinitionName = cmPeerDefinitionName;
    return this;
  }

  /**
   * 
   * @return cmPeerDefinitionName
  **/
  @ApiModelProperty(value = "")


  public String getCmPeerDefinitionName() {
    return cmPeerDefinitionName;
  }

  public void setCmPeerDefinitionName(String cmPeerDefinitionName) {
    this.cmPeerDefinitionName = cmPeerDefinitionName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHBaseReplicationSetupCommandArgs apiHBaseReplicationSetupCommandArgs = (ApiHBaseReplicationSetupCommandArgs) o;
    return Objects.equals(this.keystorePassword, apiHBaseReplicationSetupCommandArgs.keystorePassword) &&
        Objects.equals(this.replicationUser, apiHBaseReplicationSetupCommandArgs.replicationUser) &&
        Objects.equals(this.sourceRef, apiHBaseReplicationSetupCommandArgs.sourceRef) &&
        Objects.equals(this.cmPeerDefinitionName, apiHBaseReplicationSetupCommandArgs.cmPeerDefinitionName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keystorePassword, replicationUser, sourceRef, cmPeerDefinitionName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHBaseReplicationSetupCommandArgs {\n");
    
    sb.append("    keystorePassword: ").append(toIndentedString(keystorePassword)).append("\n");
    sb.append("    replicationUser: ").append(toIndentedString(replicationUser)).append("\n");
    sb.append("    sourceRef: ").append(toIndentedString(sourceRef)).append("\n");
    sb.append("    cmPeerDefinitionName: ").append(toIndentedString(cmPeerDefinitionName)).append("\n");
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

