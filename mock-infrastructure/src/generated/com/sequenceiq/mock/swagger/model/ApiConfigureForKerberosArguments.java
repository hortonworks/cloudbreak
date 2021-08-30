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
 * Arguments used to configure a cluster for Kerberos.
 */
@ApiModel(description = "Arguments used to configure a cluster for Kerberos.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiConfigureForKerberosArguments   {
  @JsonProperty("datanodeTransceiverPort")
  private Integer datanodeTransceiverPort = null;

  @JsonProperty("datanodeWebPort")
  private Integer datanodeWebPort = null;

  public ApiConfigureForKerberosArguments datanodeTransceiverPort(Integer datanodeTransceiverPort) {
    this.datanodeTransceiverPort = datanodeTransceiverPort;
    return this;
  }

  /**
   * The HDFS DataNode transceiver port to use. This will be applied to all DataNode role configuration groups. If not specified, this will default to 1004.
   * @return datanodeTransceiverPort
  **/
  @ApiModelProperty(value = "The HDFS DataNode transceiver port to use. This will be applied to all DataNode role configuration groups. If not specified, this will default to 1004.")


  public Integer getDatanodeTransceiverPort() {
    return datanodeTransceiverPort;
  }

  public void setDatanodeTransceiverPort(Integer datanodeTransceiverPort) {
    this.datanodeTransceiverPort = datanodeTransceiverPort;
  }

  public ApiConfigureForKerberosArguments datanodeWebPort(Integer datanodeWebPort) {
    this.datanodeWebPort = datanodeWebPort;
    return this;
  }

  /**
   * The HDFS DataNode web port to use.  This will be applied to all DataNode role configuration groups. If not specified, this will default to 1006.
   * @return datanodeWebPort
  **/
  @ApiModelProperty(value = "The HDFS DataNode web port to use.  This will be applied to all DataNode role configuration groups. If not specified, this will default to 1006.")


  public Integer getDatanodeWebPort() {
    return datanodeWebPort;
  }

  public void setDatanodeWebPort(Integer datanodeWebPort) {
    this.datanodeWebPort = datanodeWebPort;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiConfigureForKerberosArguments apiConfigureForKerberosArguments = (ApiConfigureForKerberosArguments) o;
    return Objects.equals(this.datanodeTransceiverPort, apiConfigureForKerberosArguments.datanodeTransceiverPort) &&
        Objects.equals(this.datanodeWebPort, apiConfigureForKerberosArguments.datanodeWebPort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datanodeTransceiverPort, datanodeWebPort);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiConfigureForKerberosArguments {\n");
    
    sb.append("    datanodeTransceiverPort: ").append(toIndentedString(datanodeTransceiverPort)).append("\n");
    sb.append("    datanodeWebPort: ").append(toIndentedString(datanodeWebPort)).append("\n");
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

