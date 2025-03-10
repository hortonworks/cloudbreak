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
 * The request object for updating the environment SSH key.
 */
@JsonPropertyOrder({
  UpdateSshKeyRequest.JSON_PROPERTY_ENVIRONMENT,
  UpdateSshKeyRequest.JSON_PROPERTY_NEW_PUBLIC_KEY,
  UpdateSshKeyRequest.JSON_PROPERTY_EXISTING_PUBLIC_KEY_ID
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class UpdateSshKeyRequest {
  public static final String JSON_PROPERTY_ENVIRONMENT = "environment";
  private String environment;

  public static final String JSON_PROPERTY_NEW_PUBLIC_KEY = "newPublicKey";
  private String newPublicKey;

  public static final String JSON_PROPERTY_EXISTING_PUBLIC_KEY_ID = "existingPublicKeyId";
  private String existingPublicKeyId;

  public UpdateSshKeyRequest() {
  }

  public UpdateSshKeyRequest environment(String environment) {
    
    this.environment = environment;
    return this;
  }

   /**
   * The name or the CRN of the environment.
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


  public UpdateSshKeyRequest newPublicKey(String newPublicKey) {
    
    this.newPublicKey = newPublicKey;
    return this;
  }

   /**
   * A new SSH public key that is stored locally. Either this or an existing public key ID has to be given.
   * @return newPublicKey
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NEW_PUBLIC_KEY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getNewPublicKey() {
    return newPublicKey;
  }


  @JsonProperty(JSON_PROPERTY_NEW_PUBLIC_KEY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNewPublicKey(String newPublicKey) {
    this.newPublicKey = newPublicKey;
  }


  public UpdateSshKeyRequest existingPublicKeyId(String existingPublicKeyId) {
    
    this.existingPublicKeyId = existingPublicKeyId;
    return this;
  }

   /**
   * The ID of the existing SSH public key that is stored on the cloud provider side. Either this or a new public key has to be given.
   * @return existingPublicKeyId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_EXISTING_PUBLIC_KEY_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getExistingPublicKeyId() {
    return existingPublicKeyId;
  }


  @JsonProperty(JSON_PROPERTY_EXISTING_PUBLIC_KEY_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setExistingPublicKeyId(String existingPublicKeyId) {
    this.existingPublicKeyId = existingPublicKeyId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateSshKeyRequest updateSshKeyRequest = (UpdateSshKeyRequest) o;
    return Objects.equals(this.environment, updateSshKeyRequest.environment) &&
        Objects.equals(this.newPublicKey, updateSshKeyRequest.newPublicKey) &&
        Objects.equals(this.existingPublicKeyId, updateSshKeyRequest.existingPublicKeyId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environment, newPublicKey, existingPublicKeyId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateSshKeyRequest {\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    newPublicKey: ").append(toIndentedString(newPublicKey)).append("\n");
    sb.append("    existingPublicKeyId: ").append(toIndentedString(existingPublicKeyId)).append("\n");
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

