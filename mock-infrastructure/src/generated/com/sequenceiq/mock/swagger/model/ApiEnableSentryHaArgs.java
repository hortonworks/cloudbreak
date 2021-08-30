package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiSimpleRollingRestartClusterArgs;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used for enable Sentry HA command.
 */
@ApiModel(description = "Arguments used for enable Sentry HA command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiEnableSentryHaArgs   {
  @JsonProperty("newSentryHostId")
  private String newSentryHostId = null;

  @JsonProperty("newSentryRoleName")
  private String newSentryRoleName = null;

  @JsonProperty("zkServiceName")
  private String zkServiceName = null;

  @JsonProperty("rrcArgs")
  private ApiSimpleRollingRestartClusterArgs rrcArgs = null;

  public ApiEnableSentryHaArgs newSentryHostId(String newSentryHostId) {
    this.newSentryHostId = newSentryHostId;
    return this;
  }

  /**
   * Id of host on which new Sentry Server role will be added.
   * @return newSentryHostId
  **/
  @ApiModelProperty(value = "Id of host on which new Sentry Server role will be added.")


  public String getNewSentryHostId() {
    return newSentryHostId;
  }

  public void setNewSentryHostId(String newSentryHostId) {
    this.newSentryHostId = newSentryHostId;
  }

  public ApiEnableSentryHaArgs newSentryRoleName(String newSentryRoleName) {
    this.newSentryRoleName = newSentryRoleName;
    return this;
  }

  /**
   * Name of the new Sentry Server role to be created. This is an optional argument.
   * @return newSentryRoleName
  **/
  @ApiModelProperty(value = "Name of the new Sentry Server role to be created. This is an optional argument.")


  public String getNewSentryRoleName() {
    return newSentryRoleName;
  }

  public void setNewSentryRoleName(String newSentryRoleName) {
    this.newSentryRoleName = newSentryRoleName;
  }

  public ApiEnableSentryHaArgs zkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
    return this;
  }

  /**
   * Name of the ZooKeeper service that will be used for Sentry HA. This is an optional parameter if the Sentry to ZooKeeper dependency is already set in CM.
   * @return zkServiceName
  **/
  @ApiModelProperty(value = "Name of the ZooKeeper service that will be used for Sentry HA. This is an optional parameter if the Sentry to ZooKeeper dependency is already set in CM.")


  public String getZkServiceName() {
    return zkServiceName;
  }

  public void setZkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
  }

  public ApiEnableSentryHaArgs rrcArgs(ApiSimpleRollingRestartClusterArgs rrcArgs) {
    this.rrcArgs = rrcArgs;
    return this;
  }

  /**
   * 
   * @return rrcArgs
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiSimpleRollingRestartClusterArgs getRrcArgs() {
    return rrcArgs;
  }

  public void setRrcArgs(ApiSimpleRollingRestartClusterArgs rrcArgs) {
    this.rrcArgs = rrcArgs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEnableSentryHaArgs apiEnableSentryHaArgs = (ApiEnableSentryHaArgs) o;
    return Objects.equals(this.newSentryHostId, apiEnableSentryHaArgs.newSentryHostId) &&
        Objects.equals(this.newSentryRoleName, apiEnableSentryHaArgs.newSentryRoleName) &&
        Objects.equals(this.zkServiceName, apiEnableSentryHaArgs.zkServiceName) &&
        Objects.equals(this.rrcArgs, apiEnableSentryHaArgs.rrcArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(newSentryHostId, newSentryRoleName, zkServiceName, rrcArgs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEnableSentryHaArgs {\n");
    
    sb.append("    newSentryHostId: ").append(toIndentedString(newSentryHostId)).append("\n");
    sb.append("    newSentryRoleName: ").append(toIndentedString(newSentryRoleName)).append("\n");
    sb.append("    zkServiceName: ").append(toIndentedString(zkServiceName)).append("\n");
    sb.append("    rrcArgs: ").append(toIndentedString(rrcArgs)).append("\n");
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

