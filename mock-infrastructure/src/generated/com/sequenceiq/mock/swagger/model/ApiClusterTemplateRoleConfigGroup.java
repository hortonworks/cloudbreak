package com.sequenceiq.mock.swagger.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Role config group info.
 */
@ApiModel(description = "Role config group info.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterTemplateRoleConfigGroup   {
  @JsonProperty("refName")
  private String refName = null;

  @JsonProperty("roleType")
  private String roleType = null;

  @JsonProperty("base")
  private Boolean base = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("configs")
  @Valid
  private List<ApiClusterTemplateConfig> configs = null;

  public ApiClusterTemplateRoleConfigGroup refName(String refName) {
    this.refName = refName;
    return this;
  }

  /**
   * The reference name of the role config.
   * @return refName
  **/
  @ApiModelProperty(value = "The reference name of the role config.")


  public String getRefName() {
    return refName;
  }

  public void setRefName(String refName) {
    this.refName = refName;
  }

  public ApiClusterTemplateRoleConfigGroup roleType(String roleType) {
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

  public ApiClusterTemplateRoleConfigGroup base(Boolean base) {
    this.base = base;
    return this;
  }

  /**
   * If true then it is the base config group for that role. There can only be one base role config group for a given role type. Defaults to false.
   * @return base
  **/
  @ApiModelProperty(value = "If true then it is the base config group for that role. There can only be one base role config group for a given role type. Defaults to false.")


  public Boolean isBase() {
    return base;
  }

  public void setBase(Boolean base) {
    this.base = base;
  }

  public ApiClusterTemplateRoleConfigGroup displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Role config group display name
   * @return displayName
  **/
  @ApiModelProperty(value = "Role config group display name")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiClusterTemplateRoleConfigGroup configs(List<ApiClusterTemplateConfig> configs) {
    this.configs = configs;
    return this;
  }

  public ApiClusterTemplateRoleConfigGroup addConfigsItem(ApiClusterTemplateConfig configsItem) {
    if (this.configs == null) {
      this.configs = new ArrayList<>();
    }
    this.configs.add(configsItem);
    return this;
  }

  /**
   * List of configurations
   * @return configs
  **/
  @ApiModelProperty(value = "List of configurations")

  @Valid

  public List<ApiClusterTemplateConfig> getConfigs() {
    return configs;
  }

  public void setConfigs(List<ApiClusterTemplateConfig> configs) {
    this.configs = configs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterTemplateRoleConfigGroup apiClusterTemplateRoleConfigGroup = (ApiClusterTemplateRoleConfigGroup) o;
    return Objects.equals(this.refName, apiClusterTemplateRoleConfigGroup.refName) &&
        Objects.equals(this.roleType, apiClusterTemplateRoleConfigGroup.roleType) &&
        Objects.equals(this.base, apiClusterTemplateRoleConfigGroup.base) &&
        Objects.equals(this.displayName, apiClusterTemplateRoleConfigGroup.displayName) &&
        Objects.equals(this.configs, apiClusterTemplateRoleConfigGroup.configs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(refName, roleType, base, displayName, configs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplateRoleConfigGroup {\n");
    
    sb.append("    refName: ").append(toIndentedString(refName)).append("\n");
    sb.append("    roleType: ").append(toIndentedString(roleType)).append("\n");
    sb.append("    base: ").append(toIndentedString(base)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    configs: ").append(toIndentedString(configs)).append("\n");
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

