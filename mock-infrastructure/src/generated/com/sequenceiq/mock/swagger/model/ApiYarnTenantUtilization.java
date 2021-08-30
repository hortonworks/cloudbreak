package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Utilization report information of a tenant of Yarn application.
 */
@ApiModel(description = "Utilization report information of a tenant of Yarn application.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiYarnTenantUtilization   {
  @JsonProperty("tenantName")
  private String tenantName = null;

  @JsonProperty("avgYarnCpuAllocation")
  private BigDecimal avgYarnCpuAllocation = null;

  @JsonProperty("avgYarnCpuUtilization")
  private BigDecimal avgYarnCpuUtilization = null;

  @JsonProperty("avgYarnCpuUnusedCapacity")
  private BigDecimal avgYarnCpuUnusedCapacity = null;

  @JsonProperty("avgYarnCpuSteadyFairShare")
  private BigDecimal avgYarnCpuSteadyFairShare = null;

  @JsonProperty("avgYarnPoolAllocatedCpuDuringContention")
  private BigDecimal avgYarnPoolAllocatedCpuDuringContention = null;

  @JsonProperty("avgYarnPoolFairShareCpuDuringContention")
  private BigDecimal avgYarnPoolFairShareCpuDuringContention = null;

  @JsonProperty("avgYarnPoolSteadyFairShareCpuDuringContention")
  private BigDecimal avgYarnPoolSteadyFairShareCpuDuringContention = null;

  @JsonProperty("avgYarnContainerWaitRatio")
  private BigDecimal avgYarnContainerWaitRatio = null;

  @JsonProperty("avgYarnMemoryAllocation")
  private BigDecimal avgYarnMemoryAllocation = null;

  @JsonProperty("avgYarnMemoryUtilization")
  private BigDecimal avgYarnMemoryUtilization = null;

  @JsonProperty("avgYarnMemoryUnusedCapacity")
  private BigDecimal avgYarnMemoryUnusedCapacity = null;

  @JsonProperty("avgYarnMemorySteadyFairShare")
  private BigDecimal avgYarnMemorySteadyFairShare = null;

  @JsonProperty("avgYarnPoolAllocatedMemoryDuringContention")
  private BigDecimal avgYarnPoolAllocatedMemoryDuringContention = null;

  @JsonProperty("avgYarnPoolFairShareMemoryDuringContention")
  private BigDecimal avgYarnPoolFairShareMemoryDuringContention = null;

  @JsonProperty("avgYarnPoolSteadyFairShareMemoryDuringContention")
  private BigDecimal avgYarnPoolSteadyFairShareMemoryDuringContention = null;

  public ApiYarnTenantUtilization tenantName(String tenantName) {
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

  public ApiYarnTenantUtilization avgYarnCpuAllocation(BigDecimal avgYarnCpuAllocation) {
    this.avgYarnCpuAllocation = avgYarnCpuAllocation;
    return this;
  }

  /**
   * Average number of VCores allocated to YARN applications of the tenant.
   * @return avgYarnCpuAllocation
  **/
  @ApiModelProperty(value = "Average number of VCores allocated to YARN applications of the tenant.")

  @Valid

  public BigDecimal getAvgYarnCpuAllocation() {
    return avgYarnCpuAllocation;
  }

  public void setAvgYarnCpuAllocation(BigDecimal avgYarnCpuAllocation) {
    this.avgYarnCpuAllocation = avgYarnCpuAllocation;
  }

  public ApiYarnTenantUtilization avgYarnCpuUtilization(BigDecimal avgYarnCpuUtilization) {
    this.avgYarnCpuUtilization = avgYarnCpuUtilization;
    return this;
  }

  /**
   * Average number of VCores used by YARN applications of the tenant.
   * @return avgYarnCpuUtilization
  **/
  @ApiModelProperty(value = "Average number of VCores used by YARN applications of the tenant.")

  @Valid

  public BigDecimal getAvgYarnCpuUtilization() {
    return avgYarnCpuUtilization;
  }

  public void setAvgYarnCpuUtilization(BigDecimal avgYarnCpuUtilization) {
    this.avgYarnCpuUtilization = avgYarnCpuUtilization;
  }

  public ApiYarnTenantUtilization avgYarnCpuUnusedCapacity(BigDecimal avgYarnCpuUnusedCapacity) {
    this.avgYarnCpuUnusedCapacity = avgYarnCpuUnusedCapacity;
    return this;
  }

  /**
   * Average unused VCores of the tenant.
   * @return avgYarnCpuUnusedCapacity
  **/
  @ApiModelProperty(value = "Average unused VCores of the tenant.")

  @Valid

  public BigDecimal getAvgYarnCpuUnusedCapacity() {
    return avgYarnCpuUnusedCapacity;
  }

  public void setAvgYarnCpuUnusedCapacity(BigDecimal avgYarnCpuUnusedCapacity) {
    this.avgYarnCpuUnusedCapacity = avgYarnCpuUnusedCapacity;
  }

  public ApiYarnTenantUtilization avgYarnCpuSteadyFairShare(BigDecimal avgYarnCpuSteadyFairShare) {
    this.avgYarnCpuSteadyFairShare = avgYarnCpuSteadyFairShare;
    return this;
  }

  /**
   * Average steady fair share VCores.
   * @return avgYarnCpuSteadyFairShare
  **/
  @ApiModelProperty(value = "Average steady fair share VCores.")

  @Valid

  public BigDecimal getAvgYarnCpuSteadyFairShare() {
    return avgYarnCpuSteadyFairShare;
  }

  public void setAvgYarnCpuSteadyFairShare(BigDecimal avgYarnCpuSteadyFairShare) {
    this.avgYarnCpuSteadyFairShare = avgYarnCpuSteadyFairShare;
  }

  public ApiYarnTenantUtilization avgYarnPoolAllocatedCpuDuringContention(BigDecimal avgYarnPoolAllocatedCpuDuringContention) {
    this.avgYarnPoolAllocatedCpuDuringContention = avgYarnPoolAllocatedCpuDuringContention;
    return this;
  }

  /**
   * Average allocated Vcores with pending containers.
   * @return avgYarnPoolAllocatedCpuDuringContention
  **/
  @ApiModelProperty(value = "Average allocated Vcores with pending containers.")

  @Valid

  public BigDecimal getAvgYarnPoolAllocatedCpuDuringContention() {
    return avgYarnPoolAllocatedCpuDuringContention;
  }

  public void setAvgYarnPoolAllocatedCpuDuringContention(BigDecimal avgYarnPoolAllocatedCpuDuringContention) {
    this.avgYarnPoolAllocatedCpuDuringContention = avgYarnPoolAllocatedCpuDuringContention;
  }

  public ApiYarnTenantUtilization avgYarnPoolFairShareCpuDuringContention(BigDecimal avgYarnPoolFairShareCpuDuringContention) {
    this.avgYarnPoolFairShareCpuDuringContention = avgYarnPoolFairShareCpuDuringContention;
    return this;
  }

  /**
   * Average fair share VCores with pending containers.
   * @return avgYarnPoolFairShareCpuDuringContention
  **/
  @ApiModelProperty(value = "Average fair share VCores with pending containers.")

  @Valid

  public BigDecimal getAvgYarnPoolFairShareCpuDuringContention() {
    return avgYarnPoolFairShareCpuDuringContention;
  }

  public void setAvgYarnPoolFairShareCpuDuringContention(BigDecimal avgYarnPoolFairShareCpuDuringContention) {
    this.avgYarnPoolFairShareCpuDuringContention = avgYarnPoolFairShareCpuDuringContention;
  }

  public ApiYarnTenantUtilization avgYarnPoolSteadyFairShareCpuDuringContention(BigDecimal avgYarnPoolSteadyFairShareCpuDuringContention) {
    this.avgYarnPoolSteadyFairShareCpuDuringContention = avgYarnPoolSteadyFairShareCpuDuringContention;
    return this;
  }

  /**
   * Average steady fair share VCores with pending containers.
   * @return avgYarnPoolSteadyFairShareCpuDuringContention
  **/
  @ApiModelProperty(value = "Average steady fair share VCores with pending containers.")

  @Valid

  public BigDecimal getAvgYarnPoolSteadyFairShareCpuDuringContention() {
    return avgYarnPoolSteadyFairShareCpuDuringContention;
  }

  public void setAvgYarnPoolSteadyFairShareCpuDuringContention(BigDecimal avgYarnPoolSteadyFairShareCpuDuringContention) {
    this.avgYarnPoolSteadyFairShareCpuDuringContention = avgYarnPoolSteadyFairShareCpuDuringContention;
  }

  public ApiYarnTenantUtilization avgYarnContainerWaitRatio(BigDecimal avgYarnContainerWaitRatio) {
    this.avgYarnContainerWaitRatio = avgYarnContainerWaitRatio;
    return this;
  }

  /**
   * Average percentage of pending containers for the pool during periods of contention.
   * @return avgYarnContainerWaitRatio
  **/
  @ApiModelProperty(value = "Average percentage of pending containers for the pool during periods of contention.")

  @Valid

  public BigDecimal getAvgYarnContainerWaitRatio() {
    return avgYarnContainerWaitRatio;
  }

  public void setAvgYarnContainerWaitRatio(BigDecimal avgYarnContainerWaitRatio) {
    this.avgYarnContainerWaitRatio = avgYarnContainerWaitRatio;
  }

  public ApiYarnTenantUtilization avgYarnMemoryAllocation(BigDecimal avgYarnMemoryAllocation) {
    this.avgYarnMemoryAllocation = avgYarnMemoryAllocation;
    return this;
  }

  /**
   * Average memory allocated to YARN applications of the tenant.
   * @return avgYarnMemoryAllocation
  **/
  @ApiModelProperty(value = "Average memory allocated to YARN applications of the tenant.")

  @Valid

  public BigDecimal getAvgYarnMemoryAllocation() {
    return avgYarnMemoryAllocation;
  }

  public void setAvgYarnMemoryAllocation(BigDecimal avgYarnMemoryAllocation) {
    this.avgYarnMemoryAllocation = avgYarnMemoryAllocation;
  }

  public ApiYarnTenantUtilization avgYarnMemoryUtilization(BigDecimal avgYarnMemoryUtilization) {
    this.avgYarnMemoryUtilization = avgYarnMemoryUtilization;
    return this;
  }

  /**
   * Average memory used by YARN applications of the tenant.
   * @return avgYarnMemoryUtilization
  **/
  @ApiModelProperty(value = "Average memory used by YARN applications of the tenant.")

  @Valid

  public BigDecimal getAvgYarnMemoryUtilization() {
    return avgYarnMemoryUtilization;
  }

  public void setAvgYarnMemoryUtilization(BigDecimal avgYarnMemoryUtilization) {
    this.avgYarnMemoryUtilization = avgYarnMemoryUtilization;
  }

  public ApiYarnTenantUtilization avgYarnMemoryUnusedCapacity(BigDecimal avgYarnMemoryUnusedCapacity) {
    this.avgYarnMemoryUnusedCapacity = avgYarnMemoryUnusedCapacity;
    return this;
  }

  /**
   * Average unused memory of the tenant.
   * @return avgYarnMemoryUnusedCapacity
  **/
  @ApiModelProperty(value = "Average unused memory of the tenant.")

  @Valid

  public BigDecimal getAvgYarnMemoryUnusedCapacity() {
    return avgYarnMemoryUnusedCapacity;
  }

  public void setAvgYarnMemoryUnusedCapacity(BigDecimal avgYarnMemoryUnusedCapacity) {
    this.avgYarnMemoryUnusedCapacity = avgYarnMemoryUnusedCapacity;
  }

  public ApiYarnTenantUtilization avgYarnMemorySteadyFairShare(BigDecimal avgYarnMemorySteadyFairShare) {
    this.avgYarnMemorySteadyFairShare = avgYarnMemorySteadyFairShare;
    return this;
  }

  /**
   * Average steady fair share memory.
   * @return avgYarnMemorySteadyFairShare
  **/
  @ApiModelProperty(value = "Average steady fair share memory.")

  @Valid

  public BigDecimal getAvgYarnMemorySteadyFairShare() {
    return avgYarnMemorySteadyFairShare;
  }

  public void setAvgYarnMemorySteadyFairShare(BigDecimal avgYarnMemorySteadyFairShare) {
    this.avgYarnMemorySteadyFairShare = avgYarnMemorySteadyFairShare;
  }

  public ApiYarnTenantUtilization avgYarnPoolAllocatedMemoryDuringContention(BigDecimal avgYarnPoolAllocatedMemoryDuringContention) {
    this.avgYarnPoolAllocatedMemoryDuringContention = avgYarnPoolAllocatedMemoryDuringContention;
    return this;
  }

  /**
   * Average allocated memory with pending containers.
   * @return avgYarnPoolAllocatedMemoryDuringContention
  **/
  @ApiModelProperty(value = "Average allocated memory with pending containers.")

  @Valid

  public BigDecimal getAvgYarnPoolAllocatedMemoryDuringContention() {
    return avgYarnPoolAllocatedMemoryDuringContention;
  }

  public void setAvgYarnPoolAllocatedMemoryDuringContention(BigDecimal avgYarnPoolAllocatedMemoryDuringContention) {
    this.avgYarnPoolAllocatedMemoryDuringContention = avgYarnPoolAllocatedMemoryDuringContention;
  }

  public ApiYarnTenantUtilization avgYarnPoolFairShareMemoryDuringContention(BigDecimal avgYarnPoolFairShareMemoryDuringContention) {
    this.avgYarnPoolFairShareMemoryDuringContention = avgYarnPoolFairShareMemoryDuringContention;
    return this;
  }

  /**
   * Average fair share memory with pending containers.
   * @return avgYarnPoolFairShareMemoryDuringContention
  **/
  @ApiModelProperty(value = "Average fair share memory with pending containers.")

  @Valid

  public BigDecimal getAvgYarnPoolFairShareMemoryDuringContention() {
    return avgYarnPoolFairShareMemoryDuringContention;
  }

  public void setAvgYarnPoolFairShareMemoryDuringContention(BigDecimal avgYarnPoolFairShareMemoryDuringContention) {
    this.avgYarnPoolFairShareMemoryDuringContention = avgYarnPoolFairShareMemoryDuringContention;
  }

  public ApiYarnTenantUtilization avgYarnPoolSteadyFairShareMemoryDuringContention(BigDecimal avgYarnPoolSteadyFairShareMemoryDuringContention) {
    this.avgYarnPoolSteadyFairShareMemoryDuringContention = avgYarnPoolSteadyFairShareMemoryDuringContention;
    return this;
  }

  /**
   * Average steady fair share memory with pending containers.
   * @return avgYarnPoolSteadyFairShareMemoryDuringContention
  **/
  @ApiModelProperty(value = "Average steady fair share memory with pending containers.")

  @Valid

  public BigDecimal getAvgYarnPoolSteadyFairShareMemoryDuringContention() {
    return avgYarnPoolSteadyFairShareMemoryDuringContention;
  }

  public void setAvgYarnPoolSteadyFairShareMemoryDuringContention(BigDecimal avgYarnPoolSteadyFairShareMemoryDuringContention) {
    this.avgYarnPoolSteadyFairShareMemoryDuringContention = avgYarnPoolSteadyFairShareMemoryDuringContention;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiYarnTenantUtilization apiYarnTenantUtilization = (ApiYarnTenantUtilization) o;
    return Objects.equals(this.tenantName, apiYarnTenantUtilization.tenantName) &&
        Objects.equals(this.avgYarnCpuAllocation, apiYarnTenantUtilization.avgYarnCpuAllocation) &&
        Objects.equals(this.avgYarnCpuUtilization, apiYarnTenantUtilization.avgYarnCpuUtilization) &&
        Objects.equals(this.avgYarnCpuUnusedCapacity, apiYarnTenantUtilization.avgYarnCpuUnusedCapacity) &&
        Objects.equals(this.avgYarnCpuSteadyFairShare, apiYarnTenantUtilization.avgYarnCpuSteadyFairShare) &&
        Objects.equals(this.avgYarnPoolAllocatedCpuDuringContention, apiYarnTenantUtilization.avgYarnPoolAllocatedCpuDuringContention) &&
        Objects.equals(this.avgYarnPoolFairShareCpuDuringContention, apiYarnTenantUtilization.avgYarnPoolFairShareCpuDuringContention) &&
        Objects.equals(this.avgYarnPoolSteadyFairShareCpuDuringContention, apiYarnTenantUtilization.avgYarnPoolSteadyFairShareCpuDuringContention) &&
        Objects.equals(this.avgYarnContainerWaitRatio, apiYarnTenantUtilization.avgYarnContainerWaitRatio) &&
        Objects.equals(this.avgYarnMemoryAllocation, apiYarnTenantUtilization.avgYarnMemoryAllocation) &&
        Objects.equals(this.avgYarnMemoryUtilization, apiYarnTenantUtilization.avgYarnMemoryUtilization) &&
        Objects.equals(this.avgYarnMemoryUnusedCapacity, apiYarnTenantUtilization.avgYarnMemoryUnusedCapacity) &&
        Objects.equals(this.avgYarnMemorySteadyFairShare, apiYarnTenantUtilization.avgYarnMemorySteadyFairShare) &&
        Objects.equals(this.avgYarnPoolAllocatedMemoryDuringContention, apiYarnTenantUtilization.avgYarnPoolAllocatedMemoryDuringContention) &&
        Objects.equals(this.avgYarnPoolFairShareMemoryDuringContention, apiYarnTenantUtilization.avgYarnPoolFairShareMemoryDuringContention) &&
        Objects.equals(this.avgYarnPoolSteadyFairShareMemoryDuringContention, apiYarnTenantUtilization.avgYarnPoolSteadyFairShareMemoryDuringContention);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantName, avgYarnCpuAllocation, avgYarnCpuUtilization, avgYarnCpuUnusedCapacity, avgYarnCpuSteadyFairShare, avgYarnPoolAllocatedCpuDuringContention, avgYarnPoolFairShareCpuDuringContention, avgYarnPoolSteadyFairShareCpuDuringContention, avgYarnContainerWaitRatio, avgYarnMemoryAllocation, avgYarnMemoryUtilization, avgYarnMemoryUnusedCapacity, avgYarnMemorySteadyFairShare, avgYarnPoolAllocatedMemoryDuringContention, avgYarnPoolFairShareMemoryDuringContention, avgYarnPoolSteadyFairShareMemoryDuringContention);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiYarnTenantUtilization {\n");
    
    sb.append("    tenantName: ").append(toIndentedString(tenantName)).append("\n");
    sb.append("    avgYarnCpuAllocation: ").append(toIndentedString(avgYarnCpuAllocation)).append("\n");
    sb.append("    avgYarnCpuUtilization: ").append(toIndentedString(avgYarnCpuUtilization)).append("\n");
    sb.append("    avgYarnCpuUnusedCapacity: ").append(toIndentedString(avgYarnCpuUnusedCapacity)).append("\n");
    sb.append("    avgYarnCpuSteadyFairShare: ").append(toIndentedString(avgYarnCpuSteadyFairShare)).append("\n");
    sb.append("    avgYarnPoolAllocatedCpuDuringContention: ").append(toIndentedString(avgYarnPoolAllocatedCpuDuringContention)).append("\n");
    sb.append("    avgYarnPoolFairShareCpuDuringContention: ").append(toIndentedString(avgYarnPoolFairShareCpuDuringContention)).append("\n");
    sb.append("    avgYarnPoolSteadyFairShareCpuDuringContention: ").append(toIndentedString(avgYarnPoolSteadyFairShareCpuDuringContention)).append("\n");
    sb.append("    avgYarnContainerWaitRatio: ").append(toIndentedString(avgYarnContainerWaitRatio)).append("\n");
    sb.append("    avgYarnMemoryAllocation: ").append(toIndentedString(avgYarnMemoryAllocation)).append("\n");
    sb.append("    avgYarnMemoryUtilization: ").append(toIndentedString(avgYarnMemoryUtilization)).append("\n");
    sb.append("    avgYarnMemoryUnusedCapacity: ").append(toIndentedString(avgYarnMemoryUnusedCapacity)).append("\n");
    sb.append("    avgYarnMemorySteadyFairShare: ").append(toIndentedString(avgYarnMemorySteadyFairShare)).append("\n");
    sb.append("    avgYarnPoolAllocatedMemoryDuringContention: ").append(toIndentedString(avgYarnPoolAllocatedMemoryDuringContention)).append("\n");
    sb.append("    avgYarnPoolFairShareMemoryDuringContention: ").append(toIndentedString(avgYarnPoolFairShareMemoryDuringContention)).append("\n");
    sb.append("    avgYarnPoolSteadyFairShareMemoryDuringContention: ").append(toIndentedString(avgYarnPoolSteadyFairShareMemoryDuringContention)).append("\n");
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

