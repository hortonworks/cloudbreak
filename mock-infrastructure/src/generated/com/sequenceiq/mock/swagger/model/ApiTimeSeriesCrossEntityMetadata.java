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
 * A class holding additional metadata to the ApiTimeSeriesAggregateStatistics class that applies specifically to cross-entity aggregate metrics.
 */
@ApiModel(description = "A class holding additional metadata to the ApiTimeSeriesAggregateStatistics class that applies specifically to cross-entity aggregate metrics.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTimeSeriesCrossEntityMetadata   {
  @JsonProperty("maxEntityDisplayName")
  private String maxEntityDisplayName = null;

  @JsonProperty("maxEntityName")
  private String maxEntityName = null;

  @JsonProperty("minEntityDisplayName")
  private String minEntityDisplayName = null;

  @JsonProperty("minEntityName")
  private String minEntityName = null;

  @JsonProperty("numEntities")
  private BigDecimal numEntities = null;

  public ApiTimeSeriesCrossEntityMetadata maxEntityDisplayName(String maxEntityDisplayName) {
    this.maxEntityDisplayName = maxEntityDisplayName;
    return this;
  }

  /**
   * The display name of the entity that had the maximum value for the cross-entity aggregate metric.
   * @return maxEntityDisplayName
  **/
  @ApiModelProperty(value = "The display name of the entity that had the maximum value for the cross-entity aggregate metric.")


  public String getMaxEntityDisplayName() {
    return maxEntityDisplayName;
  }

  public void setMaxEntityDisplayName(String maxEntityDisplayName) {
    this.maxEntityDisplayName = maxEntityDisplayName;
  }

  public ApiTimeSeriesCrossEntityMetadata maxEntityName(String maxEntityName) {
    this.maxEntityName = maxEntityName;
    return this;
  }

  /**
   * The name of the entity that had the maximum value for the cross-entity aggregate metric. <p> Available since API v11.
   * @return maxEntityName
  **/
  @ApiModelProperty(value = "The name of the entity that had the maximum value for the cross-entity aggregate metric. <p> Available since API v11.")


  public String getMaxEntityName() {
    return maxEntityName;
  }

  public void setMaxEntityName(String maxEntityName) {
    this.maxEntityName = maxEntityName;
  }

  public ApiTimeSeriesCrossEntityMetadata minEntityDisplayName(String minEntityDisplayName) {
    this.minEntityDisplayName = minEntityDisplayName;
    return this;
  }

  /**
   * The display name of the entity that had the minimum value for the cross-entity aggregate metric.
   * @return minEntityDisplayName
  **/
  @ApiModelProperty(value = "The display name of the entity that had the minimum value for the cross-entity aggregate metric.")


  public String getMinEntityDisplayName() {
    return minEntityDisplayName;
  }

  public void setMinEntityDisplayName(String minEntityDisplayName) {
    this.minEntityDisplayName = minEntityDisplayName;
  }

  public ApiTimeSeriesCrossEntityMetadata minEntityName(String minEntityName) {
    this.minEntityName = minEntityName;
    return this;
  }

  /**
   * The name of the entity that had the minimum value for the cross-entity aggregate metric. <p> Available since API v11.
   * @return minEntityName
  **/
  @ApiModelProperty(value = "The name of the entity that had the minimum value for the cross-entity aggregate metric. <p> Available since API v11.")


  public String getMinEntityName() {
    return minEntityName;
  }

  public void setMinEntityName(String minEntityName) {
    this.minEntityName = minEntityName;
  }

  public ApiTimeSeriesCrossEntityMetadata numEntities(BigDecimal numEntities) {
    this.numEntities = numEntities;
    return this;
  }

  /**
   * The number of entities covered by this point. For a raw cross-entity point this number is exact. For a rollup point this number is an average, since the number of entities being aggregated can change over the aggregation period.
   * @return numEntities
  **/
  @ApiModelProperty(required = true, value = "The number of entities covered by this point. For a raw cross-entity point this number is exact. For a rollup point this number is an average, since the number of entities being aggregated can change over the aggregation period.")
  @NotNull

  @Valid

  public BigDecimal getNumEntities() {
    return numEntities;
  }

  public void setNumEntities(BigDecimal numEntities) {
    this.numEntities = numEntities;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTimeSeriesCrossEntityMetadata apiTimeSeriesCrossEntityMetadata = (ApiTimeSeriesCrossEntityMetadata) o;
    return Objects.equals(this.maxEntityDisplayName, apiTimeSeriesCrossEntityMetadata.maxEntityDisplayName) &&
        Objects.equals(this.maxEntityName, apiTimeSeriesCrossEntityMetadata.maxEntityName) &&
        Objects.equals(this.minEntityDisplayName, apiTimeSeriesCrossEntityMetadata.minEntityDisplayName) &&
        Objects.equals(this.minEntityName, apiTimeSeriesCrossEntityMetadata.minEntityName) &&
        Objects.equals(this.numEntities, apiTimeSeriesCrossEntityMetadata.numEntities);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxEntityDisplayName, maxEntityName, minEntityDisplayName, minEntityName, numEntities);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTimeSeriesCrossEntityMetadata {\n");
    
    sb.append("    maxEntityDisplayName: ").append(toIndentedString(maxEntityDisplayName)).append("\n");
    sb.append("    maxEntityName: ").append(toIndentedString(maxEntityName)).append("\n");
    sb.append("    minEntityDisplayName: ").append(toIndentedString(minEntityDisplayName)).append("\n");
    sb.append("    minEntityName: ").append(toIndentedString(minEntityName)).append("\n");
    sb.append("    numEntities: ").append(toIndentedString(numEntities)).append("\n");
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

