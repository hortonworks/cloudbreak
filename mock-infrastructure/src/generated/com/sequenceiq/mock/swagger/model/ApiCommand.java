package com.sequenceiq.mock.swagger.model;

import java.util.Objects;

import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Provides detailed information about a submitted command.  &lt;p&gt;There are two types of commands: synchronous and asynchronous. Synchronous commands complete immediately, and their results are passed back in the returned command object after the execution of an API call. Outside of that returned object, there is no way to check the result of a synchronous command.&lt;/p&gt;  &lt;p&gt;Asynchronous commands have unique non-negative IDs. They may still be running when the API call returns. Clients can check the status of such commands using the API.&lt;/p&gt;
 */
@ApiModel(description = "Provides detailed information about a submitted command.  <p>There are two types of commands: synchronous and asynchronous. Synchronous commands complete immediately, and their results are passed back in the returned command object after the execution of an API call. Outside of that returned object, there is no way to check the result of a synchronous command.</p>  <p>Asynchronous commands have unique non-negative IDs. They may still be running when the API call returns. Clients can check the status of such commands using the API.</p>")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiCommand   {
  @JsonProperty("id")
  private Integer id = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("startTime")
  private String startTime = null;

  @JsonProperty("endTime")
  private String endTime = null;

  @JsonProperty("active")
  private Boolean active = null;

  @JsonProperty("success")
  private Boolean success = null;

  @JsonProperty("resultMessage")
  private String resultMessage = null;

  @JsonProperty("resultDataUrl")
  private String resultDataUrl = null;

  @JsonProperty("clusterRef")
  private ApiClusterRef clusterRef = null;

  @JsonProperty("serviceRef")
  private ApiServiceRef serviceRef = null;

  @JsonProperty("roleRef")
  private ApiRoleRef roleRef = null;

  @JsonProperty("hostRef")
  private ApiHostRef hostRef = null;

  @JsonProperty("parent")
  private ApiCommand parent = null;

  @JsonProperty("children")
  private ApiCommandList children = null;

  @JsonProperty("canRetry")
  private Boolean canRetry = null;

  public ApiCommand id(Integer id) {
    this.id = id;
    return this;
  }

  /**
   * The command ID.
   * @return id
  **/
  @ApiModelProperty(value = "The command ID.")


  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public ApiCommand name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The command name.
   * @return name
  **/
  @ApiModelProperty(value = "The command name.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiCommand startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * The start time.
   * @return startTime
  **/
  @ApiModelProperty(value = "The start time.")


  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public ApiCommand endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * The end time, if the command is finished.
   * @return endTime
  **/
  @ApiModelProperty(value = "The end time, if the command is finished.")


  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public ApiCommand active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * Whether the command is currently active.
   * @return active
  **/
  @ApiModelProperty(value = "Whether the command is currently active.")


  public Boolean isActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public ApiCommand success(Boolean success) {
    this.success = success;
    return this;
  }

  /**
   * If the command is finished, whether it was successful.
   * @return success
  **/
  @ApiModelProperty(value = "If the command is finished, whether it was successful.")


  public Boolean isSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public ApiCommand resultMessage(String resultMessage) {
    this.resultMessage = resultMessage;
    return this;
  }

  /**
   * If the command is finished, the result message.
   * @return resultMessage
  **/
  @ApiModelProperty(value = "If the command is finished, the result message.")


  public String getResultMessage() {
    return resultMessage;
  }

  public void setResultMessage(String resultMessage) {
    this.resultMessage = resultMessage;
  }

  public ApiCommand resultDataUrl(String resultDataUrl) {
    this.resultDataUrl = resultDataUrl;
    return this;
  }

  /**
   * URL to the command's downloadable result data, if any exists.
   * @return resultDataUrl
  **/
  @ApiModelProperty(value = "URL to the command's downloadable result data, if any exists.")


  public String getResultDataUrl() {
    return resultDataUrl;
  }

  public void setResultDataUrl(String resultDataUrl) {
    this.resultDataUrl = resultDataUrl;
  }

  public ApiCommand clusterRef(ApiClusterRef clusterRef) {
    this.clusterRef = clusterRef;
    return this;
  }

  /**
   * Reference to the cluster (for cluster commands only).
   * @return clusterRef
  **/
  @ApiModelProperty(value = "Reference to the cluster (for cluster commands only).")

  @Valid

  public ApiClusterRef getClusterRef() {
    return clusterRef;
  }

  public void setClusterRef(ApiClusterRef clusterRef) {
    this.clusterRef = clusterRef;
  }

  public ApiCommand serviceRef(ApiServiceRef serviceRef) {
    this.serviceRef = serviceRef;
    return this;
  }

  /**
   * Reference to the service (for service commands only).
   * @return serviceRef
  **/
  @ApiModelProperty(value = "Reference to the service (for service commands only).")

  @Valid

  public ApiServiceRef getServiceRef() {
    return serviceRef;
  }

  public void setServiceRef(ApiServiceRef serviceRef) {
    this.serviceRef = serviceRef;
  }

  public ApiCommand roleRef(ApiRoleRef roleRef) {
    this.roleRef = roleRef;
    return this;
  }

  /**
   * Reference to the role (for role commands only).
   * @return roleRef
  **/
  @ApiModelProperty(value = "Reference to the role (for role commands only).")

  @Valid

  public ApiRoleRef getRoleRef() {
    return roleRef;
  }

  public void setRoleRef(ApiRoleRef roleRef) {
    this.roleRef = roleRef;
  }

  public ApiCommand hostRef(ApiHostRef hostRef) {
    this.hostRef = hostRef;
    return this;
  }

  /**
   * Reference to the host (for host commands only).
   * @return hostRef
  **/
  @ApiModelProperty(value = "Reference to the host (for host commands only).")

  @Valid

  public ApiHostRef getHostRef() {
    return hostRef;
  }

  public void setHostRef(ApiHostRef hostRef) {
    this.hostRef = hostRef;
  }

  public ApiCommand parent(ApiCommand parent) {
    this.parent = parent;
    return this;
  }

  /**
   * Reference to the parent command, if any.
   * @return parent
  **/
  @ApiModelProperty(value = "Reference to the parent command, if any.")

  @Valid

  public ApiCommand getParent() {
    return parent;
  }

  public void setParent(ApiCommand parent) {
    this.parent = parent;
  }

  public ApiCommand children(ApiCommandList children) {
    this.children = children;
    return this;
  }

  /**
   * List of child commands. Only available in the full view. <p> The list contains only the summary view of the children.
   * @return children
  **/
  @ApiModelProperty(value = "List of child commands. Only available in the full view. <p> The list contains only the summary view of the children.")

  @Valid

  public ApiCommandList getChildren() {
    return children;
  }

  public void setChildren(ApiCommandList children) {
    this.children = children;
  }

  public ApiCommand canRetry(Boolean canRetry) {
    this.canRetry = canRetry;
    return this;
  }

  /**
   * If the command can be retried. Available since V11
   * @return canRetry
  **/
  @ApiModelProperty(value = "If the command can be retried. Available since V11")


  public Boolean isCanRetry() {
    return canRetry;
  }

  public void setCanRetry(Boolean canRetry) {
    this.canRetry = canRetry;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCommand apiCommand = (ApiCommand) o;
    return Objects.equals(this.id, apiCommand.id) &&
        Objects.equals(this.name, apiCommand.name) &&
        Objects.equals(this.startTime, apiCommand.startTime) &&
        Objects.equals(this.endTime, apiCommand.endTime) &&
        Objects.equals(this.active, apiCommand.active) &&
        Objects.equals(this.success, apiCommand.success) &&
        Objects.equals(this.resultMessage, apiCommand.resultMessage) &&
        Objects.equals(this.resultDataUrl, apiCommand.resultDataUrl) &&
        Objects.equals(this.clusterRef, apiCommand.clusterRef) &&
        Objects.equals(this.serviceRef, apiCommand.serviceRef) &&
        Objects.equals(this.roleRef, apiCommand.roleRef) &&
        Objects.equals(this.hostRef, apiCommand.hostRef) &&
        Objects.equals(this.parent, apiCommand.parent) &&
        Objects.equals(this.children, apiCommand.children) &&
        Objects.equals(this.canRetry, apiCommand.canRetry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, startTime, endTime, active, success, resultMessage, resultDataUrl, clusterRef, serviceRef, roleRef, hostRef, parent, children, canRetry);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCommand {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    success: ").append(toIndentedString(success)).append("\n");
    sb.append("    resultMessage: ").append(toIndentedString(resultMessage)).append("\n");
    sb.append("    resultDataUrl: ").append(toIndentedString(resultDataUrl)).append("\n");
    sb.append("    clusterRef: ").append(toIndentedString(clusterRef)).append("\n");
    sb.append("    serviceRef: ").append(toIndentedString(serviceRef)).append("\n");
    sb.append("    roleRef: ").append(toIndentedString(roleRef)).append("\n");
    sb.append("    hostRef: ").append(toIndentedString(hostRef)).append("\n");
    sb.append("    parent: ").append(toIndentedString(parent)).append("\n");
    sb.append("    children: ").append(toIndentedString(children)).append("\n");
    sb.append("    canRetry: ").append(toIndentedString(canRetry)).append("\n");
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

