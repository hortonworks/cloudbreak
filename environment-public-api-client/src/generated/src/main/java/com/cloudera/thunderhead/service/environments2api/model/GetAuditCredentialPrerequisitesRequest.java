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
 * Request object for getting the audit credential prerequisites for the given cloud platform.
 */
@JsonPropertyOrder({
  GetAuditCredentialPrerequisitesRequest.JSON_PROPERTY_CLOUD_PLATFORM
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class GetAuditCredentialPrerequisitesRequest {
  /**
   * The kind of cloud platform.
   */
  public enum CloudPlatformEnum {
    AWS("AWS"),
    
    AZURE("AZURE"),
    
    GCP("GCP");

    private String value;

    CloudPlatformEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static CloudPlatformEnum fromValue(String value) {
      for (CloudPlatformEnum b : CloudPlatformEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_CLOUD_PLATFORM = "cloudPlatform";
  private CloudPlatformEnum cloudPlatform;

  public GetAuditCredentialPrerequisitesRequest() { 
  }

  public GetAuditCredentialPrerequisitesRequest cloudPlatform(CloudPlatformEnum cloudPlatform) {
    this.cloudPlatform = cloudPlatform;
    return this;
  }

   /**
   * The kind of cloud platform.
   * @return cloudPlatform
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_CLOUD_PLATFORM)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public CloudPlatformEnum getCloudPlatform() {
    return cloudPlatform;
  }


  @JsonProperty(JSON_PROPERTY_CLOUD_PLATFORM)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setCloudPlatform(CloudPlatformEnum cloudPlatform) {
    this.cloudPlatform = cloudPlatform;
  }


  /**
   * Return true if this GetAuditCredentialPrerequisitesRequest object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetAuditCredentialPrerequisitesRequest getAuditCredentialPrerequisitesRequest = (GetAuditCredentialPrerequisitesRequest) o;
    return Objects.equals(this.cloudPlatform, getAuditCredentialPrerequisitesRequest.cloudPlatform);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cloudPlatform);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetAuditCredentialPrerequisitesRequest {\n");
    sb.append("    cloudPlatform: ").append(toIndentedString(cloudPlatform)).append("\n");
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

    // add `cloudPlatform` to the URL query string
    if (getCloudPlatform() != null) {
      joiner.add(String.format("%scloudPlatform%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getCloudPlatform()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}

