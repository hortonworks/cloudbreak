package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A supported external account type. An external account type represents an external authentication source that is used by Cloudera Manager in its APIs to take suitable actions that require authentication to an external service.  An external account type is uniquely identified by a server-generated ID and identifies with a category identifier: e.g. The \&quot;AWS\&quot; category has an account type \&quot;AWS_Access_Key_Authorization\&quot;
 */
@ApiModel(description = "A supported external account type. An external account type represents an external authentication source that is used by Cloudera Manager in its APIs to take suitable actions that require authentication to an external service.  An external account type is uniquely identified by a server-generated ID and identifies with a category identifier: e.g. The \"AWS\" category has an account type \"AWS_Access_Key_Authorization\"")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiExternalAccountType   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("categoryName")
  private String categoryName = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("allowedAccountConfigs")
  private ApiConfigList allowedAccountConfigs = null;

  public ApiExternalAccountType name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Represents the immutable name for this account.
   * @return name
  **/
  @ApiModelProperty(value = "Represents the immutable name for this account.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiExternalAccountType categoryName(String categoryName) {
    this.categoryName = categoryName;
    return this;
  }

  /**
   * Represents the category of this account.
   * @return categoryName
  **/
  @ApiModelProperty(value = "Represents the category of this account.")


  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
  }

  public ApiExternalAccountType type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Represents the type for this account.
   * @return type
  **/
  @ApiModelProperty(value = "Represents the type for this account.")


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ApiExternalAccountType displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Represents the localized display name for this account.
   * @return displayName
  **/
  @ApiModelProperty(value = "Represents the localized display name for this account.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiExternalAccountType description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Represents the localized description for this account type.
   * @return description
  **/
  @ApiModelProperty(value = "Represents the localized description for this account type.")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ApiExternalAccountType allowedAccountConfigs(ApiConfigList allowedAccountConfigs) {
    this.allowedAccountConfigs = allowedAccountConfigs;
    return this;
  }

  /**
   * Represents the list of allowed account configs.
   * @return allowedAccountConfigs
  **/
  @ApiModelProperty(value = "Represents the list of allowed account configs.")

  @Valid

  public ApiConfigList getAllowedAccountConfigs() {
    return allowedAccountConfigs;
  }

  public void setAllowedAccountConfigs(ApiConfigList allowedAccountConfigs) {
    this.allowedAccountConfigs = allowedAccountConfigs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiExternalAccountType apiExternalAccountType = (ApiExternalAccountType) o;
    return Objects.equals(this.name, apiExternalAccountType.name) &&
        Objects.equals(this.categoryName, apiExternalAccountType.categoryName) &&
        Objects.equals(this.type, apiExternalAccountType.type) &&
        Objects.equals(this.displayName, apiExternalAccountType.displayName) &&
        Objects.equals(this.description, apiExternalAccountType.description) &&
        Objects.equals(this.allowedAccountConfigs, apiExternalAccountType.allowedAccountConfigs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, categoryName, type, displayName, description, allowedAccountConfigs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiExternalAccountType {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    categoryName: ").append(toIndentedString(categoryName)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    allowedAccountConfigs: ").append(toIndentedString(allowedAccountConfigs)).append("\n");
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

