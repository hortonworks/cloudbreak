/*
 * CDP Service Discovery
 * CDP Service Discovery is a web service that provides information to a workload service
 *
 * The version of the OpenAPI document: __API_VERSION__
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.cloudera.cdp.servicediscovery.model;

import java.util.Objects;
import java.util.Arrays;
import com.cloudera.cdp.servicediscovery.model.ApiMapEntry;
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
 * A host endPoint for a service.
 */
@JsonPropertyOrder({
  ApiEndPointHost.JSON_PROPERTY_URI,
  ApiEndPointHost.JSON_PROPERTY_END_POINT_CONFIGS,
  ApiEndPointHost.JSON_PROPERTY_TYPE
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class ApiEndPointHost {
  public static final String JSON_PROPERTY_URI = "uri";
  private String uri;

  public static final String JSON_PROPERTY_END_POINT_CONFIGS = "endPointConfigs";
  private List<ApiMapEntry> endPointConfigs = new ArrayList<>();

  public static final String JSON_PROPERTY_TYPE = "type";
  private String type;

  public ApiEndPointHost() {
  }

  public ApiEndPointHost uri(String uri) {
    
    this.uri = uri;
    return this;
  }

   /**
   * Uri for the endPoint.
   * @return uri
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_URI)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getUri() {
    return uri;
  }


  @JsonProperty(JSON_PROPERTY_URI)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUri(String uri) {
    this.uri = uri;
  }


  public ApiEndPointHost endPointConfigs(List<ApiMapEntry> endPointConfigs) {
    
    this.endPointConfigs = endPointConfigs;
    return this;
  }

  public ApiEndPointHost addEndPointConfigsItem(ApiMapEntry endPointConfigsItem) {
    if (this.endPointConfigs == null) {
      this.endPointConfigs = new ArrayList<>();
    }
    this.endPointConfigs.add(endPointConfigsItem);
    return this;
  }

   /**
   * EndPointHost specific configs.
   * @return endPointConfigs
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_END_POINT_CONFIGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<ApiMapEntry> getEndPointConfigs() {
    return endPointConfigs;
  }


  @JsonProperty(JSON_PROPERTY_END_POINT_CONFIGS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEndPointConfigs(List<ApiMapEntry> endPointConfigs) {
    this.endPointConfigs = endPointConfigs;
  }


  public ApiEndPointHost type(String type) {
    
    this.type = type;
    return this;
  }

   /**
   * EndPointHost type.
   * @return type
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getType() {
    return type;
  }


  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEndPointHost apiEndPointHost = (ApiEndPointHost) o;
    return Objects.equals(this.uri, apiEndPointHost.uri) &&
        Objects.equals(this.endPointConfigs, apiEndPointHost.endPointConfigs) &&
        Objects.equals(this.type, apiEndPointHost.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, endPointConfigs, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEndPointHost {\n");
    sb.append("    uri: ").append(toIndentedString(uri)).append("\n");
    sb.append("    endPointConfigs: ").append(toIndentedString(endPointConfigs)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

