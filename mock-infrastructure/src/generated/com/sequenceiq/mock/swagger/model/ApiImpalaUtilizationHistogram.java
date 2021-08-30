package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiImpalaUtilizationHistogramBinList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Histogram of Impala utilization.
 */
@ApiModel(description = "Histogram of Impala utilization.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiImpalaUtilizationHistogram   {
  @JsonProperty("bins")
  private ApiImpalaUtilizationHistogramBinList bins = null;

  public ApiImpalaUtilizationHistogram bins(ApiImpalaUtilizationHistogramBinList bins) {
    this.bins = bins;
    return this;
  }

  /**
   * Bins of the histogram.
   * @return bins
  **/
  @ApiModelProperty(value = "Bins of the histogram.")

  @Valid

  public ApiImpalaUtilizationHistogramBinList getBins() {
    return bins;
  }

  public void setBins(ApiImpalaUtilizationHistogramBinList bins) {
    this.bins = bins;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiImpalaUtilizationHistogram apiImpalaUtilizationHistogram = (ApiImpalaUtilizationHistogram) o;
    return Objects.equals(this.bins, apiImpalaUtilizationHistogram.bins);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bins);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaUtilizationHistogram {\n");
    
    sb.append("    bins: ").append(toIndentedString(bins)).append("\n");
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

