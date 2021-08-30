package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiTimeSeriesData;
import com.sequenceiq.mock.swagger.model.ApiTimeSeriesMetadata;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A time series represents a stream of data points. Each data point contains a time and a value. Time series are returned by executing a tsquery.
 */
@ApiModel(description = "A time series represents a stream of data points. Each data point contains a time and a value. Time series are returned by executing a tsquery.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTimeSeries   {
  @JsonProperty("metadata")
  private ApiTimeSeriesMetadata metadata = null;

  @JsonProperty("data")
  @Valid
  private List<ApiTimeSeriesData> data = null;

  public ApiTimeSeries metadata(ApiTimeSeriesMetadata metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Metadata for the metric.
   * @return metadata
  **/
  @ApiModelProperty(value = "Metadata for the metric.")

  @Valid

  public ApiTimeSeriesMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ApiTimeSeriesMetadata metadata) {
    this.metadata = metadata;
  }

  public ApiTimeSeries data(List<ApiTimeSeriesData> data) {
    this.data = data;
    return this;
  }

  public ApiTimeSeries addDataItem(ApiTimeSeriesData dataItem) {
    if (this.data == null) {
      this.data = new ArrayList<>();
    }
    this.data.add(dataItem);
    return this;
  }

  /**
   * List of metric data points.
   * @return data
  **/
  @ApiModelProperty(value = "List of metric data points.")

  @Valid

  public List<ApiTimeSeriesData> getData() {
    return data;
  }

  public void setData(List<ApiTimeSeriesData> data) {
    this.data = data;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTimeSeries apiTimeSeries = (ApiTimeSeries) o;
    return Objects.equals(this.metadata, apiTimeSeries.metadata) &&
        Objects.equals(this.data, apiTimeSeries.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata, data);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTimeSeries {\n");
    
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    data: ").append(toIndentedString(data)).append("\n");
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

