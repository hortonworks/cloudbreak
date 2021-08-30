package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Models audit events from both CM and CM managed services like HDFS, HBase and Hive. Audits for CM managed services are retrieved from Cloudera Navigator server.
 */
@ApiModel(description = "Models audit events from both CM and CM managed services like HDFS, HBase and Hive. Audits for CM managed services are retrieved from Cloudera Navigator server.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiAudit   {
  @JsonProperty("timestamp")
  private String timestamp = null;

  @JsonProperty("service")
  private String service = null;

  @JsonProperty("username")
  private String username = null;

  @JsonProperty("impersonator")
  private String impersonator = null;

  @JsonProperty("ipAddress")
  private String ipAddress = null;

  @JsonProperty("command")
  private String command = null;

  @JsonProperty("resource")
  private String resource = null;

  @JsonProperty("operationText")
  private String operationText = null;

  @JsonProperty("allowed")
  private Boolean allowed = null;

  @JsonProperty("serviceValues")
  @Valid
  private Map<String, String> serviceValues = null;

  public ApiAudit timestamp(String timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * When the audit event was captured.
   * @return timestamp
  **/
  @ApiModelProperty(value = "When the audit event was captured.")


  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public ApiAudit service(String service) {
    this.service = service;
    return this;
  }

  /**
   * Service name associated with this audit.
   * @return service
  **/
  @ApiModelProperty(value = "Service name associated with this audit.")


  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public ApiAudit username(String username) {
    this.username = username;
    return this;
  }

  /**
   * The user who performed this operation.
   * @return username
  **/
  @ApiModelProperty(value = "The user who performed this operation.")


  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public ApiAudit impersonator(String impersonator) {
    this.impersonator = impersonator;
    return this;
  }

  /**
   * The impersonating user (or the proxy user) who submitted this operation. This is usually applicable when using services like Oozie or Hue, who can be configured to impersonate other users and submit jobs.
   * @return impersonator
  **/
  @ApiModelProperty(value = "The impersonating user (or the proxy user) who submitted this operation. This is usually applicable when using services like Oozie or Hue, who can be configured to impersonate other users and submit jobs.")


  public String getImpersonator() {
    return impersonator;
  }

  public void setImpersonator(String impersonator) {
    this.impersonator = impersonator;
  }

  public ApiAudit ipAddress(String ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }

  /**
   * The IP address that the client connected from.
   * @return ipAddress
  **/
  @ApiModelProperty(value = "The IP address that the client connected from.")


  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public ApiAudit command(String command) {
    this.command = command;
    return this;
  }

  /**
   * The command/operation that was requested.
   * @return command
  **/
  @ApiModelProperty(value = "The command/operation that was requested.")


  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public ApiAudit resource(String resource) {
    this.resource = resource;
    return this;
  }

  /**
   * The resource that the operation was performed on.
   * @return resource
  **/
  @ApiModelProperty(value = "The resource that the operation was performed on.")


  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public ApiAudit operationText(String operationText) {
    this.operationText = operationText;
    return this;
  }

  /**
   * The full text of the requested operation. E.g. the full Hive query. <p> Available since API v5.
   * @return operationText
  **/
  @ApiModelProperty(value = "The full text of the requested operation. E.g. the full Hive query. <p> Available since API v5.")


  public String getOperationText() {
    return operationText;
  }

  public void setOperationText(String operationText) {
    this.operationText = operationText;
  }

  public ApiAudit allowed(Boolean allowed) {
    this.allowed = allowed;
    return this;
  }

  /**
   * Whether the operation was allowed or denied by the authorization system.
   * @return allowed
  **/
  @ApiModelProperty(value = "Whether the operation was allowed or denied by the authorization system.")


  public Boolean isAllowed() {
    return allowed;
  }

  public void setAllowed(Boolean allowed) {
    this.allowed = allowed;
  }

  public ApiAudit serviceValues(Map<String, String> serviceValues) {
    this.serviceValues = serviceValues;
    return this;
  }

  public ApiAudit putServiceValuesItem(String key, String serviceValuesItem) {
    if (this.serviceValues == null) {
      this.serviceValues = new HashMap<>();
    }
    this.serviceValues.put(key, serviceValuesItem);
    return this;
  }

  /**
   * 
   * @return serviceValues
  **/
  @ApiModelProperty(value = "")


  public Map<String, String> getServiceValues() {
    return serviceValues;
  }

  public void setServiceValues(Map<String, String> serviceValues) {
    this.serviceValues = serviceValues;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiAudit apiAudit = (ApiAudit) o;
    return Objects.equals(this.timestamp, apiAudit.timestamp) &&
        Objects.equals(this.service, apiAudit.service) &&
        Objects.equals(this.username, apiAudit.username) &&
        Objects.equals(this.impersonator, apiAudit.impersonator) &&
        Objects.equals(this.ipAddress, apiAudit.ipAddress) &&
        Objects.equals(this.command, apiAudit.command) &&
        Objects.equals(this.resource, apiAudit.resource) &&
        Objects.equals(this.operationText, apiAudit.operationText) &&
        Objects.equals(this.allowed, apiAudit.allowed) &&
        Objects.equals(this.serviceValues, apiAudit.serviceValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, service, username, impersonator, ipAddress, command, resource, operationText, allowed, serviceValues);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiAudit {\n");
    
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    service: ").append(toIndentedString(service)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    impersonator: ").append(toIndentedString(impersonator)).append("\n");
    sb.append("    ipAddress: ").append(toIndentedString(ipAddress)).append("\n");
    sb.append("    command: ").append(toIndentedString(command)).append("\n");
    sb.append("    resource: ").append(toIndentedString(resource)).append("\n");
    sb.append("    operationText: ").append(toIndentedString(operationText)).append("\n");
    sb.append("    allowed: ").append(toIndentedString(allowed)).append("\n");
    sb.append("    serviceValues: ").append(toIndentedString(serviceValues)).append("\n");
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

