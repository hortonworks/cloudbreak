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
 * Request object for a delete audit credential request.
 */
@JsonPropertyOrder({
  DeleteAuditCredentialRequest.JSON_PROPERTY_CREDENTIAL_NAME
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class DeleteAuditCredentialRequest {
  public static final String JSON_PROPERTY_CREDENTIAL_NAME = "credentialName";
  private String credentialName;

  public DeleteAuditCredentialRequest() {
  }

  public DeleteAuditCredentialRequest credentialName(String credentialName) {
    
    this.credentialName = credentialName;
    return this;
  }

   /**
   * The name or CRN of the credential.
   * @return credentialName
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_CREDENTIAL_NAME)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getCredentialName() {
    return credentialName;
  }


  @JsonProperty(JSON_PROPERTY_CREDENTIAL_NAME)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setCredentialName(String credentialName) {
    this.credentialName = credentialName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeleteAuditCredentialRequest deleteAuditCredentialRequest = (DeleteAuditCredentialRequest) o;
    return Objects.equals(this.credentialName, deleteAuditCredentialRequest.credentialName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentialName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeleteAuditCredentialRequest {\n");
    sb.append("    credentialName: ").append(toIndentedString(credentialName)).append("\n");
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

