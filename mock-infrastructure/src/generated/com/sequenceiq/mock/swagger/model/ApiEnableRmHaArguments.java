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
 * Arguments used for enable RM HA command.
 */
@ApiModel(description = "Arguments used for enable RM HA command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiEnableRmHaArguments   {
  @JsonProperty("newRmHostId")
  private String newRmHostId = null;

  @JsonProperty("newRmRoleName")
  private String newRmRoleName = null;

  @JsonProperty("zkServiceName")
  private String zkServiceName = null;

  public ApiEnableRmHaArguments newRmHostId(String newRmHostId) {
    this.newRmHostId = newRmHostId;
    return this;
  }

  /**
   * Id of host on which second ResourceManager role will be added.
   * @return newRmHostId
  **/
  @ApiModelProperty(value = "Id of host on which second ResourceManager role will be added.")


  public String getNewRmHostId() {
    return newRmHostId;
  }

  public void setNewRmHostId(String newRmHostId) {
    this.newRmHostId = newRmHostId;
  }

  public ApiEnableRmHaArguments newRmRoleName(String newRmRoleName) {
    this.newRmRoleName = newRmRoleName;
    return this;
  }

  /**
   * Name of the second ResourceManager role to be created (Optional)
   * @return newRmRoleName
  **/
  @ApiModelProperty(value = "Name of the second ResourceManager role to be created (Optional)")


  public String getNewRmRoleName() {
    return newRmRoleName;
  }

  public void setNewRmRoleName(String newRmRoleName) {
    this.newRmRoleName = newRmRoleName;
  }

  public ApiEnableRmHaArguments zkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
    return this;
  }

  /**
   * Name of the ZooKeeper service that will be used for auto-failover. This is an optional parameter if the Yarn to ZooKeeper dependency is already set in CM.
   * @return zkServiceName
  **/
  @ApiModelProperty(value = "Name of the ZooKeeper service that will be used for auto-failover. This is an optional parameter if the Yarn to ZooKeeper dependency is already set in CM.")


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
    ApiEnableRmHaArguments apiEnableRmHaArguments = (ApiEnableRmHaArguments) o;
    return Objects.equals(this.newRmHostId, apiEnableRmHaArguments.newRmHostId) &&
        Objects.equals(this.newRmRoleName, apiEnableRmHaArguments.newRmRoleName) &&
        Objects.equals(this.zkServiceName, apiEnableRmHaArguments.zkServiceName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(newRmHostId, newRmRoleName, zkServiceName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEnableRmHaArguments {\n");
    
    sb.append("    newRmHostId: ").append(toIndentedString(newRmHostId)).append("\n");
    sb.append("    newRmRoleName: ").append(toIndentedString(newRmRoleName)).append("\n");
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

