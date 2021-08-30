package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiYarnTenantUtilizationList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Utilization report information of a Yarn application service.
 */
@ApiModel(description = "Utilization report information of a Yarn application service.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiYarnUtilization   {
  @JsonProperty("avgCpuUtilization")
  private BigDecimal avgCpuUtilization = null;

  @JsonProperty("maxCpuUtilization")
  private BigDecimal maxCpuUtilization = null;

  @JsonProperty("avgCpuDailyPeak")
  private BigDecimal avgCpuDailyPeak = null;

  @JsonProperty("maxCpuUtilizationTimestampMs")
  private Integer maxCpuUtilizationTimestampMs = null;

  @JsonProperty("avgCpuUtilizationPercentage")
  private BigDecimal avgCpuUtilizationPercentage = null;

  @JsonProperty("maxCpuUtilizationPercentage")
  private BigDecimal maxCpuUtilizationPercentage = null;

  @JsonProperty("avgCpuDailyPeakPercentage")
  private BigDecimal avgCpuDailyPeakPercentage = null;

  @JsonProperty("avgMemoryUtilization")
  private BigDecimal avgMemoryUtilization = null;

  @JsonProperty("maxMemoryUtilization")
  private BigDecimal maxMemoryUtilization = null;

  @JsonProperty("avgMemoryDailyPeak")
  private BigDecimal avgMemoryDailyPeak = null;

  @JsonProperty("maxMemoryUtilizationTimestampMs")
  private Integer maxMemoryUtilizationTimestampMs = null;

  @JsonProperty("avgMemoryUtilizationPercentage")
  private BigDecimal avgMemoryUtilizationPercentage = null;

  @JsonProperty("maxMemoryUtilizationPercentage")
  private BigDecimal maxMemoryUtilizationPercentage = null;

  @JsonProperty("avgMemoryDailyPeakPercentage")
  private BigDecimal avgMemoryDailyPeakPercentage = null;

  @JsonProperty("tenantUtilizations")
  private ApiYarnTenantUtilizationList tenantUtilizations = null;

  @JsonProperty("errorMessage")
  private String errorMessage = null;

  public ApiYarnUtilization avgCpuUtilization(BigDecimal avgCpuUtilization) {
    this.avgCpuUtilization = avgCpuUtilization;
    return this;
  }

  /**
   * Average number of VCores used by YARN applications during the report window.
   * @return avgCpuUtilization
  **/
  @ApiModelProperty(value = "Average number of VCores used by YARN applications during the report window.")

  @Valid

  public BigDecimal getAvgCpuUtilization() {
    return avgCpuUtilization;
  }

  public void setAvgCpuUtilization(BigDecimal avgCpuUtilization) {
    this.avgCpuUtilization = avgCpuUtilization;
  }

  public ApiYarnUtilization maxCpuUtilization(BigDecimal maxCpuUtilization) {
    this.maxCpuUtilization = maxCpuUtilization;
    return this;
  }

  /**
   * Maximum number of VCores used by YARN applications during the report window.
   * @return maxCpuUtilization
  **/
  @ApiModelProperty(value = "Maximum number of VCores used by YARN applications during the report window.")

  @Valid

  public BigDecimal getMaxCpuUtilization() {
    return maxCpuUtilization;
  }

  public void setMaxCpuUtilization(BigDecimal maxCpuUtilization) {
    this.maxCpuUtilization = maxCpuUtilization;
  }

  public ApiYarnUtilization avgCpuDailyPeak(BigDecimal avgCpuDailyPeak) {
    this.avgCpuDailyPeak = avgCpuDailyPeak;
    return this;
  }

  /**
   * Average daily peak VCores used by YARN applications during the report window. The number is computed by first finding the maximum resource consumption per day and then taking their mean.
   * @return avgCpuDailyPeak
  **/
  @ApiModelProperty(value = "Average daily peak VCores used by YARN applications during the report window. The number is computed by first finding the maximum resource consumption per day and then taking their mean.")

  @Valid

  public BigDecimal getAvgCpuDailyPeak() {
    return avgCpuDailyPeak;
  }

  public void setAvgCpuDailyPeak(BigDecimal avgCpuDailyPeak) {
    this.avgCpuDailyPeak = avgCpuDailyPeak;
  }

  public ApiYarnUtilization maxCpuUtilizationTimestampMs(Integer maxCpuUtilizationTimestampMs) {
    this.maxCpuUtilizationTimestampMs = maxCpuUtilizationTimestampMs;
    return this;
  }

  /**
   * Timestamp corresponds to maximum number of VCores used by YARN applications during the report window.
   * @return maxCpuUtilizationTimestampMs
  **/
  @ApiModelProperty(value = "Timestamp corresponds to maximum number of VCores used by YARN applications during the report window.")


  public Integer getMaxCpuUtilizationTimestampMs() {
    return maxCpuUtilizationTimestampMs;
  }

  public void setMaxCpuUtilizationTimestampMs(Integer maxCpuUtilizationTimestampMs) {
    this.maxCpuUtilizationTimestampMs = maxCpuUtilizationTimestampMs;
  }

  public ApiYarnUtilization avgCpuUtilizationPercentage(BigDecimal avgCpuUtilizationPercentage) {
    this.avgCpuUtilizationPercentage = avgCpuUtilizationPercentage;
    return this;
  }

  /**
   * Average percentage of VCores used by YARN applications during the report window.
   * @return avgCpuUtilizationPercentage
  **/
  @ApiModelProperty(value = "Average percentage of VCores used by YARN applications during the report window.")

  @Valid

  public BigDecimal getAvgCpuUtilizationPercentage() {
    return avgCpuUtilizationPercentage;
  }

  public void setAvgCpuUtilizationPercentage(BigDecimal avgCpuUtilizationPercentage) {
    this.avgCpuUtilizationPercentage = avgCpuUtilizationPercentage;
  }

  public ApiYarnUtilization maxCpuUtilizationPercentage(BigDecimal maxCpuUtilizationPercentage) {
    this.maxCpuUtilizationPercentage = maxCpuUtilizationPercentage;
    return this;
  }

  /**
   * Maximum percentage of VCores used by YARN applications during the report window.
   * @return maxCpuUtilizationPercentage
  **/
  @ApiModelProperty(value = "Maximum percentage of VCores used by YARN applications during the report window.")

  @Valid

  public BigDecimal getMaxCpuUtilizationPercentage() {
    return maxCpuUtilizationPercentage;
  }

  public void setMaxCpuUtilizationPercentage(BigDecimal maxCpuUtilizationPercentage) {
    this.maxCpuUtilizationPercentage = maxCpuUtilizationPercentage;
  }

  public ApiYarnUtilization avgCpuDailyPeakPercentage(BigDecimal avgCpuDailyPeakPercentage) {
    this.avgCpuDailyPeakPercentage = avgCpuDailyPeakPercentage;
    return this;
  }

  /**
   * Average daily peak percentage of VCores used by YARN applications during the report window.
   * @return avgCpuDailyPeakPercentage
  **/
  @ApiModelProperty(value = "Average daily peak percentage of VCores used by YARN applications during the report window.")

  @Valid

  public BigDecimal getAvgCpuDailyPeakPercentage() {
    return avgCpuDailyPeakPercentage;
  }

  public void setAvgCpuDailyPeakPercentage(BigDecimal avgCpuDailyPeakPercentage) {
    this.avgCpuDailyPeakPercentage = avgCpuDailyPeakPercentage;
  }

  public ApiYarnUtilization avgMemoryUtilization(BigDecimal avgMemoryUtilization) {
    this.avgMemoryUtilization = avgMemoryUtilization;
    return this;
  }

  /**
   * Average memory used by YARN applications during the report window.
   * @return avgMemoryUtilization
  **/
  @ApiModelProperty(value = "Average memory used by YARN applications during the report window.")

  @Valid

  public BigDecimal getAvgMemoryUtilization() {
    return avgMemoryUtilization;
  }

  public void setAvgMemoryUtilization(BigDecimal avgMemoryUtilization) {
    this.avgMemoryUtilization = avgMemoryUtilization;
  }

  public ApiYarnUtilization maxMemoryUtilization(BigDecimal maxMemoryUtilization) {
    this.maxMemoryUtilization = maxMemoryUtilization;
    return this;
  }

  /**
   * Maximum memory used by YARN applications during the report window.
   * @return maxMemoryUtilization
  **/
  @ApiModelProperty(value = "Maximum memory used by YARN applications during the report window.")

  @Valid

  public BigDecimal getMaxMemoryUtilization() {
    return maxMemoryUtilization;
  }

  public void setMaxMemoryUtilization(BigDecimal maxMemoryUtilization) {
    this.maxMemoryUtilization = maxMemoryUtilization;
  }

  public ApiYarnUtilization avgMemoryDailyPeak(BigDecimal avgMemoryDailyPeak) {
    this.avgMemoryDailyPeak = avgMemoryDailyPeak;
    return this;
  }

  /**
   * Average daily peak memory used by YARN applications during the report window. The number is computed by first finding the maximum resource consumption per day and then taking their mean.
   * @return avgMemoryDailyPeak
  **/
  @ApiModelProperty(value = "Average daily peak memory used by YARN applications during the report window. The number is computed by first finding the maximum resource consumption per day and then taking their mean.")

  @Valid

  public BigDecimal getAvgMemoryDailyPeak() {
    return avgMemoryDailyPeak;
  }

  public void setAvgMemoryDailyPeak(BigDecimal avgMemoryDailyPeak) {
    this.avgMemoryDailyPeak = avgMemoryDailyPeak;
  }

  public ApiYarnUtilization maxMemoryUtilizationTimestampMs(Integer maxMemoryUtilizationTimestampMs) {
    this.maxMemoryUtilizationTimestampMs = maxMemoryUtilizationTimestampMs;
    return this;
  }

  /**
   * Timestamp corresponds to maximum memory used by YARN applications during the report window.
   * @return maxMemoryUtilizationTimestampMs
  **/
  @ApiModelProperty(value = "Timestamp corresponds to maximum memory used by YARN applications during the report window.")


  public Integer getMaxMemoryUtilizationTimestampMs() {
    return maxMemoryUtilizationTimestampMs;
  }

  public void setMaxMemoryUtilizationTimestampMs(Integer maxMemoryUtilizationTimestampMs) {
    this.maxMemoryUtilizationTimestampMs = maxMemoryUtilizationTimestampMs;
  }

  public ApiYarnUtilization avgMemoryUtilizationPercentage(BigDecimal avgMemoryUtilizationPercentage) {
    this.avgMemoryUtilizationPercentage = avgMemoryUtilizationPercentage;
    return this;
  }

  /**
   * Average percentage memory used by YARN applications during the report window.
   * @return avgMemoryUtilizationPercentage
  **/
  @ApiModelProperty(value = "Average percentage memory used by YARN applications during the report window.")

  @Valid

  public BigDecimal getAvgMemoryUtilizationPercentage() {
    return avgMemoryUtilizationPercentage;
  }

  public void setAvgMemoryUtilizationPercentage(BigDecimal avgMemoryUtilizationPercentage) {
    this.avgMemoryUtilizationPercentage = avgMemoryUtilizationPercentage;
  }

  public ApiYarnUtilization maxMemoryUtilizationPercentage(BigDecimal maxMemoryUtilizationPercentage) {
    this.maxMemoryUtilizationPercentage = maxMemoryUtilizationPercentage;
    return this;
  }

  /**
   * Maximum percentage of memory used by YARN applications during the report window.
   * @return maxMemoryUtilizationPercentage
  **/
  @ApiModelProperty(value = "Maximum percentage of memory used by YARN applications during the report window.")

  @Valid

  public BigDecimal getMaxMemoryUtilizationPercentage() {
    return maxMemoryUtilizationPercentage;
  }

  public void setMaxMemoryUtilizationPercentage(BigDecimal maxMemoryUtilizationPercentage) {
    this.maxMemoryUtilizationPercentage = maxMemoryUtilizationPercentage;
  }

  public ApiYarnUtilization avgMemoryDailyPeakPercentage(BigDecimal avgMemoryDailyPeakPercentage) {
    this.avgMemoryDailyPeakPercentage = avgMemoryDailyPeakPercentage;
    return this;
  }

  /**
   * Average daily peak percentage of memory used by YARN applications during the report window.
   * @return avgMemoryDailyPeakPercentage
  **/
  @ApiModelProperty(value = "Average daily peak percentage of memory used by YARN applications during the report window.")

  @Valid

  public BigDecimal getAvgMemoryDailyPeakPercentage() {
    return avgMemoryDailyPeakPercentage;
  }

  public void setAvgMemoryDailyPeakPercentage(BigDecimal avgMemoryDailyPeakPercentage) {
    this.avgMemoryDailyPeakPercentage = avgMemoryDailyPeakPercentage;
  }

  public ApiYarnUtilization tenantUtilizations(ApiYarnTenantUtilizationList tenantUtilizations) {
    this.tenantUtilizations = tenantUtilizations;
    return this;
  }

  /**
   * A list of tenant utilization reports.
   * @return tenantUtilizations
  **/
  @ApiModelProperty(value = "A list of tenant utilization reports.")

  @Valid

  public ApiYarnTenantUtilizationList getTenantUtilizations() {
    return tenantUtilizations;
  }

  public void setTenantUtilizations(ApiYarnTenantUtilizationList tenantUtilizations) {
    this.tenantUtilizations = tenantUtilizations;
  }

  public ApiYarnUtilization errorMessage(String errorMessage) {
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
    ApiYarnUtilization apiYarnUtilization = (ApiYarnUtilization) o;
    return Objects.equals(this.avgCpuUtilization, apiYarnUtilization.avgCpuUtilization) &&
        Objects.equals(this.maxCpuUtilization, apiYarnUtilization.maxCpuUtilization) &&
        Objects.equals(this.avgCpuDailyPeak, apiYarnUtilization.avgCpuDailyPeak) &&
        Objects.equals(this.maxCpuUtilizationTimestampMs, apiYarnUtilization.maxCpuUtilizationTimestampMs) &&
        Objects.equals(this.avgCpuUtilizationPercentage, apiYarnUtilization.avgCpuUtilizationPercentage) &&
        Objects.equals(this.maxCpuUtilizationPercentage, apiYarnUtilization.maxCpuUtilizationPercentage) &&
        Objects.equals(this.avgCpuDailyPeakPercentage, apiYarnUtilization.avgCpuDailyPeakPercentage) &&
        Objects.equals(this.avgMemoryUtilization, apiYarnUtilization.avgMemoryUtilization) &&
        Objects.equals(this.maxMemoryUtilization, apiYarnUtilization.maxMemoryUtilization) &&
        Objects.equals(this.avgMemoryDailyPeak, apiYarnUtilization.avgMemoryDailyPeak) &&
        Objects.equals(this.maxMemoryUtilizationTimestampMs, apiYarnUtilization.maxMemoryUtilizationTimestampMs) &&
        Objects.equals(this.avgMemoryUtilizationPercentage, apiYarnUtilization.avgMemoryUtilizationPercentage) &&
        Objects.equals(this.maxMemoryUtilizationPercentage, apiYarnUtilization.maxMemoryUtilizationPercentage) &&
        Objects.equals(this.avgMemoryDailyPeakPercentage, apiYarnUtilization.avgMemoryDailyPeakPercentage) &&
        Objects.equals(this.tenantUtilizations, apiYarnUtilization.tenantUtilizations) &&
        Objects.equals(this.errorMessage, apiYarnUtilization.errorMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(avgCpuUtilization, maxCpuUtilization, avgCpuDailyPeak, maxCpuUtilizationTimestampMs, avgCpuUtilizationPercentage, maxCpuUtilizationPercentage, avgCpuDailyPeakPercentage, avgMemoryUtilization, maxMemoryUtilization, avgMemoryDailyPeak, maxMemoryUtilizationTimestampMs, avgMemoryUtilizationPercentage, maxMemoryUtilizationPercentage, avgMemoryDailyPeakPercentage, tenantUtilizations, errorMessage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiYarnUtilization {\n");
    
    sb.append("    avgCpuUtilization: ").append(toIndentedString(avgCpuUtilization)).append("\n");
    sb.append("    maxCpuUtilization: ").append(toIndentedString(maxCpuUtilization)).append("\n");
    sb.append("    avgCpuDailyPeak: ").append(toIndentedString(avgCpuDailyPeak)).append("\n");
    sb.append("    maxCpuUtilizationTimestampMs: ").append(toIndentedString(maxCpuUtilizationTimestampMs)).append("\n");
    sb.append("    avgCpuUtilizationPercentage: ").append(toIndentedString(avgCpuUtilizationPercentage)).append("\n");
    sb.append("    maxCpuUtilizationPercentage: ").append(toIndentedString(maxCpuUtilizationPercentage)).append("\n");
    sb.append("    avgCpuDailyPeakPercentage: ").append(toIndentedString(avgCpuDailyPeakPercentage)).append("\n");
    sb.append("    avgMemoryUtilization: ").append(toIndentedString(avgMemoryUtilization)).append("\n");
    sb.append("    maxMemoryUtilization: ").append(toIndentedString(maxMemoryUtilization)).append("\n");
    sb.append("    avgMemoryDailyPeak: ").append(toIndentedString(avgMemoryDailyPeak)).append("\n");
    sb.append("    maxMemoryUtilizationTimestampMs: ").append(toIndentedString(maxMemoryUtilizationTimestampMs)).append("\n");
    sb.append("    avgMemoryUtilizationPercentage: ").append(toIndentedString(avgMemoryUtilizationPercentage)).append("\n");
    sb.append("    maxMemoryUtilizationPercentage: ").append(toIndentedString(maxMemoryUtilizationPercentage)).append("\n");
    sb.append("    avgMemoryDailyPeakPercentage: ").append(toIndentedString(avgMemoryDailyPeakPercentage)).append("\n");
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

