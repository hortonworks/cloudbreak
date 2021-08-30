package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiPerfInspectorPingArgs;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used for the Cluster Performance Inspector
 */
@ApiModel(description = "Arguments used for the Cluster Performance Inspector")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterPerfInspectorArgs   {
  @JsonProperty("pingArgs")
  private ApiPerfInspectorPingArgs pingArgs = null;

  public ApiClusterPerfInspectorArgs pingArgs(ApiPerfInspectorPingArgs pingArgs) {
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
    ApiClusterPerfInspectorArgs apiClusterPerfInspectorArgs = (ApiClusterPerfInspectorArgs) o;
    return Objects.equals(this.pingArgs, apiClusterPerfInspectorArgs.pingArgs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pingArgs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterPerfInspectorArgs {\n");
    
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

