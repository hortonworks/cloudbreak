package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiJournalNodeArguments;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used for Enable NameNode High Availability command.
 */
@ApiModel(description = "Arguments used for Enable NameNode High Availability command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiEnableNnHaArguments   {
  @JsonProperty("activeNnName")
  private String activeNnName = null;

  @JsonProperty("standbyNnName")
  private String standbyNnName = null;

  @JsonProperty("standbyNnHostId")
  private String standbyNnHostId = null;

  @JsonProperty("standbyNameDirList")
  @Valid
  private List<String> standbyNameDirList = null;

  @JsonProperty("nameservice")
  private String nameservice = null;

  @JsonProperty("qjName")
  private String qjName = null;

  @JsonProperty("activeFcName")
  private String activeFcName = null;

  @JsonProperty("standbyFcName")
  private String standbyFcName = null;

  @JsonProperty("zkServiceName")
  private String zkServiceName = null;

  @JsonProperty("jns")
  @Valid
  private List<ApiJournalNodeArguments> jns = null;

  @JsonProperty("forceInitZNode")
  private Boolean forceInitZNode = null;

  @JsonProperty("clearExistingStandbyNameDirs")
  private Boolean clearExistingStandbyNameDirs = null;

  @JsonProperty("clearExistingJnEditsDir")
  private Boolean clearExistingJnEditsDir = null;

  public ApiEnableNnHaArguments activeNnName(String activeNnName) {
    this.activeNnName = activeNnName;
    return this;
  }

  /**
   * Name of the NameNode role that is going to be made Highly Available.
   * @return activeNnName
  **/
  @ApiModelProperty(value = "Name of the NameNode role that is going to be made Highly Available.")


  public String getActiveNnName() {
    return activeNnName;
  }

  public void setActiveNnName(String activeNnName) {
    this.activeNnName = activeNnName;
  }

  public ApiEnableNnHaArguments standbyNnName(String standbyNnName) {
    this.standbyNnName = standbyNnName;
    return this;
  }

  /**
   * Name of the new Standby NameNode role that will be created during the command (Optional).
   * @return standbyNnName
  **/
  @ApiModelProperty(value = "Name of the new Standby NameNode role that will be created during the command (Optional).")


  public String getStandbyNnName() {
    return standbyNnName;
  }

  public void setStandbyNnName(String standbyNnName) {
    this.standbyNnName = standbyNnName;
  }

  public ApiEnableNnHaArguments standbyNnHostId(String standbyNnHostId) {
    this.standbyNnHostId = standbyNnHostId;
    return this;
  }

  /**
   * Id of the host on which new Standby NameNode will be created.
   * @return standbyNnHostId
  **/
  @ApiModelProperty(value = "Id of the host on which new Standby NameNode will be created.")


  public String getStandbyNnHostId() {
    return standbyNnHostId;
  }

  public void setStandbyNnHostId(String standbyNnHostId) {
    this.standbyNnHostId = standbyNnHostId;
  }

  public ApiEnableNnHaArguments standbyNameDirList(List<String> standbyNameDirList) {
    this.standbyNameDirList = standbyNameDirList;
    return this;
  }

  public ApiEnableNnHaArguments addStandbyNameDirListItem(String standbyNameDirListItem) {
    if (this.standbyNameDirList == null) {
      this.standbyNameDirList = new ArrayList<>();
    }
    this.standbyNameDirList.add(standbyNameDirListItem);
    return this;
  }

  /**
   * List of directories for the new Standby NameNode. If not provided then it will use same dirs as Active NameNode.
   * @return standbyNameDirList
  **/
  @ApiModelProperty(value = "List of directories for the new Standby NameNode. If not provided then it will use same dirs as Active NameNode.")


  public List<String> getStandbyNameDirList() {
    return standbyNameDirList;
  }

  public void setStandbyNameDirList(List<String> standbyNameDirList) {
    this.standbyNameDirList = standbyNameDirList;
  }

  public ApiEnableNnHaArguments nameservice(String nameservice) {
    this.nameservice = nameservice;
    return this;
  }

  /**
   * Nameservice to be used while enabling Highly Available. It must be specified if Active NameNode isn't configured with it. If Active NameNode is already configured, then this need not be specified. However, if it is still specified, it must match the existing config for the Active NameNode.
   * @return nameservice
  **/
  @ApiModelProperty(value = "Nameservice to be used while enabling Highly Available. It must be specified if Active NameNode isn't configured with it. If Active NameNode is already configured, then this need not be specified. However, if it is still specified, it must match the existing config for the Active NameNode.")


  public String getNameservice() {
    return nameservice;
  }

  public void setNameservice(String nameservice) {
    this.nameservice = nameservice;
  }

  public ApiEnableNnHaArguments qjName(String qjName) {
    this.qjName = qjName;
    return this;
  }

  /**
   * Name of the journal located on each JournalNodes' filesystem. This can be optionally provided if the config hasn't already been set for the Active NameNode. If this isn't provided and Active NameNode doesn't also have the config, then nameservice is used by default. If Active NameNode already has this configured, then it much match the existing config.
   * @return qjName
  **/
  @ApiModelProperty(value = "Name of the journal located on each JournalNodes' filesystem. This can be optionally provided if the config hasn't already been set for the Active NameNode. If this isn't provided and Active NameNode doesn't also have the config, then nameservice is used by default. If Active NameNode already has this configured, then it much match the existing config.")


  public String getQjName() {
    return qjName;
  }

  public void setQjName(String qjName) {
    this.qjName = qjName;
  }

  public ApiEnableNnHaArguments activeFcName(String activeFcName) {
    this.activeFcName = activeFcName;
    return this;
  }

  /**
   * Name of the FailoverController role to be created on Active NameNode's host (Optional).
   * @return activeFcName
  **/
  @ApiModelProperty(value = "Name of the FailoverController role to be created on Active NameNode's host (Optional).")


  public String getActiveFcName() {
    return activeFcName;
  }

  public void setActiveFcName(String activeFcName) {
    this.activeFcName = activeFcName;
  }

  public ApiEnableNnHaArguments standbyFcName(String standbyFcName) {
    this.standbyFcName = standbyFcName;
    return this;
  }

  /**
   * Name of the FailoverController role to be created on Standby NameNode's host (Optional).
   * @return standbyFcName
  **/
  @ApiModelProperty(value = "Name of the FailoverController role to be created on Standby NameNode's host (Optional).")


  public String getStandbyFcName() {
    return standbyFcName;
  }

  public void setStandbyFcName(String standbyFcName) {
    this.standbyFcName = standbyFcName;
  }

  public ApiEnableNnHaArguments zkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
    return this;
  }

  /**
   * Name of the ZooKeeper service to be used for Auto-Failover. This MUST be provided if HDFS doesn't have a ZooKeeper dependency. If the dependency is already set, then this should be the name of the same ZooKeeper service, but can also be omitted in that case.
   * @return zkServiceName
  **/
  @ApiModelProperty(value = "Name of the ZooKeeper service to be used for Auto-Failover. This MUST be provided if HDFS doesn't have a ZooKeeper dependency. If the dependency is already set, then this should be the name of the same ZooKeeper service, but can also be omitted in that case.")


  public String getZkServiceName() {
    return zkServiceName;
  }

  public void setZkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
  }

  public ApiEnableNnHaArguments jns(List<ApiJournalNodeArguments> jns) {
    this.jns = jns;
    return this;
  }

  public ApiEnableNnHaArguments addJnsItem(ApiJournalNodeArguments jnsItem) {
    if (this.jns == null) {
      this.jns = new ArrayList<>();
    }
    this.jns.add(jnsItem);
    return this;
  }

  /**
   * Arguments for the JournalNodes to be created during the command. Must be provided only if JournalNodes don't exist already in HDFS.
   * @return jns
  **/
  @ApiModelProperty(value = "Arguments for the JournalNodes to be created during the command. Must be provided only if JournalNodes don't exist already in HDFS.")

  @Valid

  public List<ApiJournalNodeArguments> getJns() {
    return jns;
  }

  public void setJns(List<ApiJournalNodeArguments> jns) {
    this.jns = jns;
  }

  public ApiEnableNnHaArguments forceInitZNode(Boolean forceInitZNode) {
    this.forceInitZNode = forceInitZNode;
    return this;
  }

  /**
   * Boolean indicating if the ZNode should be force initialized if it is already present. Useful while re-enabling High Availability. (Default: TRUE)
   * @return forceInitZNode
  **/
  @ApiModelProperty(value = "Boolean indicating if the ZNode should be force initialized if it is already present. Useful while re-enabling High Availability. (Default: TRUE)")


  public Boolean isForceInitZNode() {
    return forceInitZNode;
  }

  public void setForceInitZNode(Boolean forceInitZNode) {
    this.forceInitZNode = forceInitZNode;
  }

  public ApiEnableNnHaArguments clearExistingStandbyNameDirs(Boolean clearExistingStandbyNameDirs) {
    this.clearExistingStandbyNameDirs = clearExistingStandbyNameDirs;
    return this;
  }

  /**
   * Boolean indicating if the existing name directories for Standby NameNode should be cleared during the workflow. Useful while re-enabling High Availability. (Default: TRUE)
   * @return clearExistingStandbyNameDirs
  **/
  @ApiModelProperty(value = "Boolean indicating if the existing name directories for Standby NameNode should be cleared during the workflow. Useful while re-enabling High Availability. (Default: TRUE)")


  public Boolean isClearExistingStandbyNameDirs() {
    return clearExistingStandbyNameDirs;
  }

  public void setClearExistingStandbyNameDirs(Boolean clearExistingStandbyNameDirs) {
    this.clearExistingStandbyNameDirs = clearExistingStandbyNameDirs;
  }

  public ApiEnableNnHaArguments clearExistingJnEditsDir(Boolean clearExistingJnEditsDir) {
    this.clearExistingJnEditsDir = clearExistingJnEditsDir;
    return this;
  }

  /**
   * Boolean indicating if the existing edits directories for the JournalNodes for the specified nameservice should be cleared during the workflow. Useful while re-enabling High Availability. (Default: TRUE)
   * @return clearExistingJnEditsDir
  **/
  @ApiModelProperty(value = "Boolean indicating if the existing edits directories for the JournalNodes for the specified nameservice should be cleared during the workflow. Useful while re-enabling High Availability. (Default: TRUE)")


  public Boolean isClearExistingJnEditsDir() {
    return clearExistingJnEditsDir;
  }

  public void setClearExistingJnEditsDir(Boolean clearExistingJnEditsDir) {
    this.clearExistingJnEditsDir = clearExistingJnEditsDir;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEnableNnHaArguments apiEnableNnHaArguments = (ApiEnableNnHaArguments) o;
    return Objects.equals(this.activeNnName, apiEnableNnHaArguments.activeNnName) &&
        Objects.equals(this.standbyNnName, apiEnableNnHaArguments.standbyNnName) &&
        Objects.equals(this.standbyNnHostId, apiEnableNnHaArguments.standbyNnHostId) &&
        Objects.equals(this.standbyNameDirList, apiEnableNnHaArguments.standbyNameDirList) &&
        Objects.equals(this.nameservice, apiEnableNnHaArguments.nameservice) &&
        Objects.equals(this.qjName, apiEnableNnHaArguments.qjName) &&
        Objects.equals(this.activeFcName, apiEnableNnHaArguments.activeFcName) &&
        Objects.equals(this.standbyFcName, apiEnableNnHaArguments.standbyFcName) &&
        Objects.equals(this.zkServiceName, apiEnableNnHaArguments.zkServiceName) &&
        Objects.equals(this.jns, apiEnableNnHaArguments.jns) &&
        Objects.equals(this.forceInitZNode, apiEnableNnHaArguments.forceInitZNode) &&
        Objects.equals(this.clearExistingStandbyNameDirs, apiEnableNnHaArguments.clearExistingStandbyNameDirs) &&
        Objects.equals(this.clearExistingJnEditsDir, apiEnableNnHaArguments.clearExistingJnEditsDir);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activeNnName, standbyNnName, standbyNnHostId, standbyNameDirList, nameservice, qjName, activeFcName, standbyFcName, zkServiceName, jns, forceInitZNode, clearExistingStandbyNameDirs, clearExistingJnEditsDir);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEnableNnHaArguments {\n");
    
    sb.append("    activeNnName: ").append(toIndentedString(activeNnName)).append("\n");
    sb.append("    standbyNnName: ").append(toIndentedString(standbyNnName)).append("\n");
    sb.append("    standbyNnHostId: ").append(toIndentedString(standbyNnHostId)).append("\n");
    sb.append("    standbyNameDirList: ").append(toIndentedString(standbyNameDirList)).append("\n");
    sb.append("    nameservice: ").append(toIndentedString(nameservice)).append("\n");
    sb.append("    qjName: ").append(toIndentedString(qjName)).append("\n");
    sb.append("    activeFcName: ").append(toIndentedString(activeFcName)).append("\n");
    sb.append("    standbyFcName: ").append(toIndentedString(standbyFcName)).append("\n");
    sb.append("    zkServiceName: ").append(toIndentedString(zkServiceName)).append("\n");
    sb.append("    jns: ").append(toIndentedString(jns)).append("\n");
    sb.append("    forceInitZNode: ").append(toIndentedString(forceInitZNode)).append("\n");
    sb.append("    clearExistingStandbyNameDirs: ").append(toIndentedString(clearExistingStandbyNameDirs)).append("\n");
    sb.append("    clearExistingJnEditsDir: ").append(toIndentedString(clearExistingJnEditsDir)).append("\n");
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

