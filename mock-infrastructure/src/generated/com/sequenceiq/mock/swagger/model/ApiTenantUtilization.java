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
 * Utilization report information of a tenant.
 */
@ApiModel(description = "Utilization report information of a tenant.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTenantUtilization   {
  @JsonProperty("tenantName")
  private String tenantName = null;

  @JsonProperty("cpuUtilizationPercentage")
  private BigDecimal cpuUtilizationPercentage = null;

  @JsonProperty("memoryUtilizationPercentage")
  private BigDecimal memoryUtilizationPercentage = null;

  public ApiTenantUtilization tenantName(String tenantName) {
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

  public ApiTenantUtilization cpuUtilizationPercentage(BigDecimal cpuUtilizationPercentage) {
    this.cpuUtilizationPercentage = cpuUtilizationPercentage;
    return this;
  }

  /**
   * Percentage of CPU resource used by workloads.
   * @return cpuUtilizationPercentage
  **/
  @ApiModelProperty(value = "Percentage of CPU resource used by workloads.")

  @Valid

  public BigDecimal getCpuUtilizationPercentage() {
    return cpuUtilizationPercentage;
  }

  public void setCpuUtilizationPercentage(BigDecimal cpuUtilizationPercentage) {
    this.cpuUtilizationPercentage = cpuUtilizationPercentage;
  }

  public ApiTenantUtilization memoryUtilizationPercentage(BigDecimal memoryUtilizationPercentage) {
    this.memoryUtilizationPercentage = memoryUtilizationPercentage;
    return this;
  }

  /**
   * Percentage of memory used by workloads.
   * @return memoryUtilizationPercentage
  **/
  @ApiModelProperty(value = "Percentage of memory used by workloads.")

  @Valid

  public BigDecimal getMemoryUtilizationPercentage() {
    return memoryUtilizationPercentage;
  }

  public void setMemoryUtilizationPercentage(BigDecimal memoryUtilizationPercentage) {
    this.memoryUtilizationPercentage = memoryUtilizationPercentage;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTenantUtilization apiTenantUtilization = (ApiTenantUtilization) o;
    return Objects.equals(this.tenantName, apiTenantUtilization.tenantName) &&
        Objects.equals(this.cpuUtilizationPercentage, apiTenantUtilization.cpuUtilizationPercentage) &&
        Objects.equals(this.memoryUtilizationPercentage, apiTenantUtilization.memoryUtilizationPercentage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantName, cpuUtilizationPercentage, memoryUtilizationPercentage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTenantUtilization {\n");
    
    sb.append("    tenantName: ").append(toIndentedString(tenantName)).append("\n");
    sb.append("    cpuUtilizationPercentage: ").append(toIndentedString(cpuUtilizationPercentage)).append("\n");
    sb.append("    memoryUtilizationPercentage: ").append(toIndentedString(memoryUtilizationPercentage)).append("\n");
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

