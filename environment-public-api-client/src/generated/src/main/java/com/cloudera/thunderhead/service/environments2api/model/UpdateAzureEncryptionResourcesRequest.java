/*
 * Cloudera Environments Service
 * Cloudera Environments Service is a web service that manages cloud provider access.
 *
 * The version of the OpenAPI document: __API_VERSION__
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.cloudera.thunderhead.service.environments2api.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Request object for updating Azure encryption resources.
 */
@JsonPropertyOrder({
  UpdateAzureEncryptionResourcesRequest.JSON_PROPERTY_ENVIRONMENT,
  UpdateAzureEncryptionResourcesRequest.JSON_PROPERTY_ENCRYPTION_KEY_URL,
  UpdateAzureEncryptionResourcesRequest.JSON_PROPERTY_ENCRYPTION_KEY_RESOURCE_GROUP_NAME,
  UpdateAzureEncryptionResourcesRequest.JSON_PROPERTY_ENCRYPTION_USER_MANAGED_IDENTITY
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class UpdateAzureEncryptionResourcesRequest {
  public static final String JSON_PROPERTY_ENVIRONMENT = "environment";
  private String environment;

  public static final String JSON_PROPERTY_ENCRYPTION_KEY_URL = "encryptionKeyUrl";
  private String encryptionKeyUrl;

  public static final String JSON_PROPERTY_ENCRYPTION_KEY_RESOURCE_GROUP_NAME = "encryptionKeyResourceGroupName";
  private String encryptionKeyResourceGroupName;

  public static final String JSON_PROPERTY_ENCRYPTION_USER_MANAGED_IDENTITY = "encryptionUserManagedIdentity";
  private String encryptionUserManagedIdentity;

  public UpdateAzureEncryptionResourcesRequest() {
  }

  public UpdateAzureEncryptionResourcesRequest environment(String environment) {
    
    this.environment = environment;
    return this;
  }

   /**
   * The name or CRN of the environment.
   * @return environment
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ENVIRONMENT)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getEnvironment() {
    return environment;
  }


  @JsonProperty(JSON_PROPERTY_ENVIRONMENT)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setEnvironment(String environment) {
    this.environment = environment;
  }


  public UpdateAzureEncryptionResourcesRequest encryptionKeyUrl(String encryptionKeyUrl) {
    
    this.encryptionKeyUrl = encryptionKeyUrl;
    return this;
  }

   /**
   * The URL of an encryption key, which will be used to encrypt the Azure Managed Disks, if the entitlement has been granted.
   * @return encryptionKeyUrl
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ENCRYPTION_KEY_URL)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getEncryptionKeyUrl() {
    return encryptionKeyUrl;
  }


  @JsonProperty(JSON_PROPERTY_ENCRYPTION_KEY_URL)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setEncryptionKeyUrl(String encryptionKeyUrl) {
    this.encryptionKeyUrl = encryptionKeyUrl;
  }


  public UpdateAzureEncryptionResourcesRequest encryptionKeyResourceGroupName(String encryptionKeyResourceGroupName) {
    
    this.encryptionKeyResourceGroupName = encryptionKeyResourceGroupName;
    return this;
  }

   /**
   * Name of the existing Azure resource group hosting the Azure Key Vault containing customer managed key which will be used to encrypt the Azure Managed Disks. It is required only when the entitlement is granted and the resource group of the key vault is different from the resource group in which the environment is to be created. Omitting it implies that, the key vault containing the encryption key is present in the same resource group where the environment would be created.
   * @return encryptionKeyResourceGroupName
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENCRYPTION_KEY_RESOURCE_GROUP_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getEncryptionKeyResourceGroupName() {
    return encryptionKeyResourceGroupName;
  }


  @JsonProperty(JSON_PROPERTY_ENCRYPTION_KEY_RESOURCE_GROUP_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEncryptionKeyResourceGroupName(String encryptionKeyResourceGroupName) {
    this.encryptionKeyResourceGroupName = encryptionKeyResourceGroupName;
  }


  public UpdateAzureEncryptionResourcesRequest encryptionUserManagedIdentity(String encryptionUserManagedIdentity) {
    
    this.encryptionUserManagedIdentity = encryptionUserManagedIdentity;
    return this;
  }

   /**
   * User managed identity for encryption.
   * @return encryptionUserManagedIdentity
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENCRYPTION_USER_MANAGED_IDENTITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getEncryptionUserManagedIdentity() {
    return encryptionUserManagedIdentity;
  }


  @JsonProperty(JSON_PROPERTY_ENCRYPTION_USER_MANAGED_IDENTITY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEncryptionUserManagedIdentity(String encryptionUserManagedIdentity) {
    this.encryptionUserManagedIdentity = encryptionUserManagedIdentity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateAzureEncryptionResourcesRequest updateAzureEncryptionResourcesRequest = (UpdateAzureEncryptionResourcesRequest) o;
    return Objects.equals(this.environment, updateAzureEncryptionResourcesRequest.environment) &&
        Objects.equals(this.encryptionKeyUrl, updateAzureEncryptionResourcesRequest.encryptionKeyUrl) &&
        Objects.equals(this.encryptionKeyResourceGroupName, updateAzureEncryptionResourcesRequest.encryptionKeyResourceGroupName) &&
        Objects.equals(this.encryptionUserManagedIdentity, updateAzureEncryptionResourcesRequest.encryptionUserManagedIdentity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environment, encryptionKeyUrl, encryptionKeyResourceGroupName, encryptionUserManagedIdentity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateAzureEncryptionResourcesRequest {\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    encryptionKeyUrl: ").append(toIndentedString(encryptionKeyUrl)).append("\n");
    sb.append("    encryptionKeyResourceGroupName: ").append(toIndentedString(encryptionKeyResourceGroupName)).append("\n");
    sb.append("    encryptionUserManagedIdentity: ").append(toIndentedString(encryptionUserManagedIdentity)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

