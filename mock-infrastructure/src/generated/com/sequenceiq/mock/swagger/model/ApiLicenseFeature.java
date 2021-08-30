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
 * Information about the Cloudera Manager license feature details.
 */
@ApiModel(description = "Information about the Cloudera Manager license feature details.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiLicenseFeature   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("enabled")
  private Boolean enabled = null;

  @JsonProperty("description")
  private String description = null;

  public ApiLicenseFeature name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Returns feature name
   * @return name
  **/
  @ApiModelProperty(value = "Returns feature name")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiLicenseFeature enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * 
   * @return enabled
  **/
  @ApiModelProperty(value = "")


  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ApiLicenseFeature description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Returns I18n description of the feature.
   * @return description
  **/
  @ApiModelProperty(value = "Returns I18n description of the feature.")


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
    ApiLicenseFeature apiLicenseFeature = (ApiLicenseFeature) o;
    return Objects.equals(this.name, apiLicenseFeature.name) &&
        Objects.equals(this.enabled, apiLicenseFeature.enabled) &&
        Objects.equals(this.description, apiLicenseFeature.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, enabled, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiLicenseFeature {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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

