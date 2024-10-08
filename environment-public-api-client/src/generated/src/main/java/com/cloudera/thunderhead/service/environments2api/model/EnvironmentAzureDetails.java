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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import com.cloudera.thunderhead.service.environments2api.model.AzureResourceEncryptionParameters;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Azure specific environment configuration information.
 */
@JsonPropertyOrder({
  EnvironmentAzureDetails.JSON_PROPERTY_RESOURCE_GROUP_NAME,
  EnvironmentAzureDetails.JSON_PROPERTY_RESOURCE_ENCRYPTION_PARAMETERS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class EnvironmentAzureDetails {
  public static final String JSON_PROPERTY_RESOURCE_GROUP_NAME = "resourceGroupName";
  private String resourceGroupName;

  public static final String JSON_PROPERTY_RESOURCE_ENCRYPTION_PARAMETERS = "resourceEncryptionParameters";
  private AzureResourceEncryptionParameters resourceEncryptionParameters;

  public EnvironmentAzureDetails() { 
  }

  public EnvironmentAzureDetails resourceGroupName(String resourceGroupName) {
    this.resourceGroupName = resourceGroupName;
    return this;
  }

   /**
   * Name of an existing Azure resource group to be used for the environment. If it is not specified then new resource groups will be generated.
   * @return resourceGroupName
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_RESOURCE_GROUP_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getResourceGroupName() {
    return resourceGroupName;
  }


  @JsonProperty(JSON_PROPERTY_RESOURCE_GROUP_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setResourceGroupName(String resourceGroupName) {
    this.resourceGroupName = resourceGroupName;
  }


  public EnvironmentAzureDetails resourceEncryptionParameters(AzureResourceEncryptionParameters resourceEncryptionParameters) {
    this.resourceEncryptionParameters = resourceEncryptionParameters;
    return this;
  }

   /**
   * Get resourceEncryptionParameters
   * @return resourceEncryptionParameters
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_RESOURCE_ENCRYPTION_PARAMETERS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public AzureResourceEncryptionParameters getResourceEncryptionParameters() {
    return resourceEncryptionParameters;
  }


  @JsonProperty(JSON_PROPERTY_RESOURCE_ENCRYPTION_PARAMETERS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setResourceEncryptionParameters(AzureResourceEncryptionParameters resourceEncryptionParameters) {
    this.resourceEncryptionParameters = resourceEncryptionParameters;
  }


  /**
   * Return true if this Environment_azureDetails object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnvironmentAzureDetails environmentAzureDetails = (EnvironmentAzureDetails) o;
    return Objects.equals(this.resourceGroupName, environmentAzureDetails.resourceGroupName) &&
        Objects.equals(this.resourceEncryptionParameters, environmentAzureDetails.resourceEncryptionParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resourceGroupName, resourceEncryptionParameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnvironmentAzureDetails {\n");
    sb.append("    resourceGroupName: ").append(toIndentedString(resourceGroupName)).append("\n");
    sb.append("    resourceEncryptionParameters: ").append(toIndentedString(resourceEncryptionParameters)).append("\n");
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

  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    // add `resourceGroupName` to the URL query string
    if (getResourceGroupName() != null) {
      joiner.add(String.format("%sresourceGroupName%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getResourceGroupName()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `resourceEncryptionParameters` to the URL query string
    if (getResourceEncryptionParameters() != null) {
      joiner.add(getResourceEncryptionParameters().toUrlQueryString(prefix + "resourceEncryptionParameters" + suffix));
    }

    return joiner.toString();
  }
}

