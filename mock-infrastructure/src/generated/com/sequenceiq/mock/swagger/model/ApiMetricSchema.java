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
 * A metric schema represents the schema for a specific metric monitored by the Cloudera Management Services.
 */
@ApiModel(description = "A metric schema represents the schema for a specific metric monitored by the Cloudera Management Services.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiMetricSchema   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("isCounter")
  private Boolean isCounter = null;

  @JsonProperty("unitNumerator")
  private String unitNumerator = null;

  @JsonProperty("unitDenominator")
  private String unitDenominator = null;

  @JsonProperty("aliases")
  @Valid
  private List<String> aliases = null;

  @JsonProperty("sources")
  @Valid
  private Map<String, List<String>> sources = null;

  public ApiMetricSchema name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Name of the metric. This name is guaranteed to be unique among the metrics.
   * @return name
  **/
  @ApiModelProperty(value = "Name of the metric. This name is guaranteed to be unique among the metrics.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiMetricSchema displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Display name of the metric.
   * @return displayName
  **/
  @ApiModelProperty(value = "Display name of the metric.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiMetricSchema description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Description of the metric.
   * @return description
  **/
  @ApiModelProperty(value = "Description of the metric.")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ApiMetricSchema isCounter(Boolean isCounter) {
    this.isCounter = isCounter;
    return this;
  }

  /**
   * Is the metric a counter. A counter tracks the total count since a process / host started. The rate of change of a counter may often be more interesting than the raw value of a counter.
   * @return isCounter
  **/
  @ApiModelProperty(value = "Is the metric a counter. A counter tracks the total count since a process / host started. The rate of change of a counter may often be more interesting than the raw value of a counter.")


  public Boolean isIsCounter() {
    return isCounter;
  }

  public void setIsCounter(Boolean isCounter) {
    this.isCounter = isCounter;
  }

  public ApiMetricSchema unitNumerator(String unitNumerator) {
    this.unitNumerator = unitNumerator;
    return this;
  }

  /**
   * Numerator for the unit of the metric.
   * @return unitNumerator
  **/
  @ApiModelProperty(value = "Numerator for the unit of the metric.")


  public String getUnitNumerator() {
    return unitNumerator;
  }

  public void setUnitNumerator(String unitNumerator) {
    this.unitNumerator = unitNumerator;
  }

  public ApiMetricSchema unitDenominator(String unitDenominator) {
    this.unitDenominator = unitDenominator;
    return this;
  }

  /**
   * Denominator for the unit of the metric.
   * @return unitDenominator
  **/
  @ApiModelProperty(value = "Denominator for the unit of the metric.")


  public String getUnitDenominator() {
    return unitDenominator;
  }

  public void setUnitDenominator(String unitDenominator) {
    this.unitDenominator = unitDenominator;
  }

  public ApiMetricSchema aliases(List<String> aliases) {
    this.aliases = aliases;
    return this;
  }

  public ApiMetricSchema addAliasesItem(String aliasesItem) {
    if (this.aliases == null) {
      this.aliases = new ArrayList<>();
    }
    this.aliases.add(aliasesItem);
    return this;
  }

  /**
   * Aliases for the metric. An alias is unique per metric (per source and version) but is not globally unique. Aliases usually refer to previous names for the metric as metrics are renamed or replaced.
   * @return aliases
  **/
  @ApiModelProperty(value = "Aliases for the metric. An alias is unique per metric (per source and version) but is not globally unique. Aliases usually refer to previous names for the metric as metrics are renamed or replaced.")


  public List<String> getAliases() {
    return aliases;
  }

  public void setAliases(List<String> aliases) {
    this.aliases = aliases;
  }

  public ApiMetricSchema sources(Map<String, List<String>> sources) {
    this.sources = sources;
    return this;
  }

  public ApiMetricSchema putSourcesItem(String key, List<String> sourcesItem) {
    if (this.sources == null) {
      this.sources = new HashMap<>();
    }
    this.sources.put(key, sourcesItem);
    return this;
  }

  /**
   * Sources for the metric. Each source entry contains the name of the source and a list of versions for which this source is valid
   * @return sources
  **/
  @ApiModelProperty(value = "Sources for the metric. Each source entry contains the name of the source and a list of versions for which this source is valid")

  @Valid

  public Map<String, List<String>> getSources() {
    return sources;
  }

  public void setSources(Map<String, List<String>> sources) {
    this.sources = sources;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiMetricSchema apiMetricSchema = (ApiMetricSchema) o;
    return Objects.equals(this.name, apiMetricSchema.name) &&
        Objects.equals(this.displayName, apiMetricSchema.displayName) &&
        Objects.equals(this.description, apiMetricSchema.description) &&
        Objects.equals(this.isCounter, apiMetricSchema.isCounter) &&
        Objects.equals(this.unitNumerator, apiMetricSchema.unitNumerator) &&
        Objects.equals(this.unitDenominator, apiMetricSchema.unitDenominator) &&
        Objects.equals(this.aliases, apiMetricSchema.aliases) &&
        Objects.equals(this.sources, apiMetricSchema.sources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, displayName, description, isCounter, unitNumerator, unitDenominator, aliases, sources);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiMetricSchema {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    isCounter: ").append(toIndentedString(isCounter)).append("\n");
    sb.append("    unitNumerator: ").append(toIndentedString(unitNumerator)).append("\n");
    sb.append("    unitDenominator: ").append(toIndentedString(unitDenominator)).append("\n");
    sb.append("    aliases: ").append(toIndentedString(aliases)).append("\n");
    sb.append("    sources: ").append(toIndentedString(sources)).append("\n");
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

