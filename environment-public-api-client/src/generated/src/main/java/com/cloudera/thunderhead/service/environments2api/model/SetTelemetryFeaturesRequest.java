/*
 * Cloudera Environments Service
 * Cloudera Environments Service is a web service that manages cloud provider access.
 *
 * The version of the OpenAPI document: __API_VERSION__
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.cloudera.thunderhead.service.environments2api.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Request object to enable environment level telemetry features.
 */
@JsonPropertyOrder({
  SetTelemetryFeaturesRequest.JSON_PROPERTY_ENVIRONMENT_NAME,
  SetTelemetryFeaturesRequest.JSON_PROPERTY_WORKLOAD_ANALYTICS,
  SetTelemetryFeaturesRequest.JSON_PROPERTY_REPORT_DEPLOYMENT_LOGS,
  SetTelemetryFeaturesRequest.JSON_PROPERTY_CLOUD_STORAGE_LOGGING
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class SetTelemetryFeaturesRequest {
  public static final String JSON_PROPERTY_ENVIRONMENT_NAME = "environmentName";
  private String environmentName;

  public static final String JSON_PROPERTY_WORKLOAD_ANALYTICS = "workloadAnalytics";
  private Boolean workloadAnalytics;

  public static final String JSON_PROPERTY_REPORT_DEPLOYMENT_LOGS = "reportDeploymentLogs";
  private Boolean reportDeploymentLogs;

  public static final String JSON_PROPERTY_CLOUD_STORAGE_LOGGING = "cloudStorageLogging";
  private Boolean cloudStorageLogging;

  public SetTelemetryFeaturesRequest() { 
  }

  public SetTelemetryFeaturesRequest environmentName(String environmentName) {
    this.environmentName = environmentName;
    return this;
  }

   /**
   * The name or CRN of the environment.
   * @return environmentName
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ENVIRONMENT_NAME)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getEnvironmentName() {
    return environmentName;
  }


  @JsonProperty(JSON_PROPERTY_ENVIRONMENT_NAME)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setEnvironmentName(String environmentName) {
    this.environmentName = environmentName;
  }


  public SetTelemetryFeaturesRequest workloadAnalytics(Boolean workloadAnalytics) {
    this.workloadAnalytics = workloadAnalytics;
    return this;
  }

   /**
   * Flag to enable environment level workload analytics.
   * @return workloadAnalytics
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_WORKLOAD_ANALYTICS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getWorkloadAnalytics() {
    return workloadAnalytics;
  }


  @JsonProperty(JSON_PROPERTY_WORKLOAD_ANALYTICS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setWorkloadAnalytics(Boolean workloadAnalytics) {
    this.workloadAnalytics = workloadAnalytics;
  }


  public SetTelemetryFeaturesRequest reportDeploymentLogs(Boolean reportDeploymentLogs) {
    this.reportDeploymentLogs = reportDeploymentLogs;
    return this;
  }

   /**
   * Flag to enable environment level deployment log collection.
   * @return reportDeploymentLogs
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_REPORT_DEPLOYMENT_LOGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getReportDeploymentLogs() {
    return reportDeploymentLogs;
  }


  @JsonProperty(JSON_PROPERTY_REPORT_DEPLOYMENT_LOGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setReportDeploymentLogs(Boolean reportDeploymentLogs) {
    this.reportDeploymentLogs = reportDeploymentLogs;
  }


  public SetTelemetryFeaturesRequest cloudStorageLogging(Boolean cloudStorageLogging) {
    this.cloudStorageLogging = cloudStorageLogging;
    return this;
  }

   /**
   * Flag to enable environment level cloud storage logging (enabled by default).
   * @return cloudStorageLogging
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CLOUD_STORAGE_LOGGING)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getCloudStorageLogging() {
    return cloudStorageLogging;
  }


  @JsonProperty(JSON_PROPERTY_CLOUD_STORAGE_LOGGING)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCloudStorageLogging(Boolean cloudStorageLogging) {
    this.cloudStorageLogging = cloudStorageLogging;
  }


  /**
   * Return true if this SetTelemetryFeaturesRequest object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetTelemetryFeaturesRequest setTelemetryFeaturesRequest = (SetTelemetryFeaturesRequest) o;
    return Objects.equals(this.environmentName, setTelemetryFeaturesRequest.environmentName) &&
        Objects.equals(this.workloadAnalytics, setTelemetryFeaturesRequest.workloadAnalytics) &&
        Objects.equals(this.reportDeploymentLogs, setTelemetryFeaturesRequest.reportDeploymentLogs) &&
        Objects.equals(this.cloudStorageLogging, setTelemetryFeaturesRequest.cloudStorageLogging);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environmentName, workloadAnalytics, reportDeploymentLogs, cloudStorageLogging);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetTelemetryFeaturesRequest {\n");
    sb.append("    environmentName: ").append(toIndentedString(environmentName)).append("\n");
    sb.append("    workloadAnalytics: ").append(toIndentedString(workloadAnalytics)).append("\n");
    sb.append("    reportDeploymentLogs: ").append(toIndentedString(reportDeploymentLogs)).append("\n");
    sb.append("    cloudStorageLogging: ").append(toIndentedString(cloudStorageLogging)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    // add `environmentName` to the URL query string
    if (getEnvironmentName() != null) {
      joiner.add(String.format("%senvironmentName%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getEnvironmentName()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `workloadAnalytics` to the URL query string
    if (getWorkloadAnalytics() != null) {
      joiner.add(String.format("%sworkloadAnalytics%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getWorkloadAnalytics()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `reportDeploymentLogs` to the URL query string
    if (getReportDeploymentLogs() != null) {
      joiner.add(String.format("%sreportDeploymentLogs%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getReportDeploymentLogs()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `cloudStorageLogging` to the URL query string
    if (getCloudStorageLogging() != null) {
      joiner.add(String.format("%scloudStorageLogging%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getCloudStorageLogging()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}

