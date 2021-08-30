package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A role config group contains roles of the same role type sharing the same configuration. While each role has to belong to a group, a role config group may be empty.  There exists a default role config group for each role type. Default groups cannot be removed nor created.  The name of a role config group is unique and cannot be changed.  The configuration of individual roles may be overridden on role level.
 */
@ApiModel(description = "A role config group contains roles of the same role type sharing the same configuration. While each role has to belong to a group, a role config group may be empty.  There exists a default role config group for each role type. Default groups cannot be removed nor created.  The name of a role config group is unique and cannot be changed.  The configuration of individual roles may be overridden on role level.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRoleConfigGroup   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("roleType")
  private String roleType = null;

  @JsonProperty("base")
  private Boolean base = null;

  @JsonProperty("config")
  private ApiConfigList config = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("serviceRef")
  private ApiServiceRef serviceRef = null;

  public ApiRoleConfigGroup name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Readonly. The unique name of this role config group.
   * @return name
  **/
  @ApiModelProperty(value = "Readonly. The unique name of this role config group.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiRoleConfigGroup roleType(String roleType) {
    this.roleType = roleType;
    return this;
  }

  /**
   * Readonly. The type of the roles in this group.
   * @return roleType
  **/
  @ApiModelProperty(value = "Readonly. The type of the roles in this group.")


  public String getRoleType() {
    return roleType;
  }

  public void setRoleType(String roleType) {
    this.roleType = roleType;
  }

  public ApiRoleConfigGroup base(Boolean base) {
    this.base = base;
    return this;
  }

  /**
   * Readonly. Indicates whether this is a base group.
   * @return base
  **/
  @ApiModelProperty(value = "Readonly. Indicates whether this is a base group.")


  public Boolean isBase() {
    return base;
  }

  public void setBase(Boolean base) {
    this.base = base;
  }

  public ApiRoleConfigGroup config(ApiConfigList config) {
    this.config = config;
    return this;
  }

  /**
   * The configuration for this group. Optional.
   * @return config
  **/
  @ApiModelProperty(value = "The configuration for this group. Optional.")

  @Valid

  public ApiConfigList getConfig() {
    return config;
  }

  public void setConfig(ApiConfigList config) {
    this.config = config;
  }

  public ApiRoleConfigGroup displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * The display name of this group.
   * @return displayName
  **/
  @ApiModelProperty(value = "The display name of this group.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiRoleConfigGroup serviceRef(ApiServiceRef serviceRef) {
    this.serviceRef = serviceRef;
    return this;
  }

  /**
   * Readonly. The service reference (service name and cluster name) of this group.
   * @return serviceRef
  **/
  @ApiModelProperty(value = "Readonly. The service reference (service name and cluster name) of this group.")

  @Valid

  public ApiServiceRef getServiceRef() {
    return serviceRef;
  }

  public void setServiceRef(ApiServiceRef serviceRef) {
    this.serviceRef = serviceRef;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiRoleConfigGroup apiRoleConfigGroup = (ApiRoleConfigGroup) o;
    return Objects.equals(this.name, apiRoleConfigGroup.name) &&
        Objects.equals(this.roleType, apiRoleConfigGroup.roleType) &&
        Objects.equals(this.base, apiRoleConfigGroup.base) &&
        Objects.equals(this.config, apiRoleConfigGroup.config) &&
        Objects.equals(this.displayName, apiRoleConfigGroup.displayName) &&
        Objects.equals(this.serviceRef, apiRoleConfigGroup.serviceRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, roleType, base, config, displayName, serviceRef);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRoleConfigGroup {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    roleType: ").append(toIndentedString(roleType)).append("\n");
    sb.append("    base: ").append(toIndentedString(base)).append("\n");
    sb.append("    config: ").append(toIndentedString(config)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    serviceRef: ").append(toIndentedString(serviceRef)).append("\n");
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

