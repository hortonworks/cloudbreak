package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Describe a time series entity type and attributes associated with this entity type. &lt;p&gt; Available since API v11.
 */
@ApiModel(description = "Describe a time series entity type and attributes associated with this entity type. <p> Available since API v11.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTimeSeriesEntityType   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("category")
  private String category = null;

  @JsonProperty("nameForCrossEntityAggregateMetrics")
  private String nameForCrossEntityAggregateMetrics = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("immutableAttributeNames")
  @Valid
  private List<String> immutableAttributeNames = null;

  @JsonProperty("mutableAttributeNames")
  @Valid
  private List<String> mutableAttributeNames = null;

  @JsonProperty("entityNameFormat")
  @Valid
  private List<String> entityNameFormat = null;

  @JsonProperty("entityDisplayNameFormat")
  private String entityDisplayNameFormat = null;

  @JsonProperty("parentMetricEntityTypeNames")
  @Valid
  private List<String> parentMetricEntityTypeNames = null;

  public ApiTimeSeriesEntityType name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Returns the name of the entity type. This name uniquely identifies this entity type.
   * @return name
  **/
  @ApiModelProperty(value = "Returns the name of the entity type. This name uniquely identifies this entity type.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiTimeSeriesEntityType category(String category) {
    this.category = category;
    return this;
  }

  /**
   * Returns the category of the entity type.
   * @return category
  **/
  @ApiModelProperty(value = "Returns the category of the entity type.")


  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public ApiTimeSeriesEntityType nameForCrossEntityAggregateMetrics(String nameForCrossEntityAggregateMetrics) {
    this.nameForCrossEntityAggregateMetrics = nameForCrossEntityAggregateMetrics;
    return this;
  }

  /**
   * Returns the string to use to pluralize the name of the entity for cross entity aggregate metrics.
   * @return nameForCrossEntityAggregateMetrics
  **/
  @ApiModelProperty(value = "Returns the string to use to pluralize the name of the entity for cross entity aggregate metrics.")


  public String getNameForCrossEntityAggregateMetrics() {
    return nameForCrossEntityAggregateMetrics;
  }

  public void setNameForCrossEntityAggregateMetrics(String nameForCrossEntityAggregateMetrics) {
    this.nameForCrossEntityAggregateMetrics = nameForCrossEntityAggregateMetrics;
  }

  public ApiTimeSeriesEntityType displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Returns the display name of the entity type.
   * @return displayName
  **/
  @ApiModelProperty(value = "Returns the display name of the entity type.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiTimeSeriesEntityType description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Returns the description of the entity type.
   * @return description
  **/
  @ApiModelProperty(value = "Returns the description of the entity type.")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ApiTimeSeriesEntityType immutableAttributeNames(List<String> immutableAttributeNames) {
    this.immutableAttributeNames = immutableAttributeNames;
    return this;
  }

  public ApiTimeSeriesEntityType addImmutableAttributeNamesItem(String immutableAttributeNamesItem) {
    if (this.immutableAttributeNames == null) {
      this.immutableAttributeNames = new ArrayList<>();
    }
    this.immutableAttributeNames.add(immutableAttributeNamesItem);
    return this;
  }

  /**
   * Returns the list of immutable attributes for this entity type. Immutable attributes values for an entity may not change over its lifetime.
   * @return immutableAttributeNames
  **/
  @ApiModelProperty(value = "Returns the list of immutable attributes for this entity type. Immutable attributes values for an entity may not change over its lifetime.")


  public List<String> getImmutableAttributeNames() {
    return immutableAttributeNames;
  }

  public void setImmutableAttributeNames(List<String> immutableAttributeNames) {
    this.immutableAttributeNames = immutableAttributeNames;
  }

  public ApiTimeSeriesEntityType mutableAttributeNames(List<String> mutableAttributeNames) {
    this.mutableAttributeNames = mutableAttributeNames;
    return this;
  }

  public ApiTimeSeriesEntityType addMutableAttributeNamesItem(String mutableAttributeNamesItem) {
    if (this.mutableAttributeNames == null) {
      this.mutableAttributeNames = new ArrayList<>();
    }
    this.mutableAttributeNames.add(mutableAttributeNamesItem);
    return this;
  }

  /**
   * Returns the list of mutable attributes for this entity type. Mutable attributes for an entity may change over its lifetime.
   * @return mutableAttributeNames
  **/
  @ApiModelProperty(value = "Returns the list of mutable attributes for this entity type. Mutable attributes for an entity may change over its lifetime.")


  public List<String> getMutableAttributeNames() {
    return mutableAttributeNames;
  }

  public void setMutableAttributeNames(List<String> mutableAttributeNames) {
    this.mutableAttributeNames = mutableAttributeNames;
  }

  public ApiTimeSeriesEntityType entityNameFormat(List<String> entityNameFormat) {
    this.entityNameFormat = entityNameFormat;
    return this;
  }

  public ApiTimeSeriesEntityType addEntityNameFormatItem(String entityNameFormatItem) {
    if (this.entityNameFormat == null) {
      this.entityNameFormat = new ArrayList<>();
    }
    this.entityNameFormat.add(entityNameFormatItem);
    return this;
  }

  /**
   * Returns a list of attribute names that will be used to construct entity names for entities of this type. The attributes named here must be immutable attributes of this type or a parent type.
   * @return entityNameFormat
  **/
  @ApiModelProperty(value = "Returns a list of attribute names that will be used to construct entity names for entities of this type. The attributes named here must be immutable attributes of this type or a parent type.")


  public List<String> getEntityNameFormat() {
    return entityNameFormat;
  }

  public void setEntityNameFormat(List<String> entityNameFormat) {
    this.entityNameFormat = entityNameFormat;
  }

  public ApiTimeSeriesEntityType entityDisplayNameFormat(String entityDisplayNameFormat) {
    this.entityDisplayNameFormat = entityDisplayNameFormat;
    return this;
  }

  /**
   * Returns a format string that will be used to construct the display name of entities of this type. If this returns null the entity name would be used as the display name.  The entity attribute values are used to replace $attribute name portions of this format string. For example, an entity with roleType \"DATANODE\" and hostname \"foo.com\" will have a display name \"DATANODE (foo.com)\" if the format is \"$roleType ($hostname)\".
   * @return entityDisplayNameFormat
  **/
  @ApiModelProperty(value = "Returns a format string that will be used to construct the display name of entities of this type. If this returns null the entity name would be used as the display name.  The entity attribute values are used to replace $attribute name portions of this format string. For example, an entity with roleType \"DATANODE\" and hostname \"foo.com\" will have a display name \"DATANODE (foo.com)\" if the format is \"$roleType ($hostname)\".")


  public String getEntityDisplayNameFormat() {
    return entityDisplayNameFormat;
  }

  public void setEntityDisplayNameFormat(String entityDisplayNameFormat) {
    this.entityDisplayNameFormat = entityDisplayNameFormat;
  }

  public ApiTimeSeriesEntityType parentMetricEntityTypeNames(List<String> parentMetricEntityTypeNames) {
    this.parentMetricEntityTypeNames = parentMetricEntityTypeNames;
    return this;
  }

  public ApiTimeSeriesEntityType addParentMetricEntityTypeNamesItem(String parentMetricEntityTypeNamesItem) {
    if (this.parentMetricEntityTypeNames == null) {
      this.parentMetricEntityTypeNames = new ArrayList<>();
    }
    this.parentMetricEntityTypeNames.add(parentMetricEntityTypeNamesItem);
    return this;
  }

  /**
   * Returns a list of metric entity type names which are parents of this metric entity type. A metric entity type inherits the attributes of its ancestors. For example a role metric entity type has its service as a parent. A service metric entity type has a cluster as a parent. The role type inherits its cluster name attribute through its service parent. Only parent ancestors should be returned here. In the example given, only the service metric entity type should be specified in the parent list.
   * @return parentMetricEntityTypeNames
  **/
  @ApiModelProperty(value = "Returns a list of metric entity type names which are parents of this metric entity type. A metric entity type inherits the attributes of its ancestors. For example a role metric entity type has its service as a parent. A service metric entity type has a cluster as a parent. The role type inherits its cluster name attribute through its service parent. Only parent ancestors should be returned here. In the example given, only the service metric entity type should be specified in the parent list.")


  public List<String> getParentMetricEntityTypeNames() {
    return parentMetricEntityTypeNames;
  }

  public void setParentMetricEntityTypeNames(List<String> parentMetricEntityTypeNames) {
    this.parentMetricEntityTypeNames = parentMetricEntityTypeNames;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTimeSeriesEntityType apiTimeSeriesEntityType = (ApiTimeSeriesEntityType) o;
    return Objects.equals(this.name, apiTimeSeriesEntityType.name) &&
        Objects.equals(this.category, apiTimeSeriesEntityType.category) &&
        Objects.equals(this.nameForCrossEntityAggregateMetrics, apiTimeSeriesEntityType.nameForCrossEntityAggregateMetrics) &&
        Objects.equals(this.displayName, apiTimeSeriesEntityType.displayName) &&
        Objects.equals(this.description, apiTimeSeriesEntityType.description) &&
        Objects.equals(this.immutableAttributeNames, apiTimeSeriesEntityType.immutableAttributeNames) &&
        Objects.equals(this.mutableAttributeNames, apiTimeSeriesEntityType.mutableAttributeNames) &&
        Objects.equals(this.entityNameFormat, apiTimeSeriesEntityType.entityNameFormat) &&
        Objects.equals(this.entityDisplayNameFormat, apiTimeSeriesEntityType.entityDisplayNameFormat) &&
        Objects.equals(this.parentMetricEntityTypeNames, apiTimeSeriesEntityType.parentMetricEntityTypeNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, category, nameForCrossEntityAggregateMetrics, displayName, description, immutableAttributeNames, mutableAttributeNames, entityNameFormat, entityDisplayNameFormat, parentMetricEntityTypeNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTimeSeriesEntityType {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    nameForCrossEntityAggregateMetrics: ").append(toIndentedString(nameForCrossEntityAggregateMetrics)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    immutableAttributeNames: ").append(toIndentedString(immutableAttributeNames)).append("\n");
    sb.append("    mutableAttributeNames: ").append(toIndentedString(mutableAttributeNames)).append("\n");
    sb.append("    entityNameFormat: ").append(toIndentedString(entityNameFormat)).append("\n");
    sb.append("    entityDisplayNameFormat: ").append(toIndentedString(entityDisplayNameFormat)).append("\n");
    sb.append("    parentMetricEntityTypeNames: ").append(toIndentedString(parentMetricEntityTypeNames)).append("\n");
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

