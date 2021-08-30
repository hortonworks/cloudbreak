package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiTimeSeriesAggregateStatistics;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A single data point of time series data.
 */
@ApiModel(description = "A single data point of time series data.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTimeSeriesData   {
  @JsonProperty("timestamp")
  private String timestamp = null;

  @JsonProperty("value")
  private BigDecimal value = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("aggregateStatistics")
  private ApiTimeSeriesAggregateStatistics aggregateStatistics = null;

  public ApiTimeSeriesData timestamp(String timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * The timestamp for this time series data point. Note that the timestamp reflects coordinated universal time (UTC) and not necessarily the server's time zone. The rest API formats the UTC timestamp as an ISO-8061 string.
   * @return timestamp
  **/
  @ApiModelProperty(value = "The timestamp for this time series data point. Note that the timestamp reflects coordinated universal time (UTC) and not necessarily the server's time zone. The rest API formats the UTC timestamp as an ISO-8061 string.")


  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public ApiTimeSeriesData value(BigDecimal value) {
    this.value = value;
    return this;
  }

  /**
   * The value of the time series data.
   * @return value
  **/
  @ApiModelProperty(value = "The value of the time series data.")

  @Valid

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public ApiTimeSeriesData type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the time series data.
   * @return type
  **/
  @ApiModelProperty(value = "The type of the time series data.")


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ApiTimeSeriesData aggregateStatistics(ApiTimeSeriesAggregateStatistics aggregateStatistics) {
    this.aggregateStatistics = aggregateStatistics;
    return this;
  }

  /**
   * Available from v6 for data points containing aggregate data. It includes further statistics about the data point. An aggregate can be across entities (e.g., fd_open_across_datanodes), over time (e.g., a daily point for the fd_open metric for a specific DataNode), or both (e.g., a daily point for the fd_open_across_datanodes metric). If the data point is for non-aggregate date this will return null.
   * @return aggregateStatistics
  **/
  @ApiModelProperty(value = "Available from v6 for data points containing aggregate data. It includes further statistics about the data point. An aggregate can be across entities (e.g., fd_open_across_datanodes), over time (e.g., a daily point for the fd_open metric for a specific DataNode), or both (e.g., a daily point for the fd_open_across_datanodes metric). If the data point is for non-aggregate date this will return null.")

  @Valid

  public ApiTimeSeriesAggregateStatistics getAggregateStatistics() {
    return aggregateStatistics;
  }

  public void setAggregateStatistics(ApiTimeSeriesAggregateStatistics aggregateStatistics) {
    this.aggregateStatistics = aggregateStatistics;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTimeSeriesData apiTimeSeriesData = (ApiTimeSeriesData) o;
    return Objects.equals(this.timestamp, apiTimeSeriesData.timestamp) &&
        Objects.equals(this.value, apiTimeSeriesData.value) &&
        Objects.equals(this.type, apiTimeSeriesData.type) &&
        Objects.equals(this.aggregateStatistics, apiTimeSeriesData.aggregateStatistics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, value, type, aggregateStatistics);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTimeSeriesData {\n");
    
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    aggregateStatistics: ").append(toIndentedString(aggregateStatistics)).append("\n");
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

