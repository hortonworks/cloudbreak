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
 * Request object for a list proxy configs request.
 */
@JsonPropertyOrder({
  ListProxyConfigsRequest.JSON_PROPERTY_PROXY_CONFIG_NAME
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class ListProxyConfigsRequest {
  public static final String JSON_PROPERTY_PROXY_CONFIG_NAME = "proxyConfigName";
  private String proxyConfigName;

  public ListProxyConfigsRequest() { 
  }

  public ListProxyConfigsRequest proxyConfigName(String proxyConfigName) {
    this.proxyConfigName = proxyConfigName;
    return this;
  }

   /**
   * An optional proxy config name to search by.
   * @return proxyConfigName
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PROXY_CONFIG_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getProxyConfigName() {
    return proxyConfigName;
  }


  @JsonProperty(JSON_PROPERTY_PROXY_CONFIG_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setProxyConfigName(String proxyConfigName) {
    this.proxyConfigName = proxyConfigName;
  }


  /**
   * Return true if this ListProxyConfigsRequest object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListProxyConfigsRequest listProxyConfigsRequest = (ListProxyConfigsRequest) o;
    return Objects.equals(this.proxyConfigName, listProxyConfigsRequest.proxyConfigName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(proxyConfigName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListProxyConfigsRequest {\n");
    sb.append("    proxyConfigName: ").append(toIndentedString(proxyConfigName)).append("\n");
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

    // add `proxyConfigName` to the URL query string
    if (getProxyConfigName() != null) {
      joiner.add(String.format("%sproxyConfigName%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getProxyConfigName()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}

