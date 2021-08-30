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
 * Arguments used for HDFS HA commands.
 */
@ApiModel(description = "Arguments used for HDFS HA commands.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHdfsHaArguments   {
  @JsonProperty("activeName")
  private String activeName = null;

  @JsonProperty("activeSharedEditsPath")
  private String activeSharedEditsPath = null;

  @JsonProperty("standByName")
  private String standByName = null;

  @JsonProperty("standBySharedEditsPath")
  private String standBySharedEditsPath = null;

  @JsonProperty("nameservice")
  private String nameservice = null;

  @JsonProperty("startDependentServices")
  private Boolean startDependentServices = null;

  @JsonProperty("deployClientConfigs")
  private Boolean deployClientConfigs = null;

  @JsonProperty("enableQuorumStorage")
  private Boolean enableQuorumStorage = null;

  public ApiHdfsHaArguments activeName(String activeName) {
    this.activeName = activeName;
    return this;
  }

  /**
   * Name of the active NameNode.
   * @return activeName
  **/
  @ApiModelProperty(value = "Name of the active NameNode.")


  public String getActiveName() {
    return activeName;
  }

  public void setActiveName(String activeName) {
    this.activeName = activeName;
  }

  public ApiHdfsHaArguments activeSharedEditsPath(String activeSharedEditsPath) {
    this.activeSharedEditsPath = activeSharedEditsPath;
    return this;
  }

  /**
   * Path to the shared edits directory on the active NameNode's host. Ignored if Quorum-based Storage is being enabled.
   * @return activeSharedEditsPath
  **/
  @ApiModelProperty(value = "Path to the shared edits directory on the active NameNode's host. Ignored if Quorum-based Storage is being enabled.")


  public String getActiveSharedEditsPath() {
    return activeSharedEditsPath;
  }

  public void setActiveSharedEditsPath(String activeSharedEditsPath) {
    this.activeSharedEditsPath = activeSharedEditsPath;
  }

  public ApiHdfsHaArguments standByName(String standByName) {
    this.standByName = standByName;
    return this;
  }

  /**
   * Name of the stand-by Namenode.
   * @return standByName
  **/
  @ApiModelProperty(value = "Name of the stand-by Namenode.")


  public String getStandByName() {
    return standByName;
  }

  public void setStandByName(String standByName) {
    this.standByName = standByName;
  }

  public ApiHdfsHaArguments standBySharedEditsPath(String standBySharedEditsPath) {
    this.standBySharedEditsPath = standBySharedEditsPath;
    return this;
  }

  /**
   * Path to the shared edits directory on the stand-by NameNode's host. Ignored if Quorum-based Storage is being enabled.
   * @return standBySharedEditsPath
  **/
  @ApiModelProperty(value = "Path to the shared edits directory on the stand-by NameNode's host. Ignored if Quorum-based Storage is being enabled.")


  public String getStandBySharedEditsPath() {
    return standBySharedEditsPath;
  }

  public void setStandBySharedEditsPath(String standBySharedEditsPath) {
    this.standBySharedEditsPath = standBySharedEditsPath;
  }

  public ApiHdfsHaArguments nameservice(String nameservice) {
    this.nameservice = nameservice;
    return this;
  }

  /**
   * Nameservice that identifies the HA pair.
   * @return nameservice
  **/
  @ApiModelProperty(value = "Nameservice that identifies the HA pair.")


  public String getNameservice() {
    return nameservice;
  }

  public void setNameservice(String nameservice) {
    this.nameservice = nameservice;
  }

  public ApiHdfsHaArguments startDependentServices(Boolean startDependentServices) {
    this.startDependentServices = startDependentServices;
    return this;
  }

  /**
   * Whether to re-start dependent services. Defaults to true.
   * @return startDependentServices
  **/
  @ApiModelProperty(value = "Whether to re-start dependent services. Defaults to true.")


  public Boolean isStartDependentServices() {
    return startDependentServices;
  }

  public void setStartDependentServices(Boolean startDependentServices) {
    this.startDependentServices = startDependentServices;
  }

  public ApiHdfsHaArguments deployClientConfigs(Boolean deployClientConfigs) {
    this.deployClientConfigs = deployClientConfigs;
    return this;
  }

  /**
   * Whether to re-deploy client configurations. Defaults to true.
   * @return deployClientConfigs
  **/
  @ApiModelProperty(value = "Whether to re-deploy client configurations. Defaults to true.")


  public Boolean isDeployClientConfigs() {
    return deployClientConfigs;
  }

  public void setDeployClientConfigs(Boolean deployClientConfigs) {
    this.deployClientConfigs = deployClientConfigs;
  }

  public ApiHdfsHaArguments enableQuorumStorage(Boolean enableQuorumStorage) {
    this.enableQuorumStorage = enableQuorumStorage;
    return this;
  }

  /**
   * This parameter has been deprecated as of CM 5.0, where HA is only supported using Quorum-based Storage. <p> Whether to enable Quorum-based Storage.  Enabling Quorum-based Storage requires a minimum of three and an odd number of JournalNodes to be created and configured before enabling HDFS HA. <p> Available since API v2.
   * @return enableQuorumStorage
  **/
  @ApiModelProperty(value = "This parameter has been deprecated as of CM 5.0, where HA is only supported using Quorum-based Storage. <p> Whether to enable Quorum-based Storage.  Enabling Quorum-based Storage requires a minimum of three and an odd number of JournalNodes to be created and configured before enabling HDFS HA. <p> Available since API v2.")


  public Boolean isEnableQuorumStorage() {
    return enableQuorumStorage;
  }

  public void setEnableQuorumStorage(Boolean enableQuorumStorage) {
    this.enableQuorumStorage = enableQuorumStorage;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHdfsHaArguments apiHdfsHaArguments = (ApiHdfsHaArguments) o;
    return Objects.equals(this.activeName, apiHdfsHaArguments.activeName) &&
        Objects.equals(this.activeSharedEditsPath, apiHdfsHaArguments.activeSharedEditsPath) &&
        Objects.equals(this.standByName, apiHdfsHaArguments.standByName) &&
        Objects.equals(this.standBySharedEditsPath, apiHdfsHaArguments.standBySharedEditsPath) &&
        Objects.equals(this.nameservice, apiHdfsHaArguments.nameservice) &&
        Objects.equals(this.startDependentServices, apiHdfsHaArguments.startDependentServices) &&
        Objects.equals(this.deployClientConfigs, apiHdfsHaArguments.deployClientConfigs) &&
        Objects.equals(this.enableQuorumStorage, apiHdfsHaArguments.enableQuorumStorage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activeName, activeSharedEditsPath, standByName, standBySharedEditsPath, nameservice, startDependentServices, deployClientConfigs, enableQuorumStorage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsHaArguments {\n");
    
    sb.append("    activeName: ").append(toIndentedString(activeName)).append("\n");
    sb.append("    activeSharedEditsPath: ").append(toIndentedString(activeSharedEditsPath)).append("\n");
    sb.append("    standByName: ").append(toIndentedString(standByName)).append("\n");
    sb.append("    standBySharedEditsPath: ").append(toIndentedString(standBySharedEditsPath)).append("\n");
    sb.append("    nameservice: ").append(toIndentedString(nameservice)).append("\n");
    sb.append("    startDependentServices: ").append(toIndentedString(startDependentServices)).append("\n");
    sb.append("    deployClientConfigs: ").append(toIndentedString(deployClientConfigs)).append("\n");
    sb.append("    enableQuorumStorage: ").append(toIndentedString(enableQuorumStorage)).append("\n");
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

