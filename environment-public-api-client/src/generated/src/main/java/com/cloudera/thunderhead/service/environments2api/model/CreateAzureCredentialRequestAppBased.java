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
import com.cloudera.thunderhead.service.environments2api.model.AzureAuthenticationTypeProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Additional configurations needed for app-based authentication.
 */
@JsonPropertyOrder({
  CreateAzureCredentialRequestAppBased.JSON_PROPERTY_AUTHENTICATION_TYPE,
  CreateAzureCredentialRequestAppBased.JSON_PROPERTY_APPLICATION_ID,
  CreateAzureCredentialRequestAppBased.JSON_PROPERTY_SECRET_KEY
})
@JsonTypeName("CreateAzureCredentialRequest_appBased")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class CreateAzureCredentialRequestAppBased {
  public static final String JSON_PROPERTY_AUTHENTICATION_TYPE = "authenticationType";
  private AzureAuthenticationTypeProperties authenticationType;

  public static final String JSON_PROPERTY_APPLICATION_ID = "applicationId";
  private String applicationId;

  public static final String JSON_PROPERTY_SECRET_KEY = "secretKey";
  private String secretKey;

  public CreateAzureCredentialRequestAppBased() {
  }

  public CreateAzureCredentialRequestAppBased authenticationType(AzureAuthenticationTypeProperties authenticationType) {
    
    this.authenticationType = authenticationType;
    return this;
  }

   /**
   * Get authenticationType
   * @return authenticationType
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_AUTHENTICATION_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public AzureAuthenticationTypeProperties getAuthenticationType() {
    return authenticationType;
  }


  @JsonProperty(JSON_PROPERTY_AUTHENTICATION_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAuthenticationType(AzureAuthenticationTypeProperties authenticationType) {
    this.authenticationType = authenticationType;
  }


  public CreateAzureCredentialRequestAppBased applicationId(String applicationId) {
    
    this.applicationId = applicationId;
    return this;
  }

   /**
   * The id of the application registered in Azure.
   * @return applicationId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_APPLICATION_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getApplicationId() {
    return applicationId;
  }


  @JsonProperty(JSON_PROPERTY_APPLICATION_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }


  public CreateAzureCredentialRequestAppBased secretKey(String secretKey) {
    
    this.secretKey = secretKey;
    return this;
  }

   /**
   * The client secret key (also referred to as application password) for the registered application.
   * @return secretKey
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SECRET_KEY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getSecretKey() {
    return secretKey;
  }


  @JsonProperty(JSON_PROPERTY_SECRET_KEY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateAzureCredentialRequestAppBased createAzureCredentialRequestAppBased = (CreateAzureCredentialRequestAppBased) o;
    return Objects.equals(this.authenticationType, createAzureCredentialRequestAppBased.authenticationType) &&
        Objects.equals(this.applicationId, createAzureCredentialRequestAppBased.applicationId) &&
        Objects.equals(this.secretKey, createAzureCredentialRequestAppBased.secretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authenticationType, applicationId, secretKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateAzureCredentialRequestAppBased {\n");
    sb.append("    authenticationType: ").append(toIndentedString(authenticationType)).append("\n");
    sb.append("    applicationId: ").append(toIndentedString(applicationId)).append("\n");
    sb.append("    secretKey: ").append(toIndentedString(secretKey)).append("\n");
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

