package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A process represents a unix process to be managed by the Cloudera Manager agents. A process can be a daemon, e.g. if it is associated with a running role. It can also be a one-off process which is expected to start, run and finish.
 */
@ApiModel(description = "A process represents a unix process to be managed by the Cloudera Manager agents. A process can be a daemon, e.g. if it is associated with a running role. It can also be a one-off process which is expected to start, run and finish.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiProcess   {
  @JsonProperty("configFiles")
  @Valid
  private List<String> configFiles = null;

  public ApiProcess configFiles(List<String> configFiles) {
    this.configFiles = configFiles;
    return this;
  }

  public ApiProcess addConfigFilesItem(String configFilesItem) {
    if (this.configFiles == null) {
      this.configFiles = new ArrayList<>();
    }
    this.configFiles.add(configFilesItem);
    return this;
  }

  /**
   * List of config files supplied to the process.
   * @return configFiles
  **/
  @ApiModelProperty(value = "List of config files supplied to the process.")


  public List<String> getConfigFiles() {
    return configFiles;
  }

  public void setConfigFiles(List<String> configFiles) {
    this.configFiles = configFiles;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiProcess apiProcess = (ApiProcess) o;
    return Objects.equals(this.configFiles, apiProcess.configFiles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configFiles);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiProcess {\n");
    
    sb.append("    configFiles: ").append(toIndentedString(configFiles)).append("\n");
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

