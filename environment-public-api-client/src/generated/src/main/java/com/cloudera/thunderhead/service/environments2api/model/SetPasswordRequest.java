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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Request object for set password request. (deprecated)
 */
@JsonPropertyOrder({
  SetPasswordRequest.JSON_PROPERTY_PASSWORD,
  SetPasswordRequest.JSON_PROPERTY_ENVIRONMENT_C_R_NS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class SetPasswordRequest {
  public static final String JSON_PROPERTY_PASSWORD = "password";
  private String password;

  public static final String JSON_PROPERTY_ENVIRONMENT_C_R_NS = "environmentCRNs";
  private List<String> environmentCRNs = new ArrayList<>();

  public SetPasswordRequest() {
  }

  public SetPasswordRequest password(String password) {
    
    this.password = password;
    return this;
  }

   /**
   * password field.
   * @return password
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_PASSWORD)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getPassword() {
    return password;
  }


  @JsonProperty(JSON_PROPERTY_PASSWORD)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setPassword(String password) {
    this.password = password;
  }


  public SetPasswordRequest environmentCRNs(List<String> environmentCRNs) {
    
    this.environmentCRNs = environmentCRNs;
    return this;
  }

  public SetPasswordRequest addEnvironmentCRNsItem(String environmentCRNsItem) {
    if (this.environmentCRNs == null) {
      this.environmentCRNs = new ArrayList<>();
    }
    this.environmentCRNs.add(environmentCRNsItem);
    return this;
  }

   /**
   * Optional list of environment CRNs. Only the passed environments user&#39;s password will be affected. If this field is not present, all environments will be affected.
   * @return environmentCRNs
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENVIRONMENT_C_R_NS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<String> getEnvironmentCRNs() {
    return environmentCRNs;
  }


  @JsonProperty(JSON_PROPERTY_ENVIRONMENT_C_R_NS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnvironmentCRNs(List<String> environmentCRNs) {
    this.environmentCRNs = environmentCRNs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetPasswordRequest setPasswordRequest = (SetPasswordRequest) o;
    return Objects.equals(this.password, setPasswordRequest.password) &&
        Objects.equals(this.environmentCRNs, setPasswordRequest.environmentCRNs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(password, environmentCRNs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetPasswordRequest {\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    environmentCRNs: ").append(toIndentedString(environmentCRNs)).append("\n");
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

