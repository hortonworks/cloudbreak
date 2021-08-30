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
 * Represents an instantiation of an external account type, referencing a supported external account type, via the typeName field, along with suitable configuration to access an external resource of the provided type.  The typeName field must match the name of an external account type.
 */
@ApiModel(description = "Represents an instantiation of an external account type, referencing a supported external account type, via the typeName field, along with suitable configuration to access an external resource of the provided type.  The typeName field must match the name of an external account type.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiExternalAccount   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("createdTime")
  private String createdTime = null;

  @JsonProperty("lastModifiedTime")
  private String lastModifiedTime = null;

  @JsonProperty("typeName")
  private String typeName = null;

  @JsonProperty("accountConfigs")
  private ApiConfigList accountConfigs = null;

  public ApiExternalAccount name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Represents the intial name of the account; used to uniquely identify this account.
   * @return name
  **/
  @ApiModelProperty(value = "Represents the intial name of the account; used to uniquely identify this account.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiExternalAccount displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Represents a modifiable label to identify this account for user-visible purposes.
   * @return displayName
  **/
  @ApiModelProperty(value = "Represents a modifiable label to identify this account for user-visible purposes.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiExternalAccount createdTime(String createdTime) {
    this.createdTime = createdTime;
    return this;
  }

  /**
   * Represents the time of creation for this account.
   * @return createdTime
  **/
  @ApiModelProperty(value = "Represents the time of creation for this account.")


  public String getCreatedTime() {
    return createdTime;
  }

  public void setCreatedTime(String createdTime) {
    this.createdTime = createdTime;
  }

  public ApiExternalAccount lastModifiedTime(String lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

  /**
   * Represents the last modification time for this account.
   * @return lastModifiedTime
  **/
  @ApiModelProperty(value = "Represents the last modification time for this account.")


  public String getLastModifiedTime() {
    return lastModifiedTime;
  }

  public void setLastModifiedTime(String lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  public ApiExternalAccount typeName(String typeName) {
    this.typeName = typeName;
    return this;
  }

  /**
   * Represents the Type ID of a supported external account type. The type represented by this field dictates which configuration options must be defined for this account.
   * @return typeName
  **/
  @ApiModelProperty(value = "Represents the Type ID of a supported external account type. The type represented by this field dictates which configuration options must be defined for this account.")


  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public ApiExternalAccount accountConfigs(ApiConfigList accountConfigs) {
    this.accountConfigs = accountConfigs;
    return this;
  }

  /**
   * Represents the account configuration for this account.  When an account is retrieved from the server, the configs returned must match allowed configuration for the type of this account.  When specified for creation of a new account or for the update of an existing account, this field must include every required configuration parameter specified in the type's definition, with the account configuration's value field specified to represent the specific configuration desired for this account.
   * @return accountConfigs
  **/
  @ApiModelProperty(value = "Represents the account configuration for this account.  When an account is retrieved from the server, the configs returned must match allowed configuration for the type of this account.  When specified for creation of a new account or for the update of an existing account, this field must include every required configuration parameter specified in the type's definition, with the account configuration's value field specified to represent the specific configuration desired for this account.")

  @Valid

  public ApiConfigList getAccountConfigs() {
    return accountConfigs;
  }

  public void setAccountConfigs(ApiConfigList accountConfigs) {
    this.accountConfigs = accountConfigs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiExternalAccount apiExternalAccount = (ApiExternalAccount) o;
    return Objects.equals(this.name, apiExternalAccount.name) &&
        Objects.equals(this.displayName, apiExternalAccount.displayName) &&
        Objects.equals(this.createdTime, apiExternalAccount.createdTime) &&
        Objects.equals(this.lastModifiedTime, apiExternalAccount.lastModifiedTime) &&
        Objects.equals(this.typeName, apiExternalAccount.typeName) &&
        Objects.equals(this.accountConfigs, apiExternalAccount.accountConfigs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, displayName, createdTime, lastModifiedTime, typeName, accountConfigs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiExternalAccount {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    createdTime: ").append(toIndentedString(createdTime)).append("\n");
    sb.append("    lastModifiedTime: ").append(toIndentedString(lastModifiedTime)).append("\n");
    sb.append("    typeName: ").append(toIndentedString(typeName)).append("\n");
    sb.append("    accountConfigs: ").append(toIndentedString(accountConfigs)).append("\n");
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

