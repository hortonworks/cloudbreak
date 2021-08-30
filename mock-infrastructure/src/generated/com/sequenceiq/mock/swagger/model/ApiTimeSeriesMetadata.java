package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Metadata for a time series.
 */
@ApiModel(description = "Metadata for a time series.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTimeSeriesMetadata   {
  @JsonProperty("metricName")
  private String metricName = null;

  @JsonProperty("entityName")
  private String entityName = null;

  @JsonProperty("startTime")
  private String startTime = null;

  @JsonProperty("endTime")
  private String endTime = null;

  @JsonProperty("attributes")
  @Valid
  private Map<String, String> attributes = null;

  @JsonProperty("unitNumerators")
  @Valid
  private List<String> unitNumerators = null;

  @JsonProperty("unitDenominators")
  @Valid
  private List<String> unitDenominators = null;

  @JsonProperty("expression")
  private String expression = null;

  @JsonProperty("alias")
  private String alias = null;

  @JsonProperty("metricCollectionFrequencyMs")
  private Integer metricCollectionFrequencyMs = null;

  @JsonProperty("rollupUsed")
  private String rollupUsed = null;

  public ApiTimeSeriesMetadata metricName(String metricName) {
    this.metricName = metricName;
    return this;
  }

  /**
   * The metric name for the time series.
   * @return metricName
  **/
  @ApiModelProperty(value = "The metric name for the time series.")


  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  public ApiTimeSeriesMetadata entityName(String entityName) {
    this.entityName = entityName;
    return this;
  }

  /**
   * The display name for the entity associated with this time series. For example, if this was a time series for an HDFS service the entity name might be something like \"My HDFS Service\". If it was for a host it might be something like \"myhost.mysite.com\".
   * @return entityName
  **/
  @ApiModelProperty(value = "The display name for the entity associated with this time series. For example, if this was a time series for an HDFS service the entity name might be something like \"My HDFS Service\". If it was for a host it might be something like \"myhost.mysite.com\".")


  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public ApiTimeSeriesMetadata startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * The start time for the time series.
   * @return startTime
  **/
  @ApiModelProperty(value = "The start time for the time series.")


  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public ApiTimeSeriesMetadata endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * The end time for the time series.
   * @return endTime
  **/
  @ApiModelProperty(value = "The end time for the time series.")


  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public ApiTimeSeriesMetadata attributes(Map<String, String> attributes) {
    this.attributes = attributes;
    return this;
  }

  public ApiTimeSeriesMetadata putAttributesItem(String key, String attributesItem) {
    if (this.attributes == null) {
      this.attributes = new HashMap<>();
    }
    this.attributes.put(key, attributesItem);
    return this;
  }

  /**
   * The attributes for the time series. Note that the entityName entry in this map is not the same as the entityName field in this ApiTimeSeriesMetadata. The entityName entry in this map is a unique identifier for the entity and not the name displayed in the UI.  For example, if this was a time series for the YARN Job History Server the entityName entry in this map might be something like \"yarn-JOBHISTORY-6bd17ceb1489aae93fef4c867350d0dd\"
   * @return attributes
  **/
  @ApiModelProperty(value = "The attributes for the time series. Note that the entityName entry in this map is not the same as the entityName field in this ApiTimeSeriesMetadata. The entityName entry in this map is a unique identifier for the entity and not the name displayed in the UI.  For example, if this was a time series for the YARN Job History Server the entityName entry in this map might be something like \"yarn-JOBHISTORY-6bd17ceb1489aae93fef4c867350d0dd\"")


  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public ApiTimeSeriesMetadata unitNumerators(List<String> unitNumerators) {
    this.unitNumerators = unitNumerators;
    return this;
  }

  public ApiTimeSeriesMetadata addUnitNumeratorsItem(String unitNumeratorsItem) {
    if (this.unitNumerators == null) {
      this.unitNumerators = new ArrayList<>();
    }
    this.unitNumerators.add(unitNumeratorsItem);
    return this;
  }

  /**
   * The numerators of the units for the time series.
   * @return unitNumerators
  **/
  @ApiModelProperty(value = "The numerators of the units for the time series.")


  public List<String> getUnitNumerators() {
    return unitNumerators;
  }

  public void setUnitNumerators(List<String> unitNumerators) {
    this.unitNumerators = unitNumerators;
  }

  public ApiTimeSeriesMetadata unitDenominators(List<String> unitDenominators) {
    this.unitDenominators = unitDenominators;
    return this;
  }

  public ApiTimeSeriesMetadata addUnitDenominatorsItem(String unitDenominatorsItem) {
    if (this.unitDenominators == null) {
      this.unitDenominators = new ArrayList<>();
    }
    this.unitDenominators.add(unitDenominatorsItem);
    return this;
  }

  /**
   * The denominators of the units for the time series.
   * @return unitDenominators
  **/
  @ApiModelProperty(value = "The denominators of the units for the time series.")


  public List<String> getUnitDenominators() {
    return unitDenominators;
  }

  public void setUnitDenominators(List<String> unitDenominators) {
    this.unitDenominators = unitDenominators;
  }

  public ApiTimeSeriesMetadata expression(String expression) {
    this.expression = expression;
    return this;
  }

  /**
   * The tsquery expression that could be used to extract just this stream.
   * @return expression
  **/
  @ApiModelProperty(value = "The tsquery expression that could be used to extract just this stream.")


  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public ApiTimeSeriesMetadata alias(String alias) {
    this.alias = alias;
    return this;
  }

  /**
   * The alias for this stream's metric. Aliases correspond to use of the 'as' keyword in the tsquery.
   * @return alias
  **/
  @ApiModelProperty(value = "The alias for this stream's metric. Aliases correspond to use of the 'as' keyword in the tsquery.")


  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public ApiTimeSeriesMetadata metricCollectionFrequencyMs(Integer metricCollectionFrequencyMs) {
    this.metricCollectionFrequencyMs = metricCollectionFrequencyMs;
    return this;
  }

  /**
   * The minimum frequency at which the underlying metric for this stream is collected. Note that this can be null if the stream returns irregularly sampled data.
   * @return metricCollectionFrequencyMs
  **/
  @ApiModelProperty(value = "The minimum frequency at which the underlying metric for this stream is collected. Note that this can be null if the stream returns irregularly sampled data.")


  public Integer getMetricCollectionFrequencyMs() {
    return metricCollectionFrequencyMs;
  }

  public void setMetricCollectionFrequencyMs(Integer metricCollectionFrequencyMs) {
    this.metricCollectionFrequencyMs = metricCollectionFrequencyMs;
  }

  public ApiTimeSeriesMetadata rollupUsed(String rollupUsed) {
    this.rollupUsed = rollupUsed;
    return this;
  }

  /**
   * The aggregate rollup for the returned data. This can be TEN_MINUTELY, HOURLY, SIX_HOURLY, DAILY, or WEEKLY.
   * @return rollupUsed
  **/
  @ApiModelProperty(value = "The aggregate rollup for the returned data. This can be TEN_MINUTELY, HOURLY, SIX_HOURLY, DAILY, or WEEKLY.")


  public String getRollupUsed() {
    return rollupUsed;
  }

  public void setRollupUsed(String rollupUsed) {
    this.rollupUsed = rollupUsed;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTimeSeriesMetadata apiTimeSeriesMetadata = (ApiTimeSeriesMetadata) o;
    return Objects.equals(this.metricName, apiTimeSeriesMetadata.metricName) &&
        Objects.equals(this.entityName, apiTimeSeriesMetadata.entityName) &&
        Objects.equals(this.startTime, apiTimeSeriesMetadata.startTime) &&
        Objects.equals(this.endTime, apiTimeSeriesMetadata.endTime) &&
        Objects.equals(this.attributes, apiTimeSeriesMetadata.attributes) &&
        Objects.equals(this.unitNumerators, apiTimeSeriesMetadata.unitNumerators) &&
        Objects.equals(this.unitDenominators, apiTimeSeriesMetadata.unitDenominators) &&
        Objects.equals(this.expression, apiTimeSeriesMetadata.expression) &&
        Objects.equals(this.alias, apiTimeSeriesMetadata.alias) &&
        Objects.equals(this.metricCollectionFrequencyMs, apiTimeSeriesMetadata.metricCollectionFrequencyMs) &&
        Objects.equals(this.rollupUsed, apiTimeSeriesMetadata.rollupUsed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metricName, entityName, startTime, endTime, attributes, unitNumerators, unitDenominators, expression, alias, metricCollectionFrequencyMs, rollupUsed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTimeSeriesMetadata {\n");
    
    sb.append("    metricName: ").append(toIndentedString(metricName)).append("\n");
    sb.append("    entityName: ").append(toIndentedString(entityName)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
    sb.append("    unitNumerators: ").append(toIndentedString(unitNumerators)).append("\n");
    sb.append("    unitDenominators: ").append(toIndentedString(unitDenominators)).append("\n");
    sb.append("    expression: ").append(toIndentedString(expression)).append("\n");
    sb.append("    alias: ").append(toIndentedString(alias)).append("\n");
    sb.append("    metricCollectionFrequencyMs: ").append(toIndentedString(metricCollectionFrequencyMs)).append("\n");
    sb.append("    rollupUsed: ").append(toIndentedString(rollupUsed)).append("\n");
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

