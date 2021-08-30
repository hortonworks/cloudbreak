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
 * Arguments used for enable Llama HA command.
 */
@ApiModel(description = "Arguments used for enable Llama HA command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiEnableLlamaHaArguments   {
  @JsonProperty("newLlamaHostId")
  private String newLlamaHostId = null;

  @JsonProperty("newLlamaRoleName")
  private String newLlamaRoleName = null;

  @JsonProperty("zkServiceName")
  private String zkServiceName = null;

  public ApiEnableLlamaHaArguments newLlamaHostId(String newLlamaHostId) {
    this.newLlamaHostId = newLlamaHostId;
    return this;
  }

  /**
   * HostId of the host on which the second Llama role will be created.
   * @return newLlamaHostId
  **/
  @ApiModelProperty(value = "HostId of the host on which the second Llama role will be created.")


  public String getNewLlamaHostId() {
    return newLlamaHostId;
  }

  public void setNewLlamaHostId(String newLlamaHostId) {
    this.newLlamaHostId = newLlamaHostId;
  }

  public ApiEnableLlamaHaArguments newLlamaRoleName(String newLlamaRoleName) {
    this.newLlamaRoleName = newLlamaRoleName;
    return this;
  }

  /**
   * Name of the second Llama role to be created (optional).
   * @return newLlamaRoleName
  **/
  @ApiModelProperty(value = "Name of the second Llama role to be created (optional).")


  public String getNewLlamaRoleName() {
    return newLlamaRoleName;
  }

  public void setNewLlamaRoleName(String newLlamaRoleName) {
    this.newLlamaRoleName = newLlamaRoleName;
  }

  public ApiEnableLlamaHaArguments zkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
    return this;
  }

  /**
   * Name of the ZooKeeper service that will be used for auto-failover. This argument may be omitted if the ZooKeeper dependency for Impala is already configured.
   * @return zkServiceName
  **/
  @ApiModelProperty(value = "Name of the ZooKeeper service that will be used for auto-failover. This argument may be omitted if the ZooKeeper dependency for Impala is already configured.")


  public String getZkServiceName() {
    return zkServiceName;
  }

  public void setZkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEnableLlamaHaArguments apiEnableLlamaHaArguments = (ApiEnableLlamaHaArguments) o;
    return Objects.equals(this.newLlamaHostId, apiEnableLlamaHaArguments.newLlamaHostId) &&
        Objects.equals(this.newLlamaRoleName, apiEnableLlamaHaArguments.newLlamaRoleName) &&
        Objects.equals(this.zkServiceName, apiEnableLlamaHaArguments.zkServiceName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(newLlamaHostId, newLlamaRoleName, zkServiceName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEnableLlamaHaArguments {\n");
    
    sb.append("    newLlamaHostId: ").append(toIndentedString(newLlamaHostId)).append("\n");
    sb.append("    newLlamaRoleName: ").append(toIndentedString(newLlamaRoleName)).append("\n");
    sb.append("    zkServiceName: ").append(toIndentedString(zkServiceName)).append("\n");
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

