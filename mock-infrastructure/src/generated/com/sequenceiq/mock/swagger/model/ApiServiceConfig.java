package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiConfig;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiRoleTypeConfig;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Service and role type configuration.
 */
@ApiModel(description = "Service and role type configuration.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiServiceConfig extends ApiConfigList  {
  @JsonProperty("roleTypeConfigs")
  @Valid
  private List<ApiRoleTypeConfig> roleTypeConfigs = null;

  public ApiServiceConfig roleTypeConfigs(List<ApiRoleTypeConfig> roleTypeConfigs) {
    this.roleTypeConfigs = roleTypeConfigs;
    return this;
  }

  public ApiServiceConfig addRoleTypeConfigsItem(ApiRoleTypeConfig roleTypeConfigsItem) {
    if (this.roleTypeConfigs == null) {
      this.roleTypeConfigs = new ArrayList<>();
    }
    this.roleTypeConfigs.add(roleTypeConfigsItem);
    return this;
  }

  /**
   * List of role type configurations. Only available up to API v2.
   * @return roleTypeConfigs
  **/
  @ApiModelProperty(value = "List of role type configurations. Only available up to API v2.")

  @Valid

  public List<ApiRoleTypeConfig> getRoleTypeConfigs() {
    return roleTypeConfigs;
  }

  public void setRoleTypeConfigs(List<ApiRoleTypeConfig> roleTypeConfigs) {
    this.roleTypeConfigs = roleTypeConfigs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiServiceConfig apiServiceConfig = (ApiServiceConfig) o;
    return Objects.equals(this.roleTypeConfigs, apiServiceConfig.roleTypeConfigs) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleTypeConfigs, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiServiceConfig {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    roleTypeConfigs: ").append(toIndentedString(roleTypeConfigs)).append("\n");
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

