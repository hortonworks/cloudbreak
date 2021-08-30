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
 * Histogram bin of Impala utilization.
 */
@ApiModel(description = "Histogram bin of Impala utilization.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiImpalaUtilizationHistogramBin   {
  @JsonProperty("startPointInclusive")
  private BigDecimal startPointInclusive = null;

  @JsonProperty("endPointExclusive")
  private BigDecimal endPointExclusive = null;

  @JsonProperty("numberOfImpalaDaemons")
  private Integer numberOfImpalaDaemons = null;

  public ApiImpalaUtilizationHistogramBin startPointInclusive(BigDecimal startPointInclusive) {
    this.startPointInclusive = startPointInclusive;
    return this;
  }

  /**
   * start point (inclusive) of the histogram bin.
   * @return startPointInclusive
  **/
  @ApiModelProperty(value = "start point (inclusive) of the histogram bin.")

  @Valid

  public BigDecimal getStartPointInclusive() {
    return startPointInclusive;
  }

  public void setStartPointInclusive(BigDecimal startPointInclusive) {
    this.startPointInclusive = startPointInclusive;
  }

  public ApiImpalaUtilizationHistogramBin endPointExclusive(BigDecimal endPointExclusive) {
    this.endPointExclusive = endPointExclusive;
    return this;
  }

  /**
   * end point (exclusive) of the histogram bin.
   * @return endPointExclusive
  **/
  @ApiModelProperty(value = "end point (exclusive) of the histogram bin.")

  @Valid

  public BigDecimal getEndPointExclusive() {
    return endPointExclusive;
  }

  public void setEndPointExclusive(BigDecimal endPointExclusive) {
    this.endPointExclusive = endPointExclusive;
  }

  public ApiImpalaUtilizationHistogramBin numberOfImpalaDaemons(Integer numberOfImpalaDaemons) {
    this.numberOfImpalaDaemons = numberOfImpalaDaemons;
    return this;
  }

  /**
   * Number of Impala daemons.
   * @return numberOfImpalaDaemons
  **/
  @ApiModelProperty(value = "Number of Impala daemons.")


  public Integer getNumberOfImpalaDaemons() {
    return numberOfImpalaDaemons;
  }

  public void setNumberOfImpalaDaemons(Integer numberOfImpalaDaemons) {
    this.numberOfImpalaDaemons = numberOfImpalaDaemons;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiImpalaUtilizationHistogramBin apiImpalaUtilizationHistogramBin = (ApiImpalaUtilizationHistogramBin) o;
    return Objects.equals(this.startPointInclusive, apiImpalaUtilizationHistogramBin.startPointInclusive) &&
        Objects.equals(this.endPointExclusive, apiImpalaUtilizationHistogramBin.endPointExclusive) &&
        Objects.equals(this.numberOfImpalaDaemons, apiImpalaUtilizationHistogramBin.numberOfImpalaDaemons);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startPointInclusive, endPointExclusive, numberOfImpalaDaemons);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaUtilizationHistogramBin {\n");
    
    sb.append("    startPointInclusive: ").append(toIndentedString(startPointInclusive)).append("\n");
    sb.append("    endPointExclusive: ").append(toIndentedString(endPointExclusive)).append("\n");
    sb.append("    numberOfImpalaDaemons: ").append(toIndentedString(numberOfImpalaDaemons)).append("\n");
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

