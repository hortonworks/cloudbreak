package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiTimeSeriesCrossEntityMetadata;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Statistics related to one time series aggregate data point. It is available from v6 for data points containing aggregate data. It includes further statistics about the data point. An aggregate can be across entities (e.g., fd_open_across_datanodes), over time (e.g., a daily point for the fd_open metric for a specific DataNode), or both (e.g., a daily point for the fd_open_across_datanodes metric). If the data point is for non-aggregate date this will return null.
 */
@ApiModel(description = "Statistics related to one time series aggregate data point. It is available from v6 for data points containing aggregate data. It includes further statistics about the data point. An aggregate can be across entities (e.g., fd_open_across_datanodes), over time (e.g., a daily point for the fd_open metric for a specific DataNode), or both (e.g., a daily point for the fd_open_across_datanodes metric). If the data point is for non-aggregate date this will return null.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTimeSeriesAggregateStatistics   {
  @JsonProperty("sampleTime")
  private String sampleTime = null;

  @JsonProperty("sampleValue")
  private BigDecimal sampleValue = null;

  @JsonProperty("count")
  private Integer count = null;

  @JsonProperty("min")
  private BigDecimal min = null;

  @JsonProperty("minTime")
  private String minTime = null;

  @JsonProperty("max")
  private BigDecimal max = null;

  @JsonProperty("maxTime")
  private String maxTime = null;

  @JsonProperty("mean")
  private BigDecimal mean = null;

  @JsonProperty("stdDev")
  private BigDecimal stdDev = null;

  @JsonProperty("crossEntityMetadata")
  private ApiTimeSeriesCrossEntityMetadata crossEntityMetadata = null;

  public ApiTimeSeriesAggregateStatistics sampleTime(String sampleTime) {
    this.sampleTime = sampleTime;
    return this;
  }

  /**
   * The timestamp of the sample data point. Note that the timestamp reflects coordinated universal time (UTC) and not necessarily the server's time zone. The rest API formats the UTC timestamp as an ISO-8061 string.
   * @return sampleTime
  **/
  @ApiModelProperty(value = "The timestamp of the sample data point. Note that the timestamp reflects coordinated universal time (UTC) and not necessarily the server's time zone. The rest API formats the UTC timestamp as an ISO-8061 string.")


  public String getSampleTime() {
    return sampleTime;
  }

  public void setSampleTime(String sampleTime) {
    this.sampleTime = sampleTime;
  }

  public ApiTimeSeriesAggregateStatistics sampleValue(BigDecimal sampleValue) {
    this.sampleValue = sampleValue;
    return this;
  }

  /**
   * The sample data point value representing an actual sample value picked from the underlying data that is being aggregated.
   * @return sampleValue
  **/
  @ApiModelProperty(value = "The sample data point value representing an actual sample value picked from the underlying data that is being aggregated.")

  @Valid

  public BigDecimal getSampleValue() {
    return sampleValue;
  }

  public void setSampleValue(BigDecimal sampleValue) {
    this.sampleValue = sampleValue;
  }

  public ApiTimeSeriesAggregateStatistics count(Integer count) {
    this.count = count;
    return this;
  }

  /**
   * The number of individual data points aggregated in this data point.
   * @return count
  **/
  @ApiModelProperty(value = "The number of individual data points aggregated in this data point.")


  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public ApiTimeSeriesAggregateStatistics min(BigDecimal min) {
    this.min = min;
    return this;
  }

  /**
   * This minimum value encountered while producing this aggregate data point. If this is a cross-time aggregate then this is the minimum value encountered during the aggregation period. If this is a cross-entity aggregate then this is the minimum value encountered across all entities. If this is a cross-time, cross-entity aggregate, then this is the minimum value for any entity across the aggregation period.
   * @return min
  **/
  @ApiModelProperty(value = "This minimum value encountered while producing this aggregate data point. If this is a cross-time aggregate then this is the minimum value encountered during the aggregation period. If this is a cross-entity aggregate then this is the minimum value encountered across all entities. If this is a cross-time, cross-entity aggregate, then this is the minimum value for any entity across the aggregation period.")

  @Valid

  public BigDecimal getMin() {
    return min;
  }

  public void setMin(BigDecimal min) {
    this.min = min;
  }

  public ApiTimeSeriesAggregateStatistics minTime(String minTime) {
    this.minTime = minTime;
    return this;
  }

  /**
   * The timestamp of the minimum data point. Note that the timestamp reflects coordinated universal time (UTC) and not necessarily the server's time zone. The rest API formats the UTC timestamp as an ISO-8061 string.
   * @return minTime
  **/
  @ApiModelProperty(value = "The timestamp of the minimum data point. Note that the timestamp reflects coordinated universal time (UTC) and not necessarily the server's time zone. The rest API formats the UTC timestamp as an ISO-8061 string.")


  public String getMinTime() {
    return minTime;
  }

  public void setMinTime(String minTime) {
    this.minTime = minTime;
  }

  public ApiTimeSeriesAggregateStatistics max(BigDecimal max) {
    this.max = max;
    return this;
  }

  /**
   * This maximum value encountered while producing this aggregate data point. If this is a cross-time aggregate then this is the maximum value encountered during the aggregation period. If this is a cross-entity aggregate then this is the maximum value encountered across all entities. If this is a cross-time, cross-entity aggregate, then this is the maximum value for any entity across the aggregation period.
   * @return max
  **/
  @ApiModelProperty(value = "This maximum value encountered while producing this aggregate data point. If this is a cross-time aggregate then this is the maximum value encountered during the aggregation period. If this is a cross-entity aggregate then this is the maximum value encountered across all entities. If this is a cross-time, cross-entity aggregate, then this is the maximum value for any entity across the aggregation period.")

  @Valid

  public BigDecimal getMax() {
    return max;
  }

  public void setMax(BigDecimal max) {
    this.max = max;
  }

  public ApiTimeSeriesAggregateStatistics maxTime(String maxTime) {
    this.maxTime = maxTime;
    return this;
  }

  /**
   * The timestamp of the maximum data point. Note that the timestamp reflects coordinated universal time (UTC) and not necessarily the server's time zone. The rest API formats the UTC timestamp as an ISO-8061 string.
   * @return maxTime
  **/
  @ApiModelProperty(value = "The timestamp of the maximum data point. Note that the timestamp reflects coordinated universal time (UTC) and not necessarily the server's time zone. The rest API formats the UTC timestamp as an ISO-8061 string.")


  public String getMaxTime() {
    return maxTime;
  }

  public void setMaxTime(String maxTime) {
    this.maxTime = maxTime;
  }

  public ApiTimeSeriesAggregateStatistics mean(BigDecimal mean) {
    this.mean = mean;
    return this;
  }

  /**
   * The mean of the values of all data-points for this aggregate data point.
   * @return mean
  **/
  @ApiModelProperty(value = "The mean of the values of all data-points for this aggregate data point.")

  @Valid

  public BigDecimal getMean() {
    return mean;
  }

  public void setMean(BigDecimal mean) {
    this.mean = mean;
  }

  public ApiTimeSeriesAggregateStatistics stdDev(BigDecimal stdDev) {
    this.stdDev = stdDev;
    return this;
  }

  /**
   * The standard deviation of the values of all data-points for this aggregate data point.
   * @return stdDev
  **/
  @ApiModelProperty(value = "The standard deviation of the values of all data-points for this aggregate data point.")

  @Valid

  public BigDecimal getStdDev() {
    return stdDev;
  }

  public void setStdDev(BigDecimal stdDev) {
    this.stdDev = stdDev;
  }

  public ApiTimeSeriesAggregateStatistics crossEntityMetadata(ApiTimeSeriesCrossEntityMetadata crossEntityMetadata) {
    this.crossEntityMetadata = crossEntityMetadata;
    return this;
  }

  /**
   * If the data-point is for a cross entity aggregate (e.g., fd_open_across_datanodes) returns the cross entity metadata, null otherwise.
   * @return crossEntityMetadata
  **/
  @ApiModelProperty(value = "If the data-point is for a cross entity aggregate (e.g., fd_open_across_datanodes) returns the cross entity metadata, null otherwise.")

  @Valid

  public ApiTimeSeriesCrossEntityMetadata getCrossEntityMetadata() {
    return crossEntityMetadata;
  }

  public void setCrossEntityMetadata(ApiTimeSeriesCrossEntityMetadata crossEntityMetadata) {
    this.crossEntityMetadata = crossEntityMetadata;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTimeSeriesAggregateStatistics apiTimeSeriesAggregateStatistics = (ApiTimeSeriesAggregateStatistics) o;
    return Objects.equals(this.sampleTime, apiTimeSeriesAggregateStatistics.sampleTime) &&
        Objects.equals(this.sampleValue, apiTimeSeriesAggregateStatistics.sampleValue) &&
        Objects.equals(this.count, apiTimeSeriesAggregateStatistics.count) &&
        Objects.equals(this.min, apiTimeSeriesAggregateStatistics.min) &&
        Objects.equals(this.minTime, apiTimeSeriesAggregateStatistics.minTime) &&
        Objects.equals(this.max, apiTimeSeriesAggregateStatistics.max) &&
        Objects.equals(this.maxTime, apiTimeSeriesAggregateStatistics.maxTime) &&
        Objects.equals(this.mean, apiTimeSeriesAggregateStatistics.mean) &&
        Objects.equals(this.stdDev, apiTimeSeriesAggregateStatistics.stdDev) &&
        Objects.equals(this.crossEntityMetadata, apiTimeSeriesAggregateStatistics.crossEntityMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sampleTime, sampleValue, count, min, minTime, max, maxTime, mean, stdDev, crossEntityMetadata);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTimeSeriesAggregateStatistics {\n");
    
    sb.append("    sampleTime: ").append(toIndentedString(sampleTime)).append("\n");
    sb.append("    sampleValue: ").append(toIndentedString(sampleValue)).append("\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
    sb.append("    min: ").append(toIndentedString(min)).append("\n");
    sb.append("    minTime: ").append(toIndentedString(minTime)).append("\n");
    sb.append("    max: ").append(toIndentedString(max)).append("\n");
    sb.append("    maxTime: ").append(toIndentedString(maxTime)).append("\n");
    sb.append("    mean: ").append(toIndentedString(mean)).append("\n");
    sb.append("    stdDev: ").append(toIndentedString(stdDev)).append("\n");
    sb.append("    crossEntityMetadata: ").append(toIndentedString(crossEntityMetadata)).append("\n");
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

