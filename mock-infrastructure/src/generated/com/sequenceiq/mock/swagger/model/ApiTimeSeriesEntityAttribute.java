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
 * A time series entity attribute represents a possible attribute of a time series entity type monitored by the Cloudera Management Services. &lt;p&gt; Available since API v11.
 */
@ApiModel(description = "A time series entity attribute represents a possible attribute of a time series entity type monitored by the Cloudera Management Services. <p> Available since API v11.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiTimeSeriesEntityAttribute   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("isValueCaseSensitive")
  private Boolean isValueCaseSensitive = null;

  public ApiTimeSeriesEntityAttribute name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Name of the of the attribute. This name uniquely identifies this attribute.
   * @return name
  **/
  @ApiModelProperty(value = "Name of the of the attribute. This name uniquely identifies this attribute.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiTimeSeriesEntityAttribute displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Display name of the attribute.
   * @return displayName
  **/
  @ApiModelProperty(value = "Display name of the attribute.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiTimeSeriesEntityAttribute description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Description of the attribute.
   * @return description
  **/
  @ApiModelProperty(value = "Description of the attribute.")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ApiTimeSeriesEntityAttribute isValueCaseSensitive(Boolean isValueCaseSensitive) {
    this.isValueCaseSensitive = isValueCaseSensitive;
    return this;
  }

  /**
   * Returns whether to treat attribute values as case-sensitive. Defaults to false.
   * @return isValueCaseSensitive
  **/
  @ApiModelProperty(value = "Returns whether to treat attribute values as case-sensitive. Defaults to false.")


  public Boolean isIsValueCaseSensitive() {
    return isValueCaseSensitive;
  }

  public void setIsValueCaseSensitive(Boolean isValueCaseSensitive) {
    this.isValueCaseSensitive = isValueCaseSensitive;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTimeSeriesEntityAttribute apiTimeSeriesEntityAttribute = (ApiTimeSeriesEntityAttribute) o;
    return Objects.equals(this.name, apiTimeSeriesEntityAttribute.name) &&
        Objects.equals(this.displayName, apiTimeSeriesEntityAttribute.displayName) &&
        Objects.equals(this.description, apiTimeSeriesEntityAttribute.description) &&
        Objects.equals(this.isValueCaseSensitive, apiTimeSeriesEntityAttribute.isValueCaseSensitive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, displayName, description, isValueCaseSensitive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTimeSeriesEntityAttribute {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    isValueCaseSensitive: ").append(toIndentedString(isValueCaseSensitive)).append("\n");
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

