package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used for the collectDiagnosticData command.
 */
@ApiModel(description = "Arguments used for the collectDiagnosticData command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiCollectDiagnosticDataArguments   {
  @JsonProperty("bundleSizeBytes")
  private Integer bundleSizeBytes = null;

  @JsonProperty("startTime")
  private String startTime = null;

  @JsonProperty("endTime")
  private String endTime = null;

  @JsonProperty("includeInfoLog")
  private Boolean includeInfoLog = null;

  @JsonProperty("ticketNumber")
  private String ticketNumber = null;

  @JsonProperty("comments")
  private String comments = null;

  @JsonProperty("clusterName")
  private String clusterName = null;

  @JsonProperty("enableMonitorMetricsCollection")
  private Boolean enableMonitorMetricsCollection = null;

  @JsonProperty("roles")
  @Valid
  private List<String> roles = null;

  @JsonProperty("phoneHome")
  private Boolean phoneHome = null;

  public ApiCollectDiagnosticDataArguments bundleSizeBytes(Integer bundleSizeBytes) {
    this.bundleSizeBytes = bundleSizeBytes;
    return this;
  }

  /**
   * The maximum approximate bundle size of the output file. Defaults to 0.
   * @return bundleSizeBytes
  **/
  @ApiModelProperty(value = "The maximum approximate bundle size of the output file. Defaults to 0.")


  public Integer getBundleSizeBytes() {
    return bundleSizeBytes;
  }

  public void setBundleSizeBytes(Integer bundleSizeBytes) {
    this.bundleSizeBytes = bundleSizeBytes;
  }

  public ApiCollectDiagnosticDataArguments startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * This parameter is ignored between CM 4.5 and CM 5.7 versions. For versions from CM 4.5 to CM 5.7, use endTime and bundleSizeBytes instead.  For CM 5.7+ versions, startTime is an optional parameter that is with endTime and bundleSizeBytes. This was introduced to perform diagnostic data estimation and collection of global diagnostics data for a certain time range. The start time (in ISO 8601 format) of the period to collection statistics for.
   * @return startTime
  **/
  @ApiModelProperty(value = "This parameter is ignored between CM 4.5 and CM 5.7 versions. For versions from CM 4.5 to CM 5.7, use endTime and bundleSizeBytes instead.  For CM 5.7+ versions, startTime is an optional parameter that is with endTime and bundleSizeBytes. This was introduced to perform diagnostic data estimation and collection of global diagnostics data for a certain time range. The start time (in ISO 8601 format) of the period to collection statistics for.")


  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public ApiCollectDiagnosticDataArguments endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * The end time (in ISO 8601 format) of the period to collection statistics for.
   * @return endTime
  **/
  @ApiModelProperty(value = "The end time (in ISO 8601 format) of the period to collection statistics for.")


  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public ApiCollectDiagnosticDataArguments includeInfoLog(Boolean includeInfoLog) {
    this.includeInfoLog = includeInfoLog;
    return this;
  }

  /**
   * This parameter is ignored as of CM 4.5. INFO logs are always collected. Whether to include INFO level logs. WARN, ERROR, and FATAL level logs are always included.
   * @return includeInfoLog
  **/
  @ApiModelProperty(value = "This parameter is ignored as of CM 4.5. INFO logs are always collected. Whether to include INFO level logs. WARN, ERROR, and FATAL level logs are always included.")


  public Boolean isIncludeInfoLog() {
    return includeInfoLog;
  }

  public void setIncludeInfoLog(Boolean includeInfoLog) {
    this.includeInfoLog = includeInfoLog;
  }

  public ApiCollectDiagnosticDataArguments ticketNumber(String ticketNumber) {
    this.ticketNumber = ticketNumber;
    return this;
  }

  /**
   * The support ticket number to attach to this data collection.
   * @return ticketNumber
  **/
  @ApiModelProperty(value = "The support ticket number to attach to this data collection.")


  public String getTicketNumber() {
    return ticketNumber;
  }

  public void setTicketNumber(String ticketNumber) {
    this.ticketNumber = ticketNumber;
  }

  public ApiCollectDiagnosticDataArguments comments(String comments) {
    this.comments = comments;
    return this;
  }

  /**
   * Comments to include with this data collection.
   * @return comments
  **/
  @ApiModelProperty(value = "Comments to include with this data collection.")


  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public ApiCollectDiagnosticDataArguments clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  /**
   * Name of the cluster to collect. If null, collects from all clusters.
   * @return clusterName
  **/
  @ApiModelProperty(value = "Name of the cluster to collect. If null, collects from all clusters.")


  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public ApiCollectDiagnosticDataArguments enableMonitorMetricsCollection(Boolean enableMonitorMetricsCollection) {
    this.enableMonitorMetricsCollection = enableMonitorMetricsCollection;
    return this;
  }

  /**
   * Flag to enable collection of metrics for chart display.
   * @return enableMonitorMetricsCollection
  **/
  @ApiModelProperty(value = "Flag to enable collection of metrics for chart display.")


  public Boolean isEnableMonitorMetricsCollection() {
    return enableMonitorMetricsCollection;
  }

  public void setEnableMonitorMetricsCollection(Boolean enableMonitorMetricsCollection) {
    this.enableMonitorMetricsCollection = enableMonitorMetricsCollection;
  }

  public ApiCollectDiagnosticDataArguments roles(List<String> roles) {
    this.roles = roles;
    return this;
  }

  public ApiCollectDiagnosticDataArguments addRolesItem(String rolesItem) {
    if (this.roles == null) {
      this.roles = new ArrayList<>();
    }
    this.roles.add(rolesItem);
    return this;
  }

  /**
   * List of roles for which to get logs and metrics.  If set, this restricts the roles for log and metrics collection to the list specified.  If empty, the default is to get logs for all roles (in the selected cluster, if one is selected).  Introduced in API v10 of the API.
   * @return roles
  **/
  @ApiModelProperty(value = "List of roles for which to get logs and metrics.  If set, this restricts the roles for log and metrics collection to the list specified.  If empty, the default is to get logs for all roles (in the selected cluster, if one is selected).  Introduced in API v10 of the API.")


  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public ApiCollectDiagnosticDataArguments phoneHome(Boolean phoneHome) {
    this.phoneHome = phoneHome;
    return this;
  }

  /**
   * Flag to enable or disable diagnostic bundle upload to EDH.  If not set, the PHONE_HOME setting decides whether the bundle will get uploaded or not.
   * @return phoneHome
  **/
  @ApiModelProperty(value = "Flag to enable or disable diagnostic bundle upload to EDH.  If not set, the PHONE_HOME setting decides whether the bundle will get uploaded or not.")


  public Boolean isPhoneHome() {
    return phoneHome;
  }

  public void setPhoneHome(Boolean phoneHome) {
    this.phoneHome = phoneHome;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCollectDiagnosticDataArguments apiCollectDiagnosticDataArguments = (ApiCollectDiagnosticDataArguments) o;
    return Objects.equals(this.bundleSizeBytes, apiCollectDiagnosticDataArguments.bundleSizeBytes) &&
        Objects.equals(this.startTime, apiCollectDiagnosticDataArguments.startTime) &&
        Objects.equals(this.endTime, apiCollectDiagnosticDataArguments.endTime) &&
        Objects.equals(this.includeInfoLog, apiCollectDiagnosticDataArguments.includeInfoLog) &&
        Objects.equals(this.ticketNumber, apiCollectDiagnosticDataArguments.ticketNumber) &&
        Objects.equals(this.comments, apiCollectDiagnosticDataArguments.comments) &&
        Objects.equals(this.clusterName, apiCollectDiagnosticDataArguments.clusterName) &&
        Objects.equals(this.enableMonitorMetricsCollection, apiCollectDiagnosticDataArguments.enableMonitorMetricsCollection) &&
        Objects.equals(this.roles, apiCollectDiagnosticDataArguments.roles) &&
        Objects.equals(this.phoneHome, apiCollectDiagnosticDataArguments.phoneHome);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bundleSizeBytes, startTime, endTime, includeInfoLog, ticketNumber, comments, clusterName, enableMonitorMetricsCollection, roles, phoneHome);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectDiagnosticDataArguments {\n");
    
    sb.append("    bundleSizeBytes: ").append(toIndentedString(bundleSizeBytes)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    includeInfoLog: ").append(toIndentedString(includeInfoLog)).append("\n");
    sb.append("    ticketNumber: ").append(toIndentedString(ticketNumber)).append("\n");
    sb.append("    comments: ").append(toIndentedString(comments)).append("\n");
    sb.append("    clusterName: ").append(toIndentedString(clusterName)).append("\n");
    sb.append("    enableMonitorMetricsCollection: ").append(toIndentedString(enableMonitorMetricsCollection)).append("\n");
    sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
    sb.append("    phoneHome: ").append(toIndentedString(phoneHome)).append("\n");
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

