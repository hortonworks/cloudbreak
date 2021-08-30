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
 * Arguments used for enable JT HA command.
 */
@ApiModel(description = "Arguments used for enable JT HA command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiEnableJtHaArguments   {
  @JsonProperty("newJtHostId")
  private String newJtHostId = null;

  @JsonProperty("forceInitZNode")
  private Boolean forceInitZNode = null;

  @JsonProperty("zkServiceName")
  private String zkServiceName = null;

  @JsonProperty("newJtRoleName")
  private String newJtRoleName = null;

  @JsonProperty("fc1RoleName")
  private String fc1RoleName = null;

  @JsonProperty("fc2RoleName")
  private String fc2RoleName = null;

  @JsonProperty("logicalName")
  private String logicalName = null;

  public ApiEnableJtHaArguments newJtHostId(String newJtHostId) {
    this.newJtHostId = newJtHostId;
    return this;
  }

  /**
   * Id of host on which second JobTracker role will be added.
   * @return newJtHostId
  **/
  @ApiModelProperty(value = "Id of host on which second JobTracker role will be added.")


  public String getNewJtHostId() {
    return newJtHostId;
  }

  public void setNewJtHostId(String newJtHostId) {
    this.newJtHostId = newJtHostId;
  }

  public ApiEnableJtHaArguments forceInitZNode(Boolean forceInitZNode) {
    this.forceInitZNode = forceInitZNode;
    return this;
  }

  /**
   * Initialize the ZNode even if it already exists. This can happen if JobTracker HA was enabled before and then disabled. Disable operation doesn't delete this ZNode. Defaults to true.
   * @return forceInitZNode
  **/
  @ApiModelProperty(value = "Initialize the ZNode even if it already exists. This can happen if JobTracker HA was enabled before and then disabled. Disable operation doesn't delete this ZNode. Defaults to true.")


  public Boolean isForceInitZNode() {
    return forceInitZNode;
  }

  public void setForceInitZNode(Boolean forceInitZNode) {
    this.forceInitZNode = forceInitZNode;
  }

  public ApiEnableJtHaArguments zkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
    return this;
  }

  /**
   * Name of the ZooKeeper service that will be used for auto-failover. This is an optional parameter if the MapReduce to ZooKeeper dependency is already set in CM.
   * @return zkServiceName
  **/
  @ApiModelProperty(value = "Name of the ZooKeeper service that will be used for auto-failover. This is an optional parameter if the MapReduce to ZooKeeper dependency is already set in CM.")


  public String getZkServiceName() {
    return zkServiceName;
  }

  public void setZkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
  }

  public ApiEnableJtHaArguments newJtRoleName(String newJtRoleName) {
    this.newJtRoleName = newJtRoleName;
    return this;
  }

  /**
   * Name of the second JobTracker role to be created (Optional)
   * @return newJtRoleName
  **/
  @ApiModelProperty(value = "Name of the second JobTracker role to be created (Optional)")


  public String getNewJtRoleName() {
    return newJtRoleName;
  }

  public void setNewJtRoleName(String newJtRoleName) {
    this.newJtRoleName = newJtRoleName;
  }

  public ApiEnableJtHaArguments fc1RoleName(String fc1RoleName) {
    this.fc1RoleName = fc1RoleName;
    return this;
  }

  /**
   * Name of first Failover Controller role to be created. This is the Failover Controller co-located with the current JobTracker (Optional)
   * @return fc1RoleName
  **/
  @ApiModelProperty(value = "Name of first Failover Controller role to be created. This is the Failover Controller co-located with the current JobTracker (Optional)")


  public String getFc1RoleName() {
    return fc1RoleName;
  }

  public void setFc1RoleName(String fc1RoleName) {
    this.fc1RoleName = fc1RoleName;
  }

  public ApiEnableJtHaArguments fc2RoleName(String fc2RoleName) {
    this.fc2RoleName = fc2RoleName;
    return this;
  }

  /**
   * Name of second Failover Controller role to be created. This is the Failover Controller co-located with the new JobTracker (Optional)
   * @return fc2RoleName
  **/
  @ApiModelProperty(value = "Name of second Failover Controller role to be created. This is the Failover Controller co-located with the new JobTracker (Optional)")


  public String getFc2RoleName() {
    return fc2RoleName;
  }

  public void setFc2RoleName(String fc2RoleName) {
    this.fc2RoleName = fc2RoleName;
  }

  public ApiEnableJtHaArguments logicalName(String logicalName) {
    this.logicalName = logicalName;
    return this;
  }

  /**
   * Logical name of the JobTracker pair. If value is not provided, \"logicaljt\" is used as the default. The name can contain only alphanumeric characters and \"-\". <p> Available since API v8.
   * @return logicalName
  **/
  @ApiModelProperty(value = "Logical name of the JobTracker pair. If value is not provided, \"logicaljt\" is used as the default. The name can contain only alphanumeric characters and \"-\". <p> Available since API v8.")


  public String getLogicalName() {
    return logicalName;
  }

  public void setLogicalName(String logicalName) {
    this.logicalName = logicalName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEnableJtHaArguments apiEnableJtHaArguments = (ApiEnableJtHaArguments) o;
    return Objects.equals(this.newJtHostId, apiEnableJtHaArguments.newJtHostId) &&
        Objects.equals(this.forceInitZNode, apiEnableJtHaArguments.forceInitZNode) &&
        Objects.equals(this.zkServiceName, apiEnableJtHaArguments.zkServiceName) &&
        Objects.equals(this.newJtRoleName, apiEnableJtHaArguments.newJtRoleName) &&
        Objects.equals(this.fc1RoleName, apiEnableJtHaArguments.fc1RoleName) &&
        Objects.equals(this.fc2RoleName, apiEnableJtHaArguments.fc2RoleName) &&
        Objects.equals(this.logicalName, apiEnableJtHaArguments.logicalName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(newJtHostId, forceInitZNode, zkServiceName, newJtRoleName, fc1RoleName, fc2RoleName, logicalName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEnableJtHaArguments {\n");
    
    sb.append("    newJtHostId: ").append(toIndentedString(newJtHostId)).append("\n");
    sb.append("    forceInitZNode: ").append(toIndentedString(forceInitZNode)).append("\n");
    sb.append("    zkServiceName: ").append(toIndentedString(zkServiceName)).append("\n");
    sb.append("    newJtRoleName: ").append(toIndentedString(newJtRoleName)).append("\n");
    sb.append("    fc1RoleName: ").append(toIndentedString(fc1RoleName)).append("\n");
    sb.append("    fc2RoleName: ").append(toIndentedString(fc2RoleName)).append("\n");
    sb.append("    logicalName: ").append(toIndentedString(logicalName)).append("\n");
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

