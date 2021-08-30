package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiTimeSeries;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * The time series response for a time series query.
 */
@ApiModel(description = "The time series response for a time series query.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTimeSeriesResponse   {
  @JsonProperty("timeSeries")
  @Valid
  private List<ApiTimeSeries> timeSeries = null;

  @JsonProperty("warnings")
  @Valid
  private List<String> warnings = null;

  @JsonProperty("timeSeriesQuery")
  private String timeSeriesQuery = null;

  public ApiTimeSeriesResponse timeSeries(List<ApiTimeSeries> timeSeries) {
    this.timeSeries = timeSeries;
    return this;
  }

  public ApiTimeSeriesResponse addTimeSeriesItem(ApiTimeSeries timeSeriesItem) {
    if (this.timeSeries == null) {
      this.timeSeries = new ArrayList<>();
    }
    this.timeSeries.add(timeSeriesItem);
    return this;
  }

  /**
   * The time series data for this single query response.
   * @return timeSeries
  **/
  @ApiModelProperty(value = "The time series data for this single query response.")

  @Valid

  public List<ApiTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  public void setTimeSeries(List<ApiTimeSeries> timeSeries) {
    this.timeSeries = timeSeries;
  }

  public ApiTimeSeriesResponse warnings(List<String> warnings) {
    this.warnings = warnings;
    return this;
  }

  public ApiTimeSeriesResponse addWarningsItem(String warningsItem) {
    if (this.warnings == null) {
      this.warnings = new ArrayList<>();
    }
    this.warnings.add(warningsItem);
    return this;
  }

  /**
   * The warnings for this single query response.
   * @return warnings
  **/
  @ApiModelProperty(value = "The warnings for this single query response.")


  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }

  public ApiTimeSeriesResponse timeSeriesQuery(String timeSeriesQuery) {
    this.timeSeriesQuery = timeSeriesQuery;
    return this;
  }

  /**
   * The query for this single query response.
   * @return timeSeriesQuery
  **/
  @ApiModelProperty(value = "The query for this single query response.")


  public String getTimeSeriesQuery() {
    return timeSeriesQuery;
  }

  public void setTimeSeriesQuery(String timeSeriesQuery) {
    this.timeSeriesQuery = timeSeriesQuery;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTimeSeriesResponse apiTimeSeriesResponse = (ApiTimeSeriesResponse) o;
    return Objects.equals(this.timeSeries, apiTimeSeriesResponse.timeSeries) &&
        Objects.equals(this.warnings, apiTimeSeriesResponse.warnings) &&
        Objects.equals(this.timeSeriesQuery, apiTimeSeriesResponse.timeSeriesQuery);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timeSeries, warnings, timeSeriesQuery);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTimeSeriesResponse {\n");
    
    sb.append("    timeSeries: ").append(toIndentedString(timeSeries)).append("\n");
    sb.append("    warnings: ").append(toIndentedString(warnings)).append("\n");
    sb.append("    timeSeriesQuery: ").append(toIndentedString(timeSeriesQuery)).append("\n");
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

