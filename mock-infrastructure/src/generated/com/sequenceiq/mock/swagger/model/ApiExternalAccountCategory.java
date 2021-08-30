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
 * Type representing an external account category.
 */
@ApiModel(description = "Type representing an external account category.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiExternalAccountCategory   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("description")
  private String description = null;

  public ApiExternalAccountCategory name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Represents an identifier for a category.
   * @return name
  **/
  @ApiModelProperty(value = "Represents an identifier for a category.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiExternalAccountCategory displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Represents a localized display name for a category.
   * @return displayName
  **/
  @ApiModelProperty(value = "Represents a localized display name for a category.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiExternalAccountCategory description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Represents a localized description for a category.
   * @return description
  **/
  @ApiModelProperty(value = "Represents a localized description for a category.")


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
    ApiExternalAccountCategory apiExternalAccountCategory = (ApiExternalAccountCategory) o;
    return Objects.equals(this.name, apiExternalAccountCategory.name) &&
        Objects.equals(this.displayName, apiExternalAccountCategory.displayName) &&
        Objects.equals(this.description, apiExternalAccountCategory.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, displayName, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiExternalAccountCategory {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
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

