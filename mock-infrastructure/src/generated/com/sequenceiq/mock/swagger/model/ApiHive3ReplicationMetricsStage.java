package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHive3ReplicationMetric;
import com.sequenceiq.mock.swagger.model.ApiHive3ReplicationMetricsStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * 
 */
@ApiModel(description = "")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHive3ReplicationMetricsStage   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("status")
  private ApiHive3ReplicationMetricsStatus status = null;

  @JsonProperty("startDate")
  private String startDate = null;

  @JsonProperty("endDate")
  private String endDate = null;

  @JsonProperty("metrics")
  @Valid
  private List<ApiHive3ReplicationMetric> metrics = null;

  @JsonProperty("errorLogPath")
  private String errorLogPath = null;

  public ApiHive3ReplicationMetricsStage name(String name) {
    this.name = name;
    return this;
  }

  /**
   * 
   * @return name
  **/
  @ApiModelProperty(value = "")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiHive3ReplicationMetricsStage status(ApiHive3ReplicationMetricsStatus status) {
    this.status = status;
    return this;
  }

  /**
   * 
   * @return status
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiHive3ReplicationMetricsStatus getStatus() {
    return status;
  }

  public void setStatus(ApiHive3ReplicationMetricsStatus status) {
    this.status = status;
  }

  public ApiHive3ReplicationMetricsStage startDate(String startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * 
   * @return startDate
  **/
  @ApiModelProperty(value = "")


  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public ApiHive3ReplicationMetricsStage endDate(String endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * 
   * @return endDate
  **/
  @ApiModelProperty(value = "")


  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  public ApiHive3ReplicationMetricsStage metrics(List<ApiHive3ReplicationMetric> metrics) {
    this.metrics = metrics;
    return this;
  }

  public ApiHive3ReplicationMetricsStage addMetricsItem(ApiHive3ReplicationMetric metricsItem) {
    if (this.metrics == null) {
      this.metrics = new ArrayList<>();
    }
    this.metrics.add(metricsItem);
    return this;
  }

  /**
   * 
   * @return metrics
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiHive3ReplicationMetric> getMetrics() {
    return metrics;
  }

  public void setMetrics(List<ApiHive3ReplicationMetric> metrics) {
    this.metrics = metrics;
  }

  public ApiHive3ReplicationMetricsStage errorLogPath(String errorLogPath) {
    this.errorLogPath = errorLogPath;
    return this;
  }

  /**
   * 
   * @return errorLogPath
  **/
  @ApiModelProperty(value = "")


  public String getErrorLogPath() {
    return errorLogPath;
  }

  public void setErrorLogPath(String errorLogPath) {
    this.errorLogPath = errorLogPath;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHive3ReplicationMetricsStage apiHive3ReplicationMetricsStage = (ApiHive3ReplicationMetricsStage) o;
    return Objects.equals(this.name, apiHive3ReplicationMetricsStage.name) &&
        Objects.equals(this.status, apiHive3ReplicationMetricsStage.status) &&
        Objects.equals(this.startDate, apiHive3ReplicationMetricsStage.startDate) &&
        Objects.equals(this.endDate, apiHive3ReplicationMetricsStage.endDate) &&
        Objects.equals(this.metrics, apiHive3ReplicationMetricsStage.metrics) &&
        Objects.equals(this.errorLogPath, apiHive3ReplicationMetricsStage.errorLogPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, status, startDate, endDate, metrics, errorLogPath);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHive3ReplicationMetricsStage {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    metrics: ").append(toIndentedString(metrics)).append("\n");
    sb.append("    errorLogPath: ").append(toIndentedString(errorLogPath)).append("\n");
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

