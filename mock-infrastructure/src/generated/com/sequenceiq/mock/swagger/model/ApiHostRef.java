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
 * A reference to a host.
 */
@ApiModel(description = "A reference to a host.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHostRef   {
  @JsonProperty("hostId")
  private String hostId = null;

  @JsonProperty("hostname")
  private String hostname = null;

  public ApiHostRef hostId(String hostId) {
    this.hostId = hostId;
    return this;
  }

  /**
   * The unique host ID.
   * @return hostId
  **/
  @ApiModelProperty(value = "The unique host ID.")


  public String getHostId() {
    return hostId;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public ApiHostRef hostname(String hostname) {
    this.hostname = hostname;
    return this;
  }

  /**
   * The hostname. Available since v31.
   * @return hostname
  **/
  @ApiModelProperty(value = "The hostname. Available since v31.")


  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHostRef apiHostRef = (ApiHostRef) o;
    return Objects.equals(this.hostId, apiHostRef.hostId) &&
        Objects.equals(this.hostname, apiHostRef.hostname);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostId, hostname);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHostRef {\n");
    
    sb.append("    hostId: ").append(toIndentedString(hostId)).append("\n");
    sb.append("    hostname: ").append(toIndentedString(hostname)).append("\n");
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

