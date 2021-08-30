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
 * During import time information related to all the non-base config groups must be provided.
 */
@ApiModel(description = "During import time information related to all the non-base config groups must be provided.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterTemplateRoleConfigGroupInfo   {
  @JsonProperty("rcgRefName")
  private String rcgRefName = null;

  @JsonProperty("name")
  private String name = null;

  public ApiClusterTemplateRoleConfigGroupInfo rcgRefName(String rcgRefName) {
    this.rcgRefName = rcgRefName;
    return this;
  }

  /**
   * Role config group reference name. This much match the reference name from the template.
   * @return rcgRefName
  **/
  @ApiModelProperty(value = "Role config group reference name. This much match the reference name from the template.")


  public String getRcgRefName() {
    return rcgRefName;
  }

  public void setRcgRefName(String rcgRefName) {
    this.rcgRefName = rcgRefName;
  }

  public ApiClusterTemplateRoleConfigGroupInfo name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Role config group name.
   * @return name
  **/
  @ApiModelProperty(value = "Role config group name.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterTemplateRoleConfigGroupInfo apiClusterTemplateRoleConfigGroupInfo = (ApiClusterTemplateRoleConfigGroupInfo) o;
    return Objects.equals(this.rcgRefName, apiClusterTemplateRoleConfigGroupInfo.rcgRefName) &&
        Objects.equals(this.name, apiClusterTemplateRoleConfigGroupInfo.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rcgRefName, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplateRoleConfigGroupInfo {\n");
    
    sb.append("    rcgRefName: ").append(toIndentedString(rcgRefName)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

