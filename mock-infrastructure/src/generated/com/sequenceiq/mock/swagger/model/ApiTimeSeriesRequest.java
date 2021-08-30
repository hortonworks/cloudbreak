package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Request object containing information needed for querying timeseries data.  Available since API v11.
 */
@ApiModel(description = "Request object containing information needed for querying timeseries data.  Available since API v11.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTimeSeriesRequest   {
  @JsonProperty("query")
  private String query = null;

  @JsonProperty("from")
  private String from = null;

  @JsonProperty("to")
  private String to = null;

  @JsonProperty("contentType")
  private String contentType = null;

  @JsonProperty("desiredRollup")
  private String desiredRollup = null;

  @JsonProperty("mustUseDesiredRollup")
  private Boolean mustUseDesiredRollup = null;

  public ApiTimeSeriesRequest query(String query) {
    this.query = query;
    return this;
  }

  /**
   * tsquery to run against the CM time-series data store. Please see the <a href=\"https://docs.cloudera.com/r/cm_tsquery\"> tsquery language documentation</a>.<p/>
   * @return query
  **/
  @ApiModelProperty(value = "tsquery to run against the CM time-series data store. Please see the <a href=\"https://docs.cloudera.com/r/cm_tsquery\"> tsquery language documentation</a>.<p/>")


  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public ApiTimeSeriesRequest from(String from) {
    this.from = from;
    return this;
  }

  /**
   * Start of the period to query in ISO 8601 format (defaults to 5 minutes before the end of the period).
   * @return from
  **/
  @ApiModelProperty(value = "Start of the period to query in ISO 8601 format (defaults to 5 minutes before the end of the period).")


  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public ApiTimeSeriesRequest to(String to) {
    this.to = to;
    return this;
  }

  /**
   * End of the period to query in ISO 8601 format (defaults to current time).
   * @return to
  **/
  @ApiModelProperty(value = "End of the period to query in ISO 8601 format (defaults to current time).")


  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public ApiTimeSeriesRequest contentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  /**
   * contentType to return the response in. The content types \"application/json\" and \"text/csv\" are supported. This defaults to \"application/json\". If \"text/csv\" is specified then we return one row per time series data point, and we don't return any of the metadata.
   * @return contentType
  **/
  @ApiModelProperty(value = "contentType to return the response in. The content types \"application/json\" and \"text/csv\" are supported. This defaults to \"application/json\". If \"text/csv\" is specified then we return one row per time series data point, and we don't return any of the metadata.")


  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public ApiTimeSeriesRequest desiredRollup(String desiredRollup) {
    this.desiredRollup = desiredRollup;
    return this;
  }

  /**
   * Aggregate rollup level desired for the response data. Valid values are RAW, TEN_MINUTELY, HOURLY, SIX_HOURLY, DAILY, and WEEKLY. Note that if the mustUseDesiredRollup parameter is not set, then the monitoring server can decide to return a different rollup level.
   * @return desiredRollup
  **/
  @ApiModelProperty(value = "Aggregate rollup level desired for the response data. Valid values are RAW, TEN_MINUTELY, HOURLY, SIX_HOURLY, DAILY, and WEEKLY. Note that if the mustUseDesiredRollup parameter is not set, then the monitoring server can decide to return a different rollup level.")


  public String getDesiredRollup() {
    return desiredRollup;
  }

  public void setDesiredRollup(String desiredRollup) {
    this.desiredRollup = desiredRollup;
  }

  public ApiTimeSeriesRequest mustUseDesiredRollup(Boolean mustUseDesiredRollup) {
    this.mustUseDesiredRollup = mustUseDesiredRollup;
    return this;
  }

  /**
   * If set to true, then the tsquery will return data with the desired aggregate rollup level.
   * @return mustUseDesiredRollup
  **/
  @ApiModelProperty(value = "If set to true, then the tsquery will return data with the desired aggregate rollup level.")


  public Boolean isMustUseDesiredRollup() {
    return mustUseDesiredRollup;
  }

  public void setMustUseDesiredRollup(Boolean mustUseDesiredRollup) {
    this.mustUseDesiredRollup = mustUseDesiredRollup;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTimeSeriesRequest apiTimeSeriesRequest = (ApiTimeSeriesRequest) o;
    return Objects.equals(this.query, apiTimeSeriesRequest.query) &&
        Objects.equals(this.from, apiTimeSeriesRequest.from) &&
        Objects.equals(this.to, apiTimeSeriesRequest.to) &&
        Objects.equals(this.contentType, apiTimeSeriesRequest.contentType) &&
        Objects.equals(this.desiredRollup, apiTimeSeriesRequest.desiredRollup) &&
        Objects.equals(this.mustUseDesiredRollup, apiTimeSeriesRequest.mustUseDesiredRollup);
  }

  @Override
  public int hashCode() {
    return Objects.hash(query, from, to, contentType, desiredRollup, mustUseDesiredRollup);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTimeSeriesRequest {\n");
    
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
    sb.append("    desiredRollup: ").append(toIndentedString(desiredRollup)).append("\n");
    sb.append("    mustUseDesiredRollup: ").append(toIndentedString(mustUseDesiredRollup)).append("\n");
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

