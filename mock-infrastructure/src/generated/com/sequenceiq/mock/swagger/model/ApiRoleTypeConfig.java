package com.sequenceiq.mock.swagger.model;

import java.util.Objects;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Role type configuration information.
 */
@ApiModel(description = "Role type configuration information.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRoleTypeConfig extends ApiConfigList  {
  @JsonProperty("roleType")
  private String roleType = null;

  public ApiRoleTypeConfig roleType(String roleType) {
    this.roleType = roleType;
    return this;
  }

  /**
   * The role type.
   * @return roleType
  **/
  @ApiModelProperty(value = "The role type.")


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
    ApiRoleTypeConfig apiRoleTypeConfig = (ApiRoleTypeConfig) o;
    return Objects.equals(this.roleType, apiRoleTypeConfig.roleType) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleType, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRoleTypeConfig {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
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

