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
 * This is the model for interactive user session information in the API. &lt;p&gt; A user may have more than one active session. Each such session will have its own session object.
 */
@ApiModel(description = "This is the model for interactive user session information in the API. <p> A user may have more than one active session. Each such session will have its own session object.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiUserSession   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("remoteAddr")
  private String remoteAddr = null;

  @JsonProperty("lastRequest")
  private String lastRequest = null;

  public ApiUserSession name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The username associated with the session. <p> This will be the same value shown to the logged in user in the UI, which will normally be the same value they typed when logging in, but it is possible that in certain external authentication scenarios, it will differ from that value.
   * @return name
  **/
  @ApiModelProperty(value = "The username associated with the session. <p> This will be the same value shown to the logged in user in the UI, which will normally be the same value they typed when logging in, but it is possible that in certain external authentication scenarios, it will differ from that value.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiUserSession remoteAddr(String remoteAddr) {
    this.remoteAddr = remoteAddr;
    return this;
  }

  /**
   * The remote IP address for the session. <p> This will be the remote IP address for the last request made as part of this session. It is not guaranteed to be the same IP address as was previously used, or the address used to initiate the session.
   * @return remoteAddr
  **/
  @ApiModelProperty(value = "The remote IP address for the session. <p> This will be the remote IP address for the last request made as part of this session. It is not guaranteed to be the same IP address as was previously used, or the address used to initiate the session.")


  public String getRemoteAddr() {
    return remoteAddr;
  }

  public void setRemoteAddr(String remoteAddr) {
    this.remoteAddr = remoteAddr;
  }

  public ApiUserSession lastRequest(String lastRequest) {
    this.lastRequest = lastRequest;
    return this;
  }

  /**
   * The date and time of the last request received as part of this session. <p> This will be returned in ISO 8601 format from the REST API.
   * @return lastRequest
  **/
  @ApiModelProperty(value = "The date and time of the last request received as part of this session. <p> This will be returned in ISO 8601 format from the REST API.")


  public String getLastRequest() {
    return lastRequest;
  }

  public void setLastRequest(String lastRequest) {
    this.lastRequest = lastRequest;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiUserSession apiUserSession = (ApiUserSession) o;
    return Objects.equals(this.name, apiUserSession.name) &&
        Objects.equals(this.remoteAddr, apiUserSession.remoteAddr) &&
        Objects.equals(this.lastRequest, apiUserSession.lastRequest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, remoteAddr, lastRequest);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiUserSession {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    remoteAddr: ").append(toIndentedString(remoteAddr)).append("\n");
    sb.append("    lastRequest: ").append(toIndentedString(lastRequest)).append("\n");
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

