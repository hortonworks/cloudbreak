package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHostNameList;
import com.sequenceiq.mock.swagger.model.ApiPerfInspectorPingArgs;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used for the Cloudera Manager level performance inspector. Network diagnostics will be run from every host in sourceHostList to every host in targetHostList.
 */
@ApiModel(description = "Arguments used for the Cloudera Manager level performance inspector. Network diagnostics will be run from every host in sourceHostList to every host in targetHostList.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHostsPerfInspectorArgs   {
  @JsonProperty("sourceHostList")
  private ApiHostNameList sourceHostList = null;

  @JsonProperty("targetHostList")
  private ApiHostNameList targetHostList = null;

  @JsonProperty("pingArgs")
  private ApiPerfInspectorPingArgs pingArgs = null;

  public ApiHostsPerfInspectorArgs sourceHostList(ApiHostNameList sourceHostList) {
    this.sourceHostList = sourceHostList;
    return this;
  }

  /**
   * Required list of host names which'll act as source for running network diagnostics test.
   * @return sourceHostList
  **/
  @ApiModelProperty(value = "Required list of host names which'll act as source for running network diagnostics test.")

  @Valid

  public ApiHostNameList getSourceHostList() {
    return sourceHostList;
  }

  public void setSourceHostList(ApiHostNameList sourceHostList) {
    this.sourceHostList = sourceHostList;
  }

  public ApiHostsPerfInspectorArgs targetHostList(ApiHostNameList targetHostList) {
    this.targetHostList = targetHostList;
    return this;
  }

  /**
   * Required list of host names which'll act as target for running network diagnostics test.
   * @return targetHostList
  **/
  @ApiModelProperty(value = "Required list of host names which'll act as target for running network diagnostics test.")

  @Valid

  public ApiHostNameList getTargetHostList() {
    return targetHostList;
  }

  public void setTargetHostList(ApiHostNameList targetHostList) {
    this.targetHostList = targetHostList;
  }

  public ApiHostsPerfInspectorArgs pingArgs(ApiPerfInspectorPingArgs pingArgs) {
    this.pingArgs = pingArgs;
    return this;
  }

  /**
   * Optional ping request arguments. If not specified, default arguments will be used for ping test.
   * @return pingArgs
  **/
  @ApiModelProperty(value = "Optional ping request arguments. If not specified, default arguments will be used for ping test.")

  @Valid

  public ApiPerfInspectorPingArgs getPingArgs() {
    return pingArgs;
  }

  public void setPingArgs(ApiPerfInspectorPingArgs pingArgs) {
    this.pingArgs = pingArgs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHostsPerfInspectorArgs apiHostsPerfInspectorArgs = (ApiHostsPerfInspectorArgs) o;
    return Objects.equals(this.sourceHostList, apiHostsPerfInspectorArgs.sourceHostList) &&
        Objects.equals(this.targetHostList, apiHostsPerfInspectorArgs.targetHostList) &&
        Objects.equals(this.pingArgs, apiHostsPerfInspectorArgs.pingArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceHostList, targetHostList, pingArgs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHostsPerfInspectorArgs {\n");
    
    sb.append("    sourceHostList: ").append(toIndentedString(sourceHostList)).append("\n");
    sb.append("    targetHostList: ").append(toIndentedString(targetHostList)).append("\n");
    sb.append("    pingArgs: ").append(toIndentedString(pingArgs)).append("\n");
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

