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
 * API request to execute generic hbase shell command  The payload (command) is passed as it is to hbase shell, so the caller needs to know the exact syntax of the supported hbase shell command, based on underlying hbase version
 */
@ApiModel(description = "API request to execute generic hbase shell command  The payload (command) is passed as it is to hbase shell, so the caller needs to know the exact syntax of the supported hbase shell command, based on underlying hbase version")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHBaseShellCommand   {
  @JsonProperty("payload")
  private String payload = null;

  public ApiHBaseShellCommand payload(String payload) {
    this.payload = payload;
    return this;
  }

  /**
   * 
   * @return payload
  **/
  @ApiModelProperty(value = "")


  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHBaseShellCommand apiHBaseShellCommand = (ApiHBaseShellCommand) o;
    return Objects.equals(this.payload, apiHBaseShellCommand.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(payload);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHBaseShellCommand {\n");
    
    sb.append("    payload: ").append(toIndentedString(payload)).append("\n");
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

