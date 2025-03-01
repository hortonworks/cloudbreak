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
import com.cloudera.thunderhead.service.environments2api.model.ProxyConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Response object for a create proxy config request.
 */
@JsonPropertyOrder({
  CreateProxyConfigResponse.JSON_PROPERTY_PROXY_CONFIG
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class CreateProxyConfigResponse {
  public static final String JSON_PROPERTY_PROXY_CONFIG = "proxyConfig";
  private ProxyConfig proxyConfig;

  public CreateProxyConfigResponse() {
  }

  public CreateProxyConfigResponse proxyConfig(ProxyConfig proxyConfig) {
    
    this.proxyConfig = proxyConfig;
    return this;
  }

   /**
   * Get proxyConfig
   * @return proxyConfig
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_PROXY_CONFIG)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public ProxyConfig getProxyConfig() {
    return proxyConfig;
  }


  @JsonProperty(JSON_PROPERTY_PROXY_CONFIG)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setProxyConfig(ProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateProxyConfigResponse createProxyConfigResponse = (CreateProxyConfigResponse) o;
    return Objects.equals(this.proxyConfig, createProxyConfigResponse.proxyConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(proxyConfig);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateProxyConfigResponse {\n");
    sb.append("    proxyConfig: ").append(toIndentedString(proxyConfig)).append("\n");
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

