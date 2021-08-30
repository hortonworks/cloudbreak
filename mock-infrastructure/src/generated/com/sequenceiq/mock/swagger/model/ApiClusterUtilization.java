package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiTenantUtilizationList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Utilization report information of a Cluster.
 */
@ApiModel(description = "Utilization report information of a Cluster.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterUtilization   {
  @JsonProperty("totalCpuCores")
  private BigDecimal totalCpuCores = null;

  @JsonProperty("avgCpuUtilization")
  private BigDecimal avgCpuUtilization = null;

  @JsonProperty("maxCpuUtilization")
  private BigDecimal maxCpuUtilization = null;

  @JsonProperty("avgCpuDailyPeak")
  private BigDecimal avgCpuDailyPeak = null;

  @JsonProperty("avgWorkloadCpu")
  private BigDecimal avgWorkloadCpu = null;

  @JsonProperty("maxWorkloadCpu")
  private BigDecimal maxWorkloadCpu = null;

  @JsonProperty("avgWorkloadCpuDailyPeak")
  private BigDecimal avgWorkloadCpuDailyPeak = null;

  @JsonProperty("totalMemory")
  private BigDecimal totalMemory = null;

  @JsonProperty("avgMemoryUtilization")
  private BigDecimal avgMemoryUtilization = null;

  @JsonProperty("maxMemoryUtilization")
  private BigDecimal maxMemoryUtilization = null;

  @JsonProperty("avgMemoryDailyPeak")
  private BigDecimal avgMemoryDailyPeak = null;

  @JsonProperty("avgWorkloadMemory")
  private BigDecimal avgWorkloadMemory = null;

  @JsonProperty("maxWorkloadMemory")
  private BigDecimal maxWorkloadMemory = null;

  @JsonProperty("avgWorkloadMemoryDailyPeak")
  private BigDecimal avgWorkloadMemoryDailyPeak = null;

  @JsonProperty("tenantUtilizations")
  private ApiTenantUtilizationList tenantUtilizations = null;

  @JsonProperty("maxCpuUtilizationTimestampMs")
  private Integer maxCpuUtilizationTimestampMs = null;

  @JsonProperty("maxMemoryUtilizationTimestampMs")
  private Integer maxMemoryUtilizationTimestampMs = null;

  @JsonProperty("maxWorkloadCpuTimestampMs")
  private Integer maxWorkloadCpuTimestampMs = null;

  @JsonProperty("maxWorkloadMemoryTimestampMs")
  private Integer maxWorkloadMemoryTimestampMs = null;

  @JsonProperty("errorMessage")
  private String errorMessage = null;

  public ApiClusterUtilization totalCpuCores(BigDecimal totalCpuCores) {
    this.totalCpuCores = totalCpuCores;
    return this;
  }

  /**
   * Average number of CPU cores available in the cluster during the report window.
   * @return totalCpuCores
  **/
  @ApiModelProperty(value = "Average number of CPU cores available in the cluster during the report window.")

  @Valid

  public BigDecimal getTotalCpuCores() {
    return totalCpuCores;
  }

  public void setTotalCpuCores(BigDecimal totalCpuCores) {
    this.totalCpuCores = totalCpuCores;
  }

  public ApiClusterUtilization avgCpuUtilization(BigDecimal avgCpuUtilization) {
    this.avgCpuUtilization = avgCpuUtilization;
    return this;
  }

  /**
   * Average CPU consumption for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.
   * @return avgCpuUtilization
  **/
  @ApiModelProperty(value = "Average CPU consumption for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.")

  @Valid

  public BigDecimal getAvgCpuUtilization() {
    return avgCpuUtilization;
  }

  public void setAvgCpuUtilization(BigDecimal avgCpuUtilization) {
    this.avgCpuUtilization = avgCpuUtilization;
  }

  public ApiClusterUtilization maxCpuUtilization(BigDecimal maxCpuUtilization) {
    this.maxCpuUtilization = maxCpuUtilization;
    return this;
  }

  /**
   * Maximum CPU consumption for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.
   * @return maxCpuUtilization
  **/
  @ApiModelProperty(value = "Maximum CPU consumption for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.")

  @Valid

  public BigDecimal getMaxCpuUtilization() {
    return maxCpuUtilization;
  }

  public void setMaxCpuUtilization(BigDecimal maxCpuUtilization) {
    this.maxCpuUtilization = maxCpuUtilization;
  }

  public ApiClusterUtilization avgCpuDailyPeak(BigDecimal avgCpuDailyPeak) {
    this.avgCpuDailyPeak = avgCpuDailyPeak;
    return this;
  }

  /**
   * Average daily peak CPU consumption for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.
   * @return avgCpuDailyPeak
  **/
  @ApiModelProperty(value = "Average daily peak CPU consumption for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.")

  @Valid

  public BigDecimal getAvgCpuDailyPeak() {
    return avgCpuDailyPeak;
  }

  public void setAvgCpuDailyPeak(BigDecimal avgCpuDailyPeak) {
    this.avgCpuDailyPeak = avgCpuDailyPeak;
  }

  public ApiClusterUtilization avgWorkloadCpu(BigDecimal avgWorkloadCpu) {
    this.avgWorkloadCpu = avgWorkloadCpu;
    return this;
  }

  /**
   * Average CPU consumption by workloads that ran on the cluster during the report window. This includes consumption by user workloads in YARN and Impala.
   * @return avgWorkloadCpu
  **/
  @ApiModelProperty(value = "Average CPU consumption by workloads that ran on the cluster during the report window. This includes consumption by user workloads in YARN and Impala.")

  @Valid

  public BigDecimal getAvgWorkloadCpu() {
    return avgWorkloadCpu;
  }

  public void setAvgWorkloadCpu(BigDecimal avgWorkloadCpu) {
    this.avgWorkloadCpu = avgWorkloadCpu;
  }

  public ApiClusterUtilization maxWorkloadCpu(BigDecimal maxWorkloadCpu) {
    this.maxWorkloadCpu = maxWorkloadCpu;
    return this;
  }

  /**
   * Maximum CPU consumption by workloads that ran on the cluster during the report window. This includes consumption by user workloads in YARN and Impala.
   * @return maxWorkloadCpu
  **/
  @ApiModelProperty(value = "Maximum CPU consumption by workloads that ran on the cluster during the report window. This includes consumption by user workloads in YARN and Impala.")

  @Valid

  public BigDecimal getMaxWorkloadCpu() {
    return maxWorkloadCpu;
  }

  public void setMaxWorkloadCpu(BigDecimal maxWorkloadCpu) {
    this.maxWorkloadCpu = maxWorkloadCpu;
  }

  public ApiClusterUtilization avgWorkloadCpuDailyPeak(BigDecimal avgWorkloadCpuDailyPeak) {
    this.avgWorkloadCpuDailyPeak = avgWorkloadCpuDailyPeak;
    return this;
  }

  /**
   * Average daily peak CPU consumption by workloads that ran on the cluster during the report window. This includes consumption by user workloads in YARN and Impala.
   * @return avgWorkloadCpuDailyPeak
  **/
  @ApiModelProperty(value = "Average daily peak CPU consumption by workloads that ran on the cluster during the report window. This includes consumption by user workloads in YARN and Impala.")

  @Valid

  public BigDecimal getAvgWorkloadCpuDailyPeak() {
    return avgWorkloadCpuDailyPeak;
  }

  public void setAvgWorkloadCpuDailyPeak(BigDecimal avgWorkloadCpuDailyPeak) {
    this.avgWorkloadCpuDailyPeak = avgWorkloadCpuDailyPeak;
  }

  public ApiClusterUtilization totalMemory(BigDecimal totalMemory) {
    this.totalMemory = totalMemory;
    return this;
  }

  /**
   * Average physical memory (in bytes) available in the cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.
   * @return totalMemory
  **/
  @ApiModelProperty(value = "Average physical memory (in bytes) available in the cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.")

  @Valid

  public BigDecimal getTotalMemory() {
    return totalMemory;
  }

  public void setTotalMemory(BigDecimal totalMemory) {
    this.totalMemory = totalMemory;
  }

  public ApiClusterUtilization avgMemoryUtilization(BigDecimal avgMemoryUtilization) {
    this.avgMemoryUtilization = avgMemoryUtilization;
    return this;
  }

  /**
   * Average memory consumption (as percentage of total memory) for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.
   * @return avgMemoryUtilization
  **/
  @ApiModelProperty(value = "Average memory consumption (as percentage of total memory) for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.")

  @Valid

  public BigDecimal getAvgMemoryUtilization() {
    return avgMemoryUtilization;
  }

  public void setAvgMemoryUtilization(BigDecimal avgMemoryUtilization) {
    this.avgMemoryUtilization = avgMemoryUtilization;
  }

  public ApiClusterUtilization maxMemoryUtilization(BigDecimal maxMemoryUtilization) {
    this.maxMemoryUtilization = maxMemoryUtilization;
    return this;
  }

  /**
   * Maximum memory consumption (as percentage of total memory) for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.
   * @return maxMemoryUtilization
  **/
  @ApiModelProperty(value = "Maximum memory consumption (as percentage of total memory) for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.")

  @Valid

  public BigDecimal getMaxMemoryUtilization() {
    return maxMemoryUtilization;
  }

  public void setMaxMemoryUtilization(BigDecimal maxMemoryUtilization) {
    this.maxMemoryUtilization = maxMemoryUtilization;
  }

  public ApiClusterUtilization avgMemoryDailyPeak(BigDecimal avgMemoryDailyPeak) {
    this.avgMemoryDailyPeak = avgMemoryDailyPeak;
    return this;
  }

  /**
   * Average daily peak memory consumption (as percentage of total memory) for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.
   * @return avgMemoryDailyPeak
  **/
  @ApiModelProperty(value = "Average daily peak memory consumption (as percentage of total memory) for the entire cluster during the report window. This includes consumption by user workloads in YARN and Impala, as well as consumption by all services running in the cluster.")

  @Valid

  public BigDecimal getAvgMemoryDailyPeak() {
    return avgMemoryDailyPeak;
  }

  public void setAvgMemoryDailyPeak(BigDecimal avgMemoryDailyPeak) {
    this.avgMemoryDailyPeak = avgMemoryDailyPeak;
  }

  public ApiClusterUtilization avgWorkloadMemory(BigDecimal avgWorkloadMemory) {
    this.avgWorkloadMemory = avgWorkloadMemory;
    return this;
  }

  /**
   * Average memory consumption (as percentage of total memory) by workloads that ran on the cluster during the report window. This includes consumption by user workloads in YARN and Impala.
   * @return avgWorkloadMemory
  **/
  @ApiModelProperty(value = "Average memory consumption (as percentage of total memory) by workloads that ran on the cluster during the report window. This includes consumption by user workloads in YARN and Impala.")

  @Valid

  public BigDecimal getAvgWorkloadMemory() {
    return avgWorkloadMemory;
  }

  public void setAvgWorkloadMemory(BigDecimal avgWorkloadMemory) {
    this.avgWorkloadMemory = avgWorkloadMemory;
  }

  public ApiClusterUtilization maxWorkloadMemory(BigDecimal maxWorkloadMemory) {
    this.maxWorkloadMemory = maxWorkloadMemory;
    return this;
  }

  /**
   * Maximum memory consumption (as percentage of total memory) by workloads that ran on the cluster. This includes consumption by user workloads in YARN and Impala
   * @return maxWorkloadMemory
  **/
  @ApiModelProperty(value = "Maximum memory consumption (as percentage of total memory) by workloads that ran on the cluster. This includes consumption by user workloads in YARN and Impala")

  @Valid

  public BigDecimal getMaxWorkloadMemory() {
    return maxWorkloadMemory;
  }

  public void setMaxWorkloadMemory(BigDecimal maxWorkloadMemory) {
    this.maxWorkloadMemory = maxWorkloadMemory;
  }

  public ApiClusterUtilization avgWorkloadMemoryDailyPeak(BigDecimal avgWorkloadMemoryDailyPeak) {
    this.avgWorkloadMemoryDailyPeak = avgWorkloadMemoryDailyPeak;
    return this;
  }

  /**
   * Average daily peak memory consumption (as percentage of total memory) by workloads that ran on the cluster during the report window. This includes consumption by user workloads in YARN and Impala.
   * @return avgWorkloadMemoryDailyPeak
  **/
  @ApiModelProperty(value = "Average daily peak memory consumption (as percentage of total memory) by workloads that ran on the cluster during the report window. This includes consumption by user workloads in YARN and Impala.")

  @Valid

  public BigDecimal getAvgWorkloadMemoryDailyPeak() {
    return avgWorkloadMemoryDailyPeak;
  }

  public void setAvgWorkloadMemoryDailyPeak(BigDecimal avgWorkloadMemoryDailyPeak) {
    this.avgWorkloadMemoryDailyPeak = avgWorkloadMemoryDailyPeak;
  }

  public ApiClusterUtilization tenantUtilizations(ApiTenantUtilizationList tenantUtilizations) {
    this.tenantUtilizations = tenantUtilizations;
    return this;
  }

  /**
   * A list of tenant utilization reports.
   * @return tenantUtilizations
  **/
  @ApiModelProperty(value = "A list of tenant utilization reports.")

  @Valid

  public ApiTenantUtilizationList getTenantUtilizations() {
    return tenantUtilizations;
  }

  public void setTenantUtilizations(ApiTenantUtilizationList tenantUtilizations) {
    this.tenantUtilizations = tenantUtilizations;
  }

  public ApiClusterUtilization maxCpuUtilizationTimestampMs(Integer maxCpuUtilizationTimestampMs) {
    this.maxCpuUtilizationTimestampMs = maxCpuUtilizationTimestampMs;
    return this;
  }

  /**
   * Timestamp corresponding to maximum CPU utilization for the entire cluster during the report window.
   * @return maxCpuUtilizationTimestampMs
  **/
  @ApiModelProperty(value = "Timestamp corresponding to maximum CPU utilization for the entire cluster during the report window.")


  public Integer getMaxCpuUtilizationTimestampMs() {
    return maxCpuUtilizationTimestampMs;
  }

  public void setMaxCpuUtilizationTimestampMs(Integer maxCpuUtilizationTimestampMs) {
    this.maxCpuUtilizationTimestampMs = maxCpuUtilizationTimestampMs;
  }

  public ApiClusterUtilization maxMemoryUtilizationTimestampMs(Integer maxMemoryUtilizationTimestampMs) {
    this.maxMemoryUtilizationTimestampMs = maxMemoryUtilizationTimestampMs;
    return this;
  }

  /**
   * Timestamp corresponding to maximum memory utilization for the entire cluster during the report window.
   * @return maxMemoryUtilizationTimestampMs
  **/
  @ApiModelProperty(value = "Timestamp corresponding to maximum memory utilization for the entire cluster during the report window.")


  public Integer getMaxMemoryUtilizationTimestampMs() {
    return maxMemoryUtilizationTimestampMs;
  }

  public void setMaxMemoryUtilizationTimestampMs(Integer maxMemoryUtilizationTimestampMs) {
    this.maxMemoryUtilizationTimestampMs = maxMemoryUtilizationTimestampMs;
  }

  public ApiClusterUtilization maxWorkloadCpuTimestampMs(Integer maxWorkloadCpuTimestampMs) {
    this.maxWorkloadCpuTimestampMs = maxWorkloadCpuTimestampMs;
    return this;
  }

  /**
   * Timestamp corresponds to maximum CPU consumption by workloads that ran on the cluster during the report window.
   * @return maxWorkloadCpuTimestampMs
  **/
  @ApiModelProperty(value = "Timestamp corresponds to maximum CPU consumption by workloads that ran on the cluster during the report window.")


  public Integer getMaxWorkloadCpuTimestampMs() {
    return maxWorkloadCpuTimestampMs;
  }

  public void setMaxWorkloadCpuTimestampMs(Integer maxWorkloadCpuTimestampMs) {
    this.maxWorkloadCpuTimestampMs = maxWorkloadCpuTimestampMs;
  }

  public ApiClusterUtilization maxWorkloadMemoryTimestampMs(Integer maxWorkloadMemoryTimestampMs) {
    this.maxWorkloadMemoryTimestampMs = maxWorkloadMemoryTimestampMs;
    return this;
  }

  /**
   * Timestamp corresponds to maximum memory resource consumption by workloads that ran on the cluster during the report window.
   * @return maxWorkloadMemoryTimestampMs
  **/
  @ApiModelProperty(value = "Timestamp corresponds to maximum memory resource consumption by workloads that ran on the cluster during the report window.")


  public Integer getMaxWorkloadMemoryTimestampMs() {
    return maxWorkloadMemoryTimestampMs;
  }

  public void setMaxWorkloadMemoryTimestampMs(Integer maxWorkloadMemoryTimestampMs) {
    this.maxWorkloadMemoryTimestampMs = maxWorkloadMemoryTimestampMs;
  }

  public ApiClusterUtilization errorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * Error message while generating utilization report.
   * @return errorMessage
  **/
  @ApiModelProperty(value = "Error message while generating utilization report.")


  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterUtilization apiClusterUtilization = (ApiClusterUtilization) o;
    return Objects.equals(this.totalCpuCores, apiClusterUtilization.totalCpuCores) &&
        Objects.equals(this.avgCpuUtilization, apiClusterUtilization.avgCpuUtilization) &&
        Objects.equals(this.maxCpuUtilization, apiClusterUtilization.maxCpuUtilization) &&
        Objects.equals(this.avgCpuDailyPeak, apiClusterUtilization.avgCpuDailyPeak) &&
        Objects.equals(this.avgWorkloadCpu, apiClusterUtilization.avgWorkloadCpu) &&
        Objects.equals(this.maxWorkloadCpu, apiClusterUtilization.maxWorkloadCpu) &&
        Objects.equals(this.avgWorkloadCpuDailyPeak, apiClusterUtilization.avgWorkloadCpuDailyPeak) &&
        Objects.equals(this.totalMemory, apiClusterUtilization.totalMemory) &&
        Objects.equals(this.avgMemoryUtilization, apiClusterUtilization.avgMemoryUtilization) &&
        Objects.equals(this.maxMemoryUtilization, apiClusterUtilization.maxMemoryUtilization) &&
        Objects.equals(this.avgMemoryDailyPeak, apiClusterUtilization.avgMemoryDailyPeak) &&
        Objects.equals(this.avgWorkloadMemory, apiClusterUtilization.avgWorkloadMemory) &&
        Objects.equals(this.maxWorkloadMemory, apiClusterUtilization.maxWorkloadMemory) &&
        Objects.equals(this.avgWorkloadMemoryDailyPeak, apiClusterUtilization.avgWorkloadMemoryDailyPeak) &&
        Objects.equals(this.tenantUtilizations, apiClusterUtilization.tenantUtilizations) &&
        Objects.equals(this.maxCpuUtilizationTimestampMs, apiClusterUtilization.maxCpuUtilizationTimestampMs) &&
        Objects.equals(this.maxMemoryUtilizationTimestampMs, apiClusterUtilization.maxMemoryUtilizationTimestampMs) &&
        Objects.equals(this.maxWorkloadCpuTimestampMs, apiClusterUtilization.maxWorkloadCpuTimestampMs) &&
        Objects.equals(this.maxWorkloadMemoryTimestampMs, apiClusterUtilization.maxWorkloadMemoryTimestampMs) &&
        Objects.equals(this.errorMessage, apiClusterUtilization.errorMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalCpuCores, avgCpuUtilization, maxCpuUtilization, avgCpuDailyPeak, avgWorkloadCpu, maxWorkloadCpu, avgWorkloadCpuDailyPeak, totalMemory, avgMemoryUtilization, maxMemoryUtilization, avgMemoryDailyPeak, avgWorkloadMemory, maxWorkloadMemory, avgWorkloadMemoryDailyPeak, tenantUtilizations, maxCpuUtilizationTimestampMs, maxMemoryUtilizationTimestampMs, maxWorkloadCpuTimestampMs, maxWorkloadMemoryTimestampMs, errorMessage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterUtilization {\n");
    
    sb.append("    totalCpuCores: ").append(toIndentedString(totalCpuCores)).append("\n");
    sb.append("    avgCpuUtilization: ").append(toIndentedString(avgCpuUtilization)).append("\n");
    sb.append("    maxCpuUtilization: ").append(toIndentedString(maxCpuUtilization)).append("\n");
    sb.append("    avgCpuDailyPeak: ").append(toIndentedString(avgCpuDailyPeak)).append("\n");
    sb.append("    avgWorkloadCpu: ").append(toIndentedString(avgWorkloadCpu)).append("\n");
    sb.append("    maxWorkloadCpu: ").append(toIndentedString(maxWorkloadCpu)).append("\n");
    sb.append("    avgWorkloadCpuDailyPeak: ").append(toIndentedString(avgWorkloadCpuDailyPeak)).append("\n");
    sb.append("    totalMemory: ").append(toIndentedString(totalMemory)).append("\n");
    sb.append("    avgMemoryUtilization: ").append(toIndentedString(avgMemoryUtilization)).append("\n");
    sb.append("    maxMemoryUtilization: ").append(toIndentedString(maxMemoryUtilization)).append("\n");
    sb.append("    avgMemoryDailyPeak: ").append(toIndentedString(avgMemoryDailyPeak)).append("\n");
    sb.append("    avgWorkloadMemory: ").append(toIndentedString(avgWorkloadMemory)).append("\n");
    sb.append("    maxWorkloadMemory: ").append(toIndentedString(maxWorkloadMemory)).append("\n");
    sb.append("    avgWorkloadMemoryDailyPeak: ").append(toIndentedString(avgWorkloadMemoryDailyPeak)).append("\n");
    sb.append("    tenantUtilizations: ").append(toIndentedString(tenantUtilizations)).append("\n");
    sb.append("    maxCpuUtilizationTimestampMs: ").append(toIndentedString(maxCpuUtilizationTimestampMs)).append("\n");
    sb.append("    maxMemoryUtilizationTimestampMs: ").append(toIndentedString(maxMemoryUtilizationTimestampMs)).append("\n");
    sb.append("    maxWorkloadCpuTimestampMs: ").append(toIndentedString(maxWorkloadCpuTimestampMs)).append("\n");
    sb.append("    maxWorkloadMemoryTimestampMs: ").append(toIndentedString(maxWorkloadMemoryTimestampMs)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
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

