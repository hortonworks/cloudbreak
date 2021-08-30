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
 * Role info: This will contain information related to a role referred by some configuration. During import type this role must be materizalized.
 */
@ApiModel(description = "Role info: This will contain information related to a role referred by some configuration. During import type this role must be materizalized.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterTemplateRole   {
  @JsonProperty("refName")
  private String refName = null;

  @JsonProperty("roleType")
  private String roleType = null;

  public ApiClusterTemplateRole refName(String refName) {
    this.refName = refName;
    return this;
  }

  /**
   * Role reference name
   * @return refName
  **/
  @ApiModelProperty(value = "Role reference name")


  public String getRefName() {
    return refName;
  }

  public void setRefName(String refName) {
    this.refName = refName;
  }

  public ApiClusterTemplateRole roleType(String roleType) {
    this.roleType = roleType;
    return this;
  }

  /**
   * Role type
   * @return roleType
  **/
  @ApiModelProperty(value = "Role type")


  public String getRoleType() {
    return roleType;
  }

  public void setRoleType(String roleType) {
    this.roleType = roleType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterTemplateRole apiClusterTemplateRole = (ApiClusterTemplateRole) o;
    return Objects.equals(this.refName, apiClusterTemplateRole.refName) &&
        Objects.equals(this.roleType, apiClusterTemplateRole.roleType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(refName, roleType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplateRole {\n");
    
    sb.append("    refName: ").append(toIndentedString(refName)).append("\n");
    sb.append("    roleType: ").append(toIndentedString(roleType)).append("\n");
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

