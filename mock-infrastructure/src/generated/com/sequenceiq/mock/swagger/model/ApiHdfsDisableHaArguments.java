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
 * Arguments used for the HDFS disable HA command.
 */
@ApiModel(description = "Arguments used for the HDFS disable HA command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHdfsDisableHaArguments   {
  @JsonProperty("activeName")
  private String activeName = null;

  @JsonProperty("secondaryName")
  private String secondaryName = null;

  @JsonProperty("startDependentServices")
  private Boolean startDependentServices = null;

  @JsonProperty("deployClientConfigs")
  private Boolean deployClientConfigs = null;

  @JsonProperty("disableQuorumStorage")
  private Boolean disableQuorumStorage = null;

  public ApiHdfsDisableHaArguments activeName(String activeName) {
    this.activeName = activeName;
    return this;
  }

  /**
   * Name of the the NameNode to be kept.
   * @return activeName
  **/
  @ApiModelProperty(value = "Name of the the NameNode to be kept.")


  public String getActiveName() {
    return activeName;
  }

  public void setActiveName(String activeName) {
    this.activeName = activeName;
  }

  public ApiHdfsDisableHaArguments secondaryName(String secondaryName) {
    this.secondaryName = secondaryName;
    return this;
  }

  /**
   * Name of the SecondaryNamenode to associate with the active NameNode.
   * @return secondaryName
  **/
  @ApiModelProperty(value = "Name of the SecondaryNamenode to associate with the active NameNode.")


  public String getSecondaryName() {
    return secondaryName;
  }

  public void setSecondaryName(String secondaryName) {
    this.secondaryName = secondaryName;
  }

  public ApiHdfsDisableHaArguments startDependentServices(Boolean startDependentServices) {
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

  public ApiHdfsDisableHaArguments deployClientConfigs(Boolean deployClientConfigs) {
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

  public ApiHdfsDisableHaArguments disableQuorumStorage(Boolean disableQuorumStorage) {
    this.disableQuorumStorage = disableQuorumStorage;
    return this;
  }

  /**
   * Whether to disable Quorum-based Storage. Defaults to false.  Available since API v2.
   * @return disableQuorumStorage
  **/
  @ApiModelProperty(value = "Whether to disable Quorum-based Storage. Defaults to false.  Available since API v2.")


  public Boolean isDisableQuorumStorage() {
    return disableQuorumStorage;
  }

  public void setDisableQuorumStorage(Boolean disableQuorumStorage) {
    this.disableQuorumStorage = disableQuorumStorage;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHdfsDisableHaArguments apiHdfsDisableHaArguments = (ApiHdfsDisableHaArguments) o;
    return Objects.equals(this.activeName, apiHdfsDisableHaArguments.activeName) &&
        Objects.equals(this.secondaryName, apiHdfsDisableHaArguments.secondaryName) &&
        Objects.equals(this.startDependentServices, apiHdfsDisableHaArguments.startDependentServices) &&
        Objects.equals(this.deployClientConfigs, apiHdfsDisableHaArguments.deployClientConfigs) &&
        Objects.equals(this.disableQuorumStorage, apiHdfsDisableHaArguments.disableQuorumStorage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activeName, secondaryName, startDependentServices, deployClientConfigs, disableQuorumStorage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHdfsDisableHaArguments {\n");
    
    sb.append("    activeName: ").append(toIndentedString(activeName)).append("\n");
    sb.append("    secondaryName: ").append(toIndentedString(secondaryName)).append("\n");
    sb.append("    startDependentServices: ").append(toIndentedString(startDependentServices)).append("\n");
    sb.append("    deployClientConfigs: ").append(toIndentedString(deployClientConfigs)).append("\n");
    sb.append("    disableQuorumStorage: ").append(toIndentedString(disableQuorumStorage)).append("\n");
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

