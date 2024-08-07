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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Request object for an update AWS credential request.
 */
@JsonPropertyOrder({
  UpdateAwsCredentialRequest.JSON_PROPERTY_CREDENTIAL_NAME,
  UpdateAwsCredentialRequest.JSON_PROPERTY_ROLE_ARN,
  UpdateAwsCredentialRequest.JSON_PROPERTY_DESCRIPTION,
  UpdateAwsCredentialRequest.JSON_PROPERTY_SKIP_ORG_POLICY_DECISIONS,
  UpdateAwsCredentialRequest.JSON_PROPERTY_VERIFY_PERMISSIONS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class UpdateAwsCredentialRequest {
  public static final String JSON_PROPERTY_CREDENTIAL_NAME = "credentialName";
  private String credentialName;

  public static final String JSON_PROPERTY_ROLE_ARN = "roleArn";
  private String roleArn;

  public static final String JSON_PROPERTY_DESCRIPTION = "description";
  private String description;

  public static final String JSON_PROPERTY_SKIP_ORG_POLICY_DECISIONS = "skipOrgPolicyDecisions";
  private Boolean skipOrgPolicyDecisions = false;

  public static final String JSON_PROPERTY_VERIFY_PERMISSIONS = "verifyPermissions";
  private Boolean verifyPermissions = false;

  public UpdateAwsCredentialRequest() { 
  }

  public UpdateAwsCredentialRequest credentialName(String credentialName) {
    this.credentialName = credentialName;
    return this;
  }

   /**
   * The name of the credential.
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


  public UpdateAwsCredentialRequest roleArn(String roleArn) {
    this.roleArn = roleArn;
    return this;
  }

   /**
   * The ARN of the delegated access role.
   * @return roleArn
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ROLE_ARN)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getRoleArn() {
    return roleArn;
  }


  @JsonProperty(JSON_PROPERTY_ROLE_ARN)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setRoleArn(String roleArn) {
    this.roleArn = roleArn;
  }


  public UpdateAwsCredentialRequest description(String description) {
    this.description = description;
    return this;
  }

   /**
   * A description for the credential.
   * @return description
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getDescription() {
    return description;
  }


  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDescription(String description) {
    this.description = description;
  }


  public UpdateAwsCredentialRequest skipOrgPolicyDecisions(Boolean skipOrgPolicyDecisions) {
    this.skipOrgPolicyDecisions = skipOrgPolicyDecisions;
    return this;
  }

   /**
   * Whether to skip organizational policy decision checks or not.
   * @return skipOrgPolicyDecisions
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SKIP_ORG_POLICY_DECISIONS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getSkipOrgPolicyDecisions() {
    return skipOrgPolicyDecisions;
  }


  @JsonProperty(JSON_PROPERTY_SKIP_ORG_POLICY_DECISIONS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSkipOrgPolicyDecisions(Boolean skipOrgPolicyDecisions) {
    this.skipOrgPolicyDecisions = skipOrgPolicyDecisions;
  }


  public UpdateAwsCredentialRequest verifyPermissions(Boolean verifyPermissions) {
    this.verifyPermissions = verifyPermissions;
    return this;
  }

   /**
   * Whether to verify permissions upon saving or not.
   * @return verifyPermissions
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_VERIFY_PERMISSIONS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getVerifyPermissions() {
    return verifyPermissions;
  }


  @JsonProperty(JSON_PROPERTY_VERIFY_PERMISSIONS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setVerifyPermissions(Boolean verifyPermissions) {
    this.verifyPermissions = verifyPermissions;
  }


  /**
   * Return true if this UpdateAwsCredentialRequest object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateAwsCredentialRequest updateAwsCredentialRequest = (UpdateAwsCredentialRequest) o;
    return Objects.equals(this.credentialName, updateAwsCredentialRequest.credentialName) &&
        Objects.equals(this.roleArn, updateAwsCredentialRequest.roleArn) &&
        Objects.equals(this.description, updateAwsCredentialRequest.description) &&
        Objects.equals(this.skipOrgPolicyDecisions, updateAwsCredentialRequest.skipOrgPolicyDecisions) &&
        Objects.equals(this.verifyPermissions, updateAwsCredentialRequest.verifyPermissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentialName, roleArn, description, skipOrgPolicyDecisions, verifyPermissions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateAwsCredentialRequest {\n");
    sb.append("    credentialName: ").append(toIndentedString(credentialName)).append("\n");
    sb.append("    roleArn: ").append(toIndentedString(roleArn)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    skipOrgPolicyDecisions: ").append(toIndentedString(skipOrgPolicyDecisions)).append("\n");
    sb.append("    verifyPermissions: ").append(toIndentedString(verifyPermissions)).append("\n");
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

    // add `credentialName` to the URL query string
    if (getCredentialName() != null) {
      joiner.add(String.format("%scredentialName%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getCredentialName()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `roleArn` to the URL query string
    if (getRoleArn() != null) {
      joiner.add(String.format("%sroleArn%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getRoleArn()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `description` to the URL query string
    if (getDescription() != null) {
      joiner.add(String.format("%sdescription%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getDescription()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `skipOrgPolicyDecisions` to the URL query string
    if (getSkipOrgPolicyDecisions() != null) {
      joiner.add(String.format("%sskipOrgPolicyDecisions%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getSkipOrgPolicyDecisions()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `verifyPermissions` to the URL query string
    if (getVerifyPermissions() != null) {
      joiner.add(String.format("%sverifyPermissions%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getVerifyPermissions()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}

