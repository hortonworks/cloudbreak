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
 * Metadata about an Impala query attribute.
 */
@ApiModel(description = "Metadata about an Impala query attribute.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiImpalaQueryAttribute   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("supportsHistograms")
  private Boolean supportsHistograms = null;

  @JsonProperty("description")
  private String description = null;

  public ApiImpalaQueryAttribute name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the attribute. This name can be used in filters, for example 'user' could be used in the filter 'user = root'.
   * @return name
  **/
  @ApiModelProperty(value = "The name of the attribute. This name can be used in filters, for example 'user' could be used in the filter 'user = root'.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiImpalaQueryAttribute type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the attribute. Valid types are STRING, NUMBER, BOOLEAN, BYTES, MILLISECONDS, BYTES_PER_SECOND, BYTE_SECONDS.
   * @return type
  **/
  @ApiModelProperty(value = "The type of the attribute. Valid types are STRING, NUMBER, BOOLEAN, BYTES, MILLISECONDS, BYTES_PER_SECOND, BYTE_SECONDS.")


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ApiImpalaQueryAttribute displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * The display name for the attribute.
   * @return displayName
  **/
  @ApiModelProperty(value = "The display name for the attribute.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiImpalaQueryAttribute supportsHistograms(Boolean supportsHistograms) {
    this.supportsHistograms = supportsHistograms;
    return this;
  }

  /**
   * Whether the Service Monitor can generate a histogram of the distribution of the attribute across queries.
   * @return supportsHistograms
  **/
  @ApiModelProperty(value = "Whether the Service Monitor can generate a histogram of the distribution of the attribute across queries.")


  public Boolean isSupportsHistograms() {
    return supportsHistograms;
  }

  public void setSupportsHistograms(Boolean supportsHistograms) {
    this.supportsHistograms = supportsHistograms;
  }

  public ApiImpalaQueryAttribute description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of the attribute.
   * @return description
  **/
  @ApiModelProperty(value = "The description of the attribute.")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiImpalaQueryAttribute apiImpalaQueryAttribute = (ApiImpalaQueryAttribute) o;
    return Objects.equals(this.name, apiImpalaQueryAttribute.name) &&
        Objects.equals(this.type, apiImpalaQueryAttribute.type) &&
        Objects.equals(this.displayName, apiImpalaQueryAttribute.displayName) &&
        Objects.equals(this.supportsHistograms, apiImpalaQueryAttribute.supportsHistograms) &&
        Objects.equals(this.description, apiImpalaQueryAttribute.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, displayName, supportsHistograms, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiImpalaQueryAttribute {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    supportsHistograms: ").append(toIndentedString(supportsHistograms)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

