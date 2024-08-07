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
 * Request object for a set AWS GovCloud audit credential request.
 */
@JsonPropertyOrder({
  SetAWSGovCloudAuditCredentialRequest.JSON_PROPERTY_ROLE_ARN
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class SetAWSGovCloudAuditCredentialRequest {
  public static final String JSON_PROPERTY_ROLE_ARN = "roleArn";
  private String roleArn;

  public SetAWSGovCloudAuditCredentialRequest() { 
  }

  public SetAWSGovCloudAuditCredentialRequest roleArn(String roleArn) {
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


  /**
   * Return true if this SetAWSGovCloudAuditCredentialRequest object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetAWSGovCloudAuditCredentialRequest setAWSGovCloudAuditCredentialRequest = (SetAWSGovCloudAuditCredentialRequest) o;
    return Objects.equals(this.roleArn, setAWSGovCloudAuditCredentialRequest.roleArn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleArn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetAWSGovCloudAuditCredentialRequest {\n");
    sb.append("    roleArn: ").append(toIndentedString(roleArn)).append("\n");
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

    // add `roleArn` to the URL query string
    if (getRoleArn() != null) {
      joiner.add(String.format("%sroleArn%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getRoleArn()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}

