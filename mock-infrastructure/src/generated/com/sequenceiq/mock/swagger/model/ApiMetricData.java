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
 * A single data point of metric data.
 */
@ApiModel(description = "A single data point of metric data.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiMetricData   {
  @JsonProperty("timestamp")
  private String timestamp = null;

  @JsonProperty("value")
  private BigDecimal value = null;

  public ApiMetricData timestamp(String timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * When the metric reading was collected.
   * @return timestamp
  **/
  @ApiModelProperty(value = "When the metric reading was collected.")


  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public ApiMetricData value(BigDecimal value) {
    this.value = value;
    return this;
  }

  /**
   * The value of the metric.
   * @return value
  **/
  @ApiModelProperty(value = "The value of the metric.")

  @Valid

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiMetricData apiMetricData = (ApiMetricData) o;
    return Objects.equals(this.timestamp, apiMetricData.timestamp) &&
        Objects.equals(this.value, apiMetricData.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiMetricData {\n");
    
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

