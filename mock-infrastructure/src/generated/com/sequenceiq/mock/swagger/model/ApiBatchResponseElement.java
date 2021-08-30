package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A single element of a batch response, often part of a list with other elements.
 */
@ApiModel(description = "A single element of a batch response, often part of a list with other elements.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiBatchResponseElement   {
  @JsonProperty("statusCode")
  private Integer statusCode = null;

  @JsonProperty("response")
  private Object response = null;

  public ApiBatchResponseElement statusCode(Integer statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  /**
   * Read-only. The HTTP status code of the response.
   * @return statusCode
  **/
  @ApiModelProperty(value = "Read-only. The HTTP status code of the response.")


  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public ApiBatchResponseElement response(Object response) {
    this.response = response;
    return this;
  }

  /**
   * Read-only. The (optional) serialized body of the response, in the representation produced by the corresponding API endpoint, such as application/json.
   * @return response
  **/
  @ApiModelProperty(value = "Read-only. The (optional) serialized body of the response, in the representation produced by the corresponding API endpoint, such as application/json.")


  public Object getResponse() {
    return response;
  }

  public void setResponse(Object response) {
    this.response = response;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiBatchResponseElement apiBatchResponseElement = (ApiBatchResponseElement) o;
    return Objects.equals(this.statusCode, apiBatchResponseElement.statusCode) &&
        Objects.equals(this.response, apiBatchResponseElement.response);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statusCode, response);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiBatchResponseElement {\n");
    
    sb.append("    statusCode: ").append(toIndentedString(statusCode)).append("\n");
    sb.append("    response: ").append(toIndentedString(response)).append("\n");
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

