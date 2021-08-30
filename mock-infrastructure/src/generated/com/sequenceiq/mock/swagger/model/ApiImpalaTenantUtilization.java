package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiImpalaUtilizationHistogram;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Utilization report information of a tenant of Impala application.
 */
@ApiModel(description = "Utilization report information of a tenant of Impala application.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiImpalaTenantUtilization   {
  @JsonProperty("tenantName")
  private String tenantName = null;

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

  @JsonProperty("avgSpilledMemory")
  private BigDecimal avgSpilledMemory = null;

  @JsonProperty("maxSpilledMemory")
  private BigDecimal maxSpilledMemory = null;

  public ApiImpalaTenantUtilization tenantName(String tenantName) {
    this.tenantName = tenantName;
    return this;
  }

  /**
   * Name of the tenant.
   * @return tenantName
  **/
  @ApiModelProperty(value = "Name of the tenant.")


  public String getTenantName() {
    return tenantName;
  }

  public void setTenantName(String tenantName) {
    this.tenantName = tenantName;
  }

  public ApiImpalaTenantUtilization totalQueries(BigDecimal totalQueries) {
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

  public ApiImpalaTenantUtilization successfulQueries(BigDecimal successfulQueries) {
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

  public ApiImpalaTenantUtilization oomQueries(BigDecimal oomQueries) {
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

  public ApiImpalaTenantUtilization timeOutQueries(BigDecimal timeOutQueries) {
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

  public ApiImpalaTenantUtilization rejectedQueries(BigDecimal rejectedQueries) {
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

  public ApiImpalaTenantUtilization avgWaitTimeInQueue(BigDecimal avgWaitTimeInQueue) {
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

  public ApiImpalaTenantUtilization peakAllocationTimestampMS(Integer peakAllocationTimestampMS) {
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

  public ApiImpalaTenantUtilization maxAllocatedMemory(BigDecimal maxAllocatedMemory) {
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

  public ApiImpalaTenantUtilization maxAllocatedMemoryPercentage(BigDecimal maxAllocatedMemoryPercentage) {
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

  public ApiImpalaTenantUtilization utilizedAtMaxAllocated(BigDecimal utilizedAtMaxAllocated) {
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

  public ApiImpalaTenantUtilization utilizedAtMaxAllocatedPercentage(BigDecimal utilizedAtMaxAllocatedPercentage) {
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

  public ApiImpalaTenantUtilization peakUsageTimestampMS(Integer peakUsageTimestampMS) {
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

  public ApiImpalaTenantUtilization maxUtilizedMemory(BigDecimal maxUtilizedMemory) {
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

  public ApiImpalaTenantUtilization maxUtilizedMemoryPercentage(BigDecimal maxUtilizedMemoryPercentage) {
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

  public ApiImpalaTenantUtilization allocatedAtMaxUtilized(BigDecimal allocatedAtMaxUtilized) {
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

  public ApiImpalaTenantUtilization allocatedAtMaxUtilizedPercentage(BigDecimal allocatedAtMaxUtilizedPercentage) {
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

  public ApiImpalaTenantUtilization distributionUtilizedByImpalaDaemon(ApiImpalaUtilizationHistogram distributionUtilizedByImpalaDaemon) {
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

  public ApiImpalaTenantUtilization distributionAllocatedByImpalaDaemon(ApiImpalaUtilizationHistogram distributionAllocatedByImpalaDaemon) {
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

  public ApiImpalaTenantUtilization avgSpilledMemory(BigDecimal avgSpilledMemory) {
    this.avgSpilledMemory = avgSpilledMemory;
    return this;
  }

  /**
   * Average spill per query.
   * @return avgSpilledMemory
  **/
  @ApiModelProperty(value = "Average spill per query.")

  @Valid

  public BigDecimal getAvgSpilledMemory() {
    return avgSpilledMemory;
  }

  public void setAvgSpilledMemory(BigDecimal avgSpilledMemory) {
    this.avgSpilledMemory = avgSpilledMemory;
  }

  public ApiImpalaTenantUtilization maxSpilledMemory(BigDecimal maxSpilledMemory) {
    this.maxSpilledMemory = maxSpilledMemory;
    return this;
  }

  /**
   * Maximum spill per query.
   * @return maxSpilledMemory
  **/
  @ApiModelProperty(value = "Maximum spill per query.")

  @Valid

  public BigDecimal getMaxSpilledMemory() {
    return maxSpilledMemory;
  }

  public void setMaxSpilledMemory(BigDecimal maxSpilledMemory) {
    this.maxSpilledMemory = maxSpilledMemory;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiImpalaTenantUtilization apiImpalaTenantUtilization = (ApiImpalaTenantUtilization) o;
    return Objects.equals(this.tenantName, apiImpalaTenantUtilization.tenantName) &&
        Objects.equals(this.totalQueries, apiImpalaTenantUtilization.totalQueries) &&
        Objects.equals(this.successfulQueries, apiImpalaTenantUtilization.successfulQueries) &&
        Objects.equals(this.oomQueries, apiImpalaTenantUtilization.oomQueries) &&
        Objects.equals(this.timeOutQueries, apiImpalaTenantUtilization.timeOutQueries) &&
        Objects.equals(this.rejectedQueries, apiImpalaTenantUtilization.rejectedQueries) &&
        Objects.equals(this.avgWaitTimeInQueue, apiImpalaTenantUtilization.avgWaitTimeInQueue) &&
        Objects.equals(this.peakAllocationTimestampMS, apiImpalaTenantUtilization.peakAllocationTimestampMS) &&
        Objects.equals(this.maxAllocatedMemory, apiImpalaTenantUtilization.maxAllocatedMemory) &&
        Objects.equals(this.maxAllocatedMemoryPercentage, apiImpalaTenantUtilization.maxAllocatedMemoryPercentage) &&
        Objects.equals(this.utilizedAtMaxAllocated, apiImpalaTenantUtilization.utilizedAtMaxAllocated) &&
        Objects.equals(this.utilizedAtMaxAllocatedPercentage, apiImpalaTenantUtilization.utilizedAtMaxAllocatedPercentage) &&
        Objects.equals(this.peakUsageTimestampMS, apiImpalaTenantUtilization.peakUsageTimestampMS) &&
        Objects.equals(this.maxUtilizedMemory, apiImpalaTenantUtilization.maxUtilizedMemory) &&
        Objects.equals(this.maxUtilizedMemoryPercentage, apiImpalaTenantUtilization.maxUtilizedMemoryPercentage) &&
        Objects.equals(this.allocatedAtMaxUtilized, apiImpalaTenantUtilization.allocatedAtMaxUtilized) &&
        Objects.equals(this.allocatedAtMaxUtilizedPercentage, apiImpalaTenantUtilization.allocatedAtMaxUtilizedPercentage) &&
        Objects.equals(this.distributionUtilizedByImpalaDaemon, apiImpalaTenantUtilization.distributionUtilizedByImpalaDaemon) &&
        Objects.equals(this.distributionAllocatedByImpalaDaemon, apiImpalaTenantUtilization.distributionAllocatedByImpalaDaemon) &&
        Objects.equals(this.avgSpilledMemory, apiImpalaTenantUtilization.avgSpilledMemory) &&
        Objects.equals(this.maxSpilledMemory, apiImpalaTenantUtilization.maxSpilledMemory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantName, totalQueries, successfulQueries, oomQueries, timeOutQueries, rejectedQueries, avgWaitTimeInQueue, peakAllocationTimestampMS, maxAllocatedMemory, maxAllocatedMemoryPercentage, utilizedAtMaxAllocated, utilizedAtMaxAllocatedPercentage, peakUsageTimestampMS, maxUtilizedMemory, maxUtilizedMemoryPercentage, allocatedAtMaxUtilized, allocatedAtMaxUtilizedPercentage, distributionUtilizedByImpalaDaemon, distributionAllocatedByImpalaDaemon, avgSpilledMemory, maxSpilledMemory);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaTenantUtilization {\n");
    
    sb.append("    tenantName: ").append(toIndentedString(tenantName)).append("\n");
    sb.append("    totalQueries: ").append(toIndentedString(totalQueries)).append("\n");
    sb.append("    successfulQueries: ").append(toIndentedString(successfulQueries)).append("\n");
    sb.append("    oomQueries: ").append(toIndentedString(oomQueries)).append("\n");
    sb.append("    timeOutQueries: ").append(toIndentedString(timeOutQueries)).append("\n");
    sb.append("    rejectedQueries: ").append(toIndentedString(rejectedQueries)).append("\n");
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
    sb.append("    avgSpilledMemory: ").append(toIndentedString(avgSpilledMemory)).append("\n");
    sb.append("    maxSpilledMemory: ").append(toIndentedString(maxSpilledMemory)).append("\n");
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

