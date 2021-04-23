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
 * A remote repository URL. Cannot be provided as a path parameter because it may contain special characters.
 */
@ApiModel(description = "A remote repository URL. Cannot be provided as a path parameter because it may contain special characters.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiRemoteRepoUrl   {
  @JsonProperty("remoteRepoUrl")
  private String remoteRepoUrl = null;

  public ApiRemoteRepoUrl remoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
    return this;
  }

  /**
   * 
   * @return remoteRepoUrl
  **/
  @ApiModelProperty(value = "")


  public String getRemoteRepoUrl() {
    return remoteRepoUrl;
  }

  public void setRemoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiRemoteRepoUrl apiRemoteRepoUrl = (ApiRemoteRepoUrl) o;
    return Objects.equals(this.remoteRepoUrl, apiRemoteRepoUrl.remoteRepoUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(remoteRepoUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRemoteRepoUrl {\n");
    
    sb.append("    remoteRepoUrl: ").append(toIndentedString(remoteRepoUrl)).append("\n");
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

