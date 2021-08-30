package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiImpalaTenantUtilizationList;
import com.sequenceiq.mock.swagger.model.ApiImpalaUtilizationHistogram;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Utilization report information of a Impala application service.
 */
@ApiModel(description = "Utilization report information of a Impala application service.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiImpalaUtilization   {
  @JsonProperty("totalQueries")
  private BigDecimal totalQueries = null;

  @JsonProperty("successfulQueries")
  private BigDecimal successfulQueries = null;

  @JsonProperty("oomQueries")
  private BigDecimal oomQueries = null;

  @JsonProperty("timeOutQueries")
  private BigDecimal timeOutQueries = null;

  @JsonProperty("rejectedQueries")
  private BigDecimal rejectedQueries = null;

  @JsonProperty("successfulQueriesPercentage")
  private BigDecimal successfulQueriesPercentage = null;

  @JsonProperty("oomQueriesPercentage")
  private BigDecimal oomQueriesPercentage = null;

  @JsonProperty("timeOutQueriesPercentage")
  private BigDecimal timeOutQueriesPercentage = null;

  @JsonProperty("rejectedQueriesPercentage")
  private BigDecimal rejectedQueriesPercentage = null;

  @JsonProperty("avgWaitTimeInQueue")
  private BigDecimal avgWaitTimeInQueue = null;

  @JsonProperty("peakAllocationTimestampMS")
  private Integer peakAllocationTimestampMS = null;

  @JsonProperty("maxAllocatedMemory")
  private BigDecimal maxAllocatedMemory = null;

  @JsonProperty("maxAllocatedMemoryPercentage")
  private BigDecimal maxAllocatedMemoryPercentage = null;

  @JsonProperty("utilizedAtMaxAllocated")
  private BigDecimal utilizedAtMaxAllocated = null;

  @JsonProperty("utilizedAtMaxAllocatedPercentage")
  private BigDecimal utilizedAtMaxAllocatedPercentage = null;

  @JsonProperty("peakUsageTimestampMS")
  private Integer peakUsageTimestampMS = null;

  @JsonProperty("maxUtilizedMemory")
  private BigDecimal maxUtilizedMemory = null;

  @JsonProperty("maxUtilizedMemoryPercentage")
  private BigDecimal maxUtilizedMemoryPercentage = null;

  @JsonProperty("allocatedAtMaxUtilized")
  private BigDecimal allocatedAtMaxUtilized = null;

  @JsonProperty("allocatedAtMaxUtilizedPercentage")
  private BigDecimal allocatedAtMaxUtilizedPercentage = null;

  @JsonProperty("distributionUtilizedByImpalaDaemon")
  private ApiImpalaUtilizationHistogram distributionUtilizedByImpalaDaemon = null;

  @JsonProperty("distributionAllocatedByImpalaDaemon")
  private ApiImpalaUtilizationHistogram distributionAllocatedByImpalaDaemon = null;

  @JsonProperty("tenantUtilizations")
  private ApiImpalaTenantUtilizationList tenantUtilizations = null;

  @JsonProperty("errorMessage")
  private String errorMessage = null;

  public ApiImpalaUtilization totalQueries(BigDecimal totalQueries) {
    this.totalQueries = totalQueries;
    return this;
  }

  /**
   * Total number of queries submitted to Impala.
   * @return totalQueries
  **/
  @ApiModelProperty(value = "Total number of queries submitted to Impala.")

  @Valid

  public BigDecimal getTotalQueries() {
    return totalQueries;
  }

  public void setTotalQueries(BigDecimal totalQueries) {
    this.totalQueries = totalQueries;
  }

  public ApiImpalaUtilization successfulQueries(BigDecimal successfulQueries) {
    this.successfulQueries = successfulQueries;
    return this;
  }

  /**
   * Number of queries that finished successfully.
   * @return successfulQueries
  **/
  @ApiModelProperty(value = "Number of queries that finished successfully.")

  @Valid

  public BigDecimal getSuccessfulQueries() {
    return successfulQueries;
  }

  public void setSuccessfulQueries(BigDecimal successfulQueries) {
    this.successfulQueries = successfulQueries;
  }

  public ApiImpalaUtilization oomQueries(BigDecimal oomQueries) {
    this.oomQueries = oomQueries;
    return this;
  }

  /**
   * Number of queries that failed due to insufficient memory.
   * @return oomQueries
  **/
  @ApiModelProperty(value = "Number of queries that failed due to insufficient memory.")

  @Valid

  public BigDecimal getOomQueries() {
    return oomQueries;
  }

  public void setOomQueries(BigDecimal oomQueries) {
    this.oomQueries = oomQueries;
  }

  public ApiImpalaUtilization timeOutQueries(BigDecimal timeOutQueries) {
    this.timeOutQueries = timeOutQueries;
    return this;
  }

  /**
   * Number of queries that timed out while waiting for resources in a pool.
   * @return timeOutQueries
  **/
  @ApiModelProperty(value = "Number of queries that timed out while waiting for resources in a pool.")

  @Valid

  public BigDecimal getTimeOutQueries() {
    return timeOutQueries;
  }

  public void setTimeOutQueries(BigDecimal timeOutQueries) {
    this.timeOutQueries = timeOutQueries;
  }

  public ApiImpalaUtilization rejectedQueries(BigDecimal rejectedQueries) {
    this.rejectedQueries = rejectedQueries;
    return this;
  }

  /**
   * Number of queries that were rejected by Impala because the pool was full.
   * @return rejectedQueries
  **/
  @ApiModelProperty(value = "Number of queries that were rejected by Impala because the pool was full.")

  @Valid

  public BigDecimal getRejectedQueries() {
    return rejectedQueries;
  }

  public void setRejectedQueries(BigDecimal rejectedQueries) {
    this.rejectedQueries = rejectedQueries;
  }

  public ApiImpalaUtilization successfulQueriesPercentage(BigDecimal successfulQueriesPercentage) {
    this.successfulQueriesPercentage = successfulQueriesPercentage;
    return this;
  }

  /**
   * Percentage of queries that finished successfully.
   * @return successfulQueriesPercentage
  **/
  @ApiModelProperty(value = "Percentage of queries that finished successfully.")

  @Valid

  public BigDecimal getSuccessfulQueriesPercentage() {
    return successfulQueriesPercentage;
  }

  public void setSuccessfulQueriesPercentage(BigDecimal successfulQueriesPercentage) {
    this.successfulQueriesPercentage = successfulQueriesPercentage;
  }

  public ApiImpalaUtilization oomQueriesPercentage(BigDecimal oomQueriesPercentage) {
    this.oomQueriesPercentage = oomQueriesPercentage;
    return this;
  }

  /**
   * Percentage of queries that failed due to insufficient memory.
   * @return oomQueriesPercentage
  **/
  @ApiModelProperty(value = "Percentage of queries that failed due to insufficient memory.")

  @Valid

  public BigDecimal getOomQueriesPercentage() {
    return oomQueriesPercentage;
  }

  public void setOomQueriesPercentage(BigDecimal oomQueriesPercentage) {
    this.oomQueriesPercentage = oomQueriesPercentage;
  }

  public ApiImpalaUtilization timeOutQueriesPercentage(BigDecimal timeOutQueriesPercentage) {
    this.timeOutQueriesPercentage = timeOutQueriesPercentage;
    return this;
  }

  /**
   * Percentage of queries that timed out while waiting for resources in a pool.
   * @return timeOutQueriesPercentage
  **/
  @ApiModelProperty(value = "Percentage of queries that timed out while waiting for resources in a pool.")

  @Valid

  public BigDecimal getTimeOutQueriesPercentage() {
    return timeOutQueriesPercentage;
  }

  public void setTimeOutQueriesPercentage(BigDecimal timeOutQueriesPercentage) {
    this.timeOutQueriesPercentage = timeOutQueriesPercentage;
  }

  public ApiImpalaUtilization rejectedQueriesPercentage(BigDecimal rejectedQueriesPercentage) {
    this.rejectedQueriesPercentage = rejectedQueriesPercentage;
    return this;
  }

  /**
   * Percentage of queries that were rejected by Impala because the pool was full.
   * @return rejectedQueriesPercentage
  **/
  @ApiModelProperty(value = "Percentage of queries that were rejected by Impala because the pool was full.")

  @Valid

  public BigDecimal getRejectedQueriesPercentage() {
    return rejectedQueriesPercentage;
  }

  public void setRejectedQueriesPercentage(BigDecimal rejectedQueriesPercentage) {
    this.rejectedQueriesPercentage = rejectedQueriesPercentage;
  }

  public ApiImpalaUtilization avgWaitTimeInQueue(BigDecimal avgWaitTimeInQueue) {
    this.avgWaitTimeInQueue = avgWaitTimeInQueue;
    return this;
  }

  /**
   * Average time, in milliseconds, spent by a query in an Impala pool while waiting for resources.
   * @return avgWaitTimeInQueue
  **/
  @ApiModelProperty(value = "Average time, in milliseconds, spent by a query in an Impala pool while waiting for resources.")

  @Valid

  public BigDecimal getAvgWaitTimeInQueue() {
    return avgWaitTimeInQueue;
  }

  public void setAvgWaitTimeInQueue(BigDecimal avgWaitTimeInQueue) {
    this.avgWaitTimeInQueue = avgWaitTimeInQueue;
  }

  public ApiImpalaUtilization peakAllocationTimestampMS(Integer peakAllocationTimestampMS) {
    this.peakAllocationTimestampMS = peakAllocationTimestampMS;
    return this;
  }

  /**
   * The time when Impala reserved the maximum amount of memory for queries.
   * @return peakAllocationTimestampMS
  **/
  @ApiModelProperty(value = "The time when Impala reserved the maximum amount of memory for queries.")


  public Integer getPeakAllocationTimestampMS() {
    return peakAllocationTimestampMS;
  }

  public void setPeakAllocationTimestampMS(Integer peakAllocationTimestampMS) {
    this.peakAllocationTimestampMS = peakAllocationTimestampMS;
  }

  public ApiImpalaUtilization maxAllocatedMemory(BigDecimal maxAllocatedMemory) {
    this.maxAllocatedMemory = maxAllocatedMemory;
    return this;
  }

  /**
   * The maximum memory (in bytes) that was reserved by Impala for executing queries.
   * @return maxAllocatedMemory
  **/
  @ApiModelProperty(value = "The maximum memory (in bytes) that was reserved by Impala for executing queries.")

  @Valid

  public BigDecimal getMaxAllocatedMemory() {
    return maxAllocatedMemory;
  }

  public void setMaxAllocatedMemory(BigDecimal maxAllocatedMemory) {
    this.maxAllocatedMemory = maxAllocatedMemory;
  }

  public ApiImpalaUtilization maxAllocatedMemoryPercentage(BigDecimal maxAllocatedMemoryPercentage) {
    this.maxAllocatedMemoryPercentage = maxAllocatedMemoryPercentage;
    return this;
  }

  /**
   * The maximum percentage of memory that was reserved by Impala for executing queries.
   * @return maxAllocatedMemoryPercentage
  **/
  @ApiModelProperty(value = "The maximum percentage of memory that was reserved by Impala for executing queries.")

  @Valid

  public BigDecimal getMaxAllocatedMemoryPercentage() {
    return maxAllocatedMemoryPercentage;
  }

  public void setMaxAllocatedMemoryPercentage(BigDecimal maxAllocatedMemoryPercentage) {
    this.maxAllocatedMemoryPercentage = maxAllocatedMemoryPercentage;
  }

  public ApiImpalaUtilization utilizedAtMaxAllocated(BigDecimal utilizedAtMaxAllocated) {
    this.utilizedAtMaxAllocated = utilizedAtMaxAllocated;
    return this;
  }

  /**
   * The amount of memory (in bytes) used by Impala for running queries at the time when maximum memory was reserved.
   * @return utilizedAtMaxAllocated
  **/
  @ApiModelProperty(value = "The amount of memory (in bytes) used by Impala for running queries at the time when maximum memory was reserved.")

  @Valid

  public BigDecimal getUtilizedAtMaxAllocated() {
    return utilizedAtMaxAllocated;
  }

  public void setUtilizedAtMaxAllocated(BigDecimal utilizedAtMaxAllocated) {
    this.utilizedAtMaxAllocated = utilizedAtMaxAllocated;
  }

  public ApiImpalaUtilization utilizedAtMaxAllocatedPercentage(BigDecimal utilizedAtMaxAllocatedPercentage) {
    this.utilizedAtMaxAllocatedPercentage = utilizedAtMaxAllocatedPercentage;
    return this;
  }

  /**
   * The percentage of memory used by Impala for running queries at the time when maximum memory was reserved.
   * @return utilizedAtMaxAllocatedPercentage
  **/
  @ApiModelProperty(value = "The percentage of memory used by Impala for running queries at the time when maximum memory was reserved.")

  @Valid

  public BigDecimal getUtilizedAtMaxAllocatedPercentage() {
    return utilizedAtMaxAllocatedPercentage;
  }

  public void setUtilizedAtMaxAllocatedPercentage(BigDecimal utilizedAtMaxAllocatedPercentage) {
    this.utilizedAtMaxAllocatedPercentage = utilizedAtMaxAllocatedPercentage;
  }

  public ApiImpalaUtilization peakUsageTimestampMS(Integer peakUsageTimestampMS) {
    this.peakUsageTimestampMS = peakUsageTimestampMS;
    return this;
  }

  /**
   * The time when Impala used the maximum amount of memory for queries.
   * @return peakUsageTimestampMS
  **/
  @ApiModelProperty(value = "The time when Impala used the maximum amount of memory for queries.")


  public Integer getPeakUsageTimestampMS() {
    return peakUsageTimestampMS;
  }

  public void setPeakUsageTimestampMS(Integer peakUsageTimestampMS) {
    this.peakUsageTimestampMS = peakUsageTimestampMS;
  }

  public ApiImpalaUtilization maxUtilizedMemory(BigDecimal maxUtilizedMemory) {
    this.maxUtilizedMemory = maxUtilizedMemory;
    return this;
  }

  /**
   * The maximum memory (in bytes) that was used by Impala for executing queries.
   * @return maxUtilizedMemory
  **/
  @ApiModelProperty(value = "The maximum memory (in bytes) that was used by Impala for executing queries.")

  @Valid

  public BigDecimal getMaxUtilizedMemory() {
    return maxUtilizedMemory;
  }

  public void setMaxUtilizedMemory(BigDecimal maxUtilizedMemory) {
    this.maxUtilizedMemory = maxUtilizedMemory;
  }

  public ApiImpalaUtilization maxUtilizedMemoryPercentage(BigDecimal maxUtilizedMemoryPercentage) {
    this.maxUtilizedMemoryPercentage = maxUtilizedMemoryPercentage;
    return this;
  }

  /**
   * The maximum percentage of memory that was used by Impala for executing queries.
   * @return maxUtilizedMemoryPercentage
  **/
  @ApiModelProperty(value = "The maximum percentage of memory that was used by Impala for executing queries.")

  @Valid

  public BigDecimal getMaxUtilizedMemoryPercentage() {
    return maxUtilizedMemoryPercentage;
  }

  public void setMaxUtilizedMemoryPercentage(BigDecimal maxUtilizedMemoryPercentage) {
    this.maxUtilizedMemoryPercentage = maxUtilizedMemoryPercentage;
  }

  public ApiImpalaUtilization allocatedAtMaxUtilized(BigDecimal allocatedAtMaxUtilized) {
    this.allocatedAtMaxUtilized = allocatedAtMaxUtilized;
    return this;
  }

  /**
   * The amount of memory (in bytes) reserved by Impala at the time when it was using the maximum memory for executing queries.
   * @return allocatedAtMaxUtilized
  **/
  @ApiModelProperty(value = "The amount of memory (in bytes) reserved by Impala at the time when it was using the maximum memory for executing queries.")

  @Valid

  public BigDecimal getAllocatedAtMaxUtilized() {
    return allocatedAtMaxUtilized;
  }

  public void setAllocatedAtMaxUtilized(BigDecimal allocatedAtMaxUtilized) {
    this.allocatedAtMaxUtilized = allocatedAtMaxUtilized;
  }

  public ApiImpalaUtilization allocatedAtMaxUtilizedPercentage(BigDecimal allocatedAtMaxUtilizedPercentage) {
    this.allocatedAtMaxUtilizedPercentage = allocatedAtMaxUtilizedPercentage;
    return this;
  }

  /**
   * The percentage of memory reserved by Impala at the time when it was using the maximum memory for executing queries.
   * @return allocatedAtMaxUtilizedPercentage
  **/
  @ApiModelProperty(value = "The percentage of memory reserved by Impala at the time when it was using the maximum memory for executing queries.")

  @Valid

  public BigDecimal getAllocatedAtMaxUtilizedPercentage() {
    return allocatedAtMaxUtilizedPercentage;
  }

  public void setAllocatedAtMaxUtilizedPercentage(BigDecimal allocatedAtMaxUtilizedPercentage) {
    this.allocatedAtMaxUtilizedPercentage = allocatedAtMaxUtilizedPercentage;
  }

  public ApiImpalaUtilization distributionUtilizedByImpalaDaemon(ApiImpalaUtilizationHistogram distributionUtilizedByImpalaDaemon) {
    this.distributionUtilizedByImpalaDaemon = distributionUtilizedByImpalaDaemon;
    return this;
  }

  /**
   * Distribution of memory used per Impala daemon for executing queries at the time Impala used the maximum memory.
   * @return distributionUtilizedByImpalaDaemon
  **/
  @ApiModelProperty(value = "Distribution of memory used per Impala daemon for executing queries at the time Impala used the maximum memory.")

  @Valid

  public ApiImpalaUtilizationHistogram getDistributionUtilizedByImpalaDaemon() {
    return distributionUtilizedByImpalaDaemon;
  }

  public void setDistributionUtilizedByImpalaDaemon(ApiImpalaUtilizationHistogram distributionUtilizedByImpalaDaemon) {
    this.distributionUtilizedByImpalaDaemon = distributionUtilizedByImpalaDaemon;
  }

  public ApiImpalaUtilization distributionAllocatedByImpalaDaemon(ApiImpalaUtilizationHistogram distributionAllocatedByImpalaDaemon) {
    this.distributionAllocatedByImpalaDaemon = distributionAllocatedByImpalaDaemon;
    return this;
  }

  /**
   * Distribution of memory reserved per Impala daemon for executing queries at the time Impala used the maximum memory.
   * @return distributionAllocatedByImpalaDaemon
  **/
  @ApiModelProperty(value = "Distribution of memory reserved per Impala daemon for executing queries at the time Impala used the maximum memory.")

  @Valid

  public ApiImpalaUtilizationHistogram getDistributionAllocatedByImpalaDaemon() {
    return distributionAllocatedByImpalaDaemon;
  }

  public void setDistributionAllocatedByImpalaDaemon(ApiImpalaUtilizationHistogram distributionAllocatedByImpalaDaemon) {
    this.distributionAllocatedByImpalaDaemon = distributionAllocatedByImpalaDaemon;
  }

  public ApiImpalaUtilization tenantUtilizations(ApiImpalaTenantUtilizationList tenantUtilizations) {
    this.tenantUtilizations = tenantUtilizations;
    return this;
  }

  /**
   * A list of tenant utilization reports.
   * @return tenantUtilizations
  **/
  @ApiModelProperty(value = "A list of tenant utilization reports.")

  @Valid

  public ApiImpalaTenantUtilizationList getTenantUtilizations() {
    return tenantUtilizations;
  }

  public void setTenantUtilizations(ApiImpalaTenantUtilizationList tenantUtilizations) {
    this.tenantUtilizations = tenantUtilizations;
  }

  public ApiImpalaUtilization errorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * error message of utilization report.
   * @return errorMessage
  **/
  @ApiModelProperty(value = "error message of utilization report.")


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
    ApiImpalaUtilization apiImpalaUtilization = (ApiImpalaUtilization) o;
    return Objects.equals(this.totalQueries, apiImpalaUtilization.totalQueries) &&
        Objects.equals(this.successfulQueries, apiImpalaUtilization.successfulQueries) &&
        Objects.equals(this.oomQueries, apiImpalaUtilization.oomQueries) &&
        Objects.equals(this.timeOutQueries, apiImpalaUtilization.timeOutQueries) &&
        Objects.equals(this.rejectedQueries, apiImpalaUtilization.rejectedQueries) &&
        Objects.equals(this.successfulQueriesPercentage, apiImpalaUtilization.successfulQueriesPercentage) &&
        Objects.equals(this.oomQueriesPercentage, apiImpalaUtilization.oomQueriesPercentage) &&
        Objects.equals(this.timeOutQueriesPercentage, apiImpalaUtilization.timeOutQueriesPercentage) &&
        Objects.equals(this.rejectedQueriesPercentage, apiImpalaUtilization.rejectedQueriesPercentage) &&
        Objects.equals(this.avgWaitTimeInQueue, apiImpalaUtilization.avgWaitTimeInQueue) &&
        Objects.equals(this.peakAllocationTimestampMS, apiImpalaUtilization.peakAllocationTimestampMS) &&
        Objects.equals(this.maxAllocatedMemory, apiImpalaUtilization.maxAllocatedMemory) &&
        Objects.equals(this.maxAllocatedMemoryPercentage, apiImpalaUtilization.maxAllocatedMemoryPercentage) &&
        Objects.equals(this.utilizedAtMaxAllocated, apiImpalaUtilization.utilizedAtMaxAllocated) &&
        Objects.equals(this.utilizedAtMaxAllocatedPercentage, apiImpalaUtilization.utilizedAtMaxAllocatedPercentage) &&
        Objects.equals(this.peakUsageTimestampMS, apiImpalaUtilization.peakUsageTimestampMS) &&
        Objects.equals(this.maxUtilizedMemory, apiImpalaUtilization.maxUtilizedMemory) &&
        Objects.equals(this.maxUtilizedMemoryPercentage, apiImpalaUtilization.maxUtilizedMemoryPercentage) &&
        Objects.equals(this.allocatedAtMaxUtilized, apiImpalaUtilization.allocatedAtMaxUtilized) &&
        Objects.equals(this.allocatedAtMaxUtilizedPercentage, apiImpalaUtilization.allocatedAtMaxUtilizedPercentage) &&
        Objects.equals(this.distributionUtilizedByImpalaDaemon, apiImpalaUtilization.distributionUtilizedByImpalaDaemon) &&
        Objects.equals(this.distributionAllocatedByImpalaDaemon, apiImpalaUtilization.distributionAllocatedByImpalaDaemon) &&
        Objects.equals(this.tenantUtilizations, apiImpalaUtilization.tenantUtilizations) &&
        Objects.equals(this.errorMessage, apiImpalaUtilization.errorMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalQueries, successfulQueries, oomQueries, timeOutQueries, rejectedQueries, successfulQueriesPercentage, oomQueriesPercentage, timeOutQueriesPercentage, rejectedQueriesPercentage, avgWaitTimeInQueue, peakAllocationTimestampMS, maxAllocatedMemory, maxAllocatedMemoryPercentage, utilizedAtMaxAllocated, utilizedAtMaxAllocatedPercentage, peakUsageTimestampMS, maxUtilizedMemory, maxUtilizedMemoryPercentage, allocatedAtMaxUtilized, allocatedAtMaxUtilizedPercentage, distributionUtilizedByImpalaDaemon, distributionAllocatedByImpalaDaemon, tenantUtilizations, errorMessage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaUtilization {\n");
    
    sb.append("    totalQueries: ").append(toIndentedString(totalQueries)).append("\n");
    sb.append("    successfulQueries: ").append(toIndentedString(successfulQueries)).append("\n");
    sb.append("    oomQueries: ").append(toIndentedString(oomQueries)).append("\n");
    sb.append("    timeOutQueries: ").append(toIndentedString(timeOutQueries)).append("\n");
    sb.append("    rejectedQueries: ").append(toIndentedString(rejectedQueries)).append("\n");
    sb.append("    successfulQueriesPercentage: ").append(toIndentedString(successfulQueriesPercentage)).append("\n");
    sb.append("    oomQueriesPercentage: ").append(toIndentedString(oomQueriesPercentage)).append("\n");
    sb.append("    timeOutQueriesPercentage: ").append(toIndentedString(timeOutQueriesPercentage)).append("\n");
    sb.append("    rejectedQueriesPercentage: ").append(toIndentedString(rejectedQueriesPercentage)).append("\n");
    sb.append("    avgWaitTimeInQueue: ").append(toIndentedString(avgWaitTimeInQueue)).append("\n");
    sb.append("    peakAllocationTimestampMS: ").append(toIndentedString(peakAllocationTimestampMS)).append("\n");
    sb.append("    maxAllocatedMemory: ").append(toIndentedString(maxAllocatedMemory)).append("\n");
    sb.append("    maxAllocatedMemoryPercentage: ").append(toIndentedString(maxAllocatedMemoryPercentage)).append("\n");
    sb.append("    utilizedAtMaxAllocated: ").append(toIndentedString(utilizedAtMaxAllocated)).append("\n");
    sb.append("    utilizedAtMaxAllocatedPercentage: ").append(toIndentedString(utilizedAtMaxAllocatedPercentage)).append("\n");
    sb.append("    peakUsageTimestampMS: ").append(toIndentedString(peakUsageTimestampMS)).append("\n");
    sb.append("    maxUtilizedMemory: ").append(toIndentedString(maxUtilizedMemory)).append("\n");
    sb.append("    maxUtilizedMemoryPercentage: ").append(toIndentedString(maxUtilizedMemoryPercentage)).append("\n");
    sb.append("    allocatedAtMaxUtilized: ").append(toIndentedString(allocatedAtMaxUtilized)).append("\n");
    sb.append("    allocatedAtMaxUtilizedPercentage: ").append(toIndentedString(allocatedAtMaxUtilizedPercentage)).append("\n");
    sb.append("    distributionUtilizedByImpalaDaemon: ").append(toIndentedString(distributionUtilizedByImpalaDaemon)).append("\n");
    sb.append("    distributionAllocatedByImpalaDaemon: ").append(toIndentedString(distributionAllocatedByImpalaDaemon)).append("\n");
    sb.append("    tenantUtilizations: ").append(toIndentedString(tenantUtilizations)).append("\n");
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

