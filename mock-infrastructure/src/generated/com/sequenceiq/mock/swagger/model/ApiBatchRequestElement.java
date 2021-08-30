package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.HTTPMethod;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A single element of a batch request, often part of a list with other elements.
 */
@ApiModel(description = "A single element of a batch request, often part of a list with other elements.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiBatchRequestElement   {
  @JsonProperty("method")
  private HTTPMethod method = null;

  @JsonProperty("url")
  private String url = null;

  @JsonProperty("body")
  private Object body = null;

  @JsonProperty("contentType")
  private String contentType = null;

  @JsonProperty("acceptType")
  private String acceptType = null;

  public ApiBatchRequestElement method(HTTPMethod method) {
    this.method = method;
    return this;
  }

  /**
   * The type of request (e.g. POST, GET, etc.).
   * @return method
  **/
  @ApiModelProperty(value = "The type of request (e.g. POST, GET, etc.).")

  @Valid

  public HTTPMethod getMethod() {
    return method;
  }

  public void setMethod(HTTPMethod method) {
    this.method = method;
  }

  public ApiBatchRequestElement url(String url) {
    this.url = url;
    return this;
  }

  /**
   * The URL of the request. Must not have a scheme, host, or port. The path should be prefixed with \"/api/\", and should include path and query parameters.
   * @return url
  **/
  @ApiModelProperty(value = "The URL of the request. Must not have a scheme, host, or port. The path should be prefixed with \"/api/\", and should include path and query parameters.")


  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public ApiBatchRequestElement body(Object body) {
    this.body = body;
    return this;
  }

  /**
   * Optional body of the request. Must be serialized in accordance with #getContentType(). For application/json, use com.cloudera.api.ApiObjectMapper.
   * @return body
  **/
  @ApiModelProperty(value = "Optional body of the request. Must be serialized in accordance with #getContentType(). For application/json, use com.cloudera.api.ApiObjectMapper.")


  public Object getBody() {
    return body;
  }

  public void setBody(Object body) {
    this.body = body;
  }

  public ApiBatchRequestElement contentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  /**
   * Content-Type header of the request element. If unset, the element will be treated as if the wildcard type had been specified unless it has a body, in which case it will fall back to application/json.
   * @return contentType
  **/
  @ApiModelProperty(value = "Content-Type header of the request element. If unset, the element will be treated as if the wildcard type had been specified unless it has a body, in which case it will fall back to application/json.")


  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public ApiBatchRequestElement acceptType(String acceptType) {
    this.acceptType = acceptType;
    return this;
  }

  /**
   * Accept header of the request element. The response body (if it exists) will be in this representation. If unset, the element will be treated as if the wildcard type had been requested.
   * @return acceptType
  **/
  @ApiModelProperty(value = "Accept header of the request element. The response body (if it exists) will be in this representation. If unset, the element will be treated as if the wildcard type had been requested.")


  public String getAcceptType() {
    return acceptType;
  }

  public void setAcceptType(String acceptType) {
    this.acceptType = acceptType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiBatchRequestElement apiBatchRequestElement = (ApiBatchRequestElement) o;
    return Objects.equals(this.method, apiBatchRequestElement.method) &&
        Objects.equals(this.url, apiBatchRequestElement.url) &&
        Objects.equals(this.body, apiBatchRequestElement.body) &&
        Objects.equals(this.contentType, apiBatchRequestElement.contentType) &&
        Objects.equals(this.acceptType, apiBatchRequestElement.acceptType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, url, body, contentType, acceptType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiBatchRequestElement {\n");
    
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
    sb.append("    acceptType: ").append(toIndentedString(acceptType)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

