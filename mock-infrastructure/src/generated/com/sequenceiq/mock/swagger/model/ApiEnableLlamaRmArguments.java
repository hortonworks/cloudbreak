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
 * Arguments used for enable Llama RM command.
 */
@ApiModel(description = "Arguments used for enable Llama RM command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiEnableLlamaRmArguments   {
  @JsonProperty("llama1HostId")
  private String llama1HostId = null;

  @JsonProperty("llama1RoleName")
  private String llama1RoleName = null;

  @JsonProperty("llama2HostId")
  private String llama2HostId = null;

  @JsonProperty("llama2RoleName")
  private String llama2RoleName = null;

  @JsonProperty("zkServiceName")
  private String zkServiceName = null;

  @JsonProperty("skipRestart")
  private Boolean skipRestart = null;

  public ApiEnableLlamaRmArguments llama1HostId(String llama1HostId) {
    this.llama1HostId = llama1HostId;
    return this;
  }

  /**
   * HostId of the host on which the first Llama role will be created.
   * @return llama1HostId
  **/
  @ApiModelProperty(value = "HostId of the host on which the first Llama role will be created.")


  public String getLlama1HostId() {
    return llama1HostId;
  }

  public void setLlama1HostId(String llama1HostId) {
    this.llama1HostId = llama1HostId;
  }

  public ApiEnableLlamaRmArguments llama1RoleName(String llama1RoleName) {
    this.llama1RoleName = llama1RoleName;
    return this;
  }

  /**
   * Name of the first Llama role to be created (optional).
   * @return llama1RoleName
  **/
  @ApiModelProperty(value = "Name of the first Llama role to be created (optional).")


  public String getLlama1RoleName() {
    return llama1RoleName;
  }

  public void setLlama1RoleName(String llama1RoleName) {
    this.llama1RoleName = llama1RoleName;
  }

  public ApiEnableLlamaRmArguments llama2HostId(String llama2HostId) {
    this.llama2HostId = llama2HostId;
    return this;
  }

  /**
   * HostId of the host on which the second Llama role will be created.
   * @return llama2HostId
  **/
  @ApiModelProperty(value = "HostId of the host on which the second Llama role will be created.")


  public String getLlama2HostId() {
    return llama2HostId;
  }

  public void setLlama2HostId(String llama2HostId) {
    this.llama2HostId = llama2HostId;
  }

  public ApiEnableLlamaRmArguments llama2RoleName(String llama2RoleName) {
    this.llama2RoleName = llama2RoleName;
    return this;
  }

  /**
   * Name of the second Llama role to be created (optional).
   * @return llama2RoleName
  **/
  @ApiModelProperty(value = "Name of the second Llama role to be created (optional).")


  public String getLlama2RoleName() {
    return llama2RoleName;
  }

  public void setLlama2RoleName(String llama2RoleName) {
    this.llama2RoleName = llama2RoleName;
  }

  public ApiEnableLlamaRmArguments zkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
    return this;
  }

  /**
   * Name of the ZooKeeper service that will be used for auto-failover. Only relevant when enabling Llama RM in HA mode (i.e., when two Llama roles are being created). This argument may be omitted if the ZooKeeper dependency for Impala is already configured.
   * @return zkServiceName
  **/
  @ApiModelProperty(value = "Name of the ZooKeeper service that will be used for auto-failover. Only relevant when enabling Llama RM in HA mode (i.e., when two Llama roles are being created). This argument may be omitted if the ZooKeeper dependency for Impala is already configured.")


  public String getZkServiceName() {
    return zkServiceName;
  }

  public void setZkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
  }

  public ApiEnableLlamaRmArguments skipRestart(Boolean skipRestart) {
    this.skipRestart = skipRestart;
    return this;
  }

  /**
   * Skip the restart of Yarn, Impala, and their dependent services, and don't deploy client configuration. Default is false (i.e., by default, the services are restarted and client configuration is deployed).
   * @return skipRestart
  **/
  @ApiModelProperty(required = true, value = "Skip the restart of Yarn, Impala, and their dependent services, and don't deploy client configuration. Default is false (i.e., by default, the services are restarted and client configuration is deployed).")
  @NotNull


  public Boolean isSkipRestart() {
    return skipRestart;
  }

  public void setSkipRestart(Boolean skipRestart) {
    this.skipRestart = skipRestart;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEnableLlamaRmArguments apiEnableLlamaRmArguments = (ApiEnableLlamaRmArguments) o;
    return Objects.equals(this.llama1HostId, apiEnableLlamaRmArguments.llama1HostId) &&
        Objects.equals(this.llama1RoleName, apiEnableLlamaRmArguments.llama1RoleName) &&
        Objects.equals(this.llama2HostId, apiEnableLlamaRmArguments.llama2HostId) &&
        Objects.equals(this.llama2RoleName, apiEnableLlamaRmArguments.llama2RoleName) &&
        Objects.equals(this.zkServiceName, apiEnableLlamaRmArguments.zkServiceName) &&
        Objects.equals(this.skipRestart, apiEnableLlamaRmArguments.skipRestart);
  }

  @Override
  public int hashCode() {
    return Objects.hash(llama1HostId, llama1RoleName, llama2HostId, llama2RoleName, zkServiceName, skipRestart);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEnableLlamaRmArguments {\n");
    
    sb.append("    llama1HostId: ").append(toIndentedString(llama1HostId)).append("\n");
    sb.append("    llama1RoleName: ").append(toIndentedString(llama1RoleName)).append("\n");
    sb.append("    llama2HostId: ").append(toIndentedString(llama2HostId)).append("\n");
    sb.append("    llama2RoleName: ").append(toIndentedString(llama2RoleName)).append("\n");
    sb.append("    zkServiceName: ").append(toIndentedString(zkServiceName)).append("\n");
    sb.append("    skipRestart: ").append(toIndentedString(skipRestart)).append("\n");
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

