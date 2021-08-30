package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateRole;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.sequenceiq.mock.swagger.model.ApiEntityTag;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Service information
 */
@ApiModel(description = "Service information")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterTemplateService   {
  @JsonProperty("refName")
  private String refName = null;

  @JsonProperty("serviceType")
  private String serviceType = null;

  @JsonProperty("serviceConfigs")
  @Valid
  private List<ApiClusterTemplateConfig> serviceConfigs = null;

  @JsonProperty("roleConfigGroups")
  @Valid
  private List<ApiClusterTemplateRoleConfigGroup> roleConfigGroups = null;

  @JsonProperty("roles")
  @Valid
  private List<ApiClusterTemplateRole> roles = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("tags")
  @Valid
  private List<ApiEntityTag> tags = null;

  public ApiClusterTemplateService refName(String refName) {
    this.refName = refName;
    return this;
  }

  /**
   * Reference name of the service. This could be referred by some configuration.
   * @return refName
  **/
  @ApiModelProperty(value = "Reference name of the service. This could be referred by some configuration.")


  public String getRefName() {
    return refName;
  }

  public void setRefName(String refName) {
    this.refName = refName;
  }

  public ApiClusterTemplateService serviceType(String serviceType) {
    this.serviceType = serviceType;
    return this;
  }

  /**
   * Service type
   * @return serviceType
  **/
  @ApiModelProperty(value = "Service type")


  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public ApiClusterTemplateService serviceConfigs(List<ApiClusterTemplateConfig> serviceConfigs) {
    this.serviceConfigs = serviceConfigs;
    return this;
  }

  public ApiClusterTemplateService addServiceConfigsItem(ApiClusterTemplateConfig serviceConfigsItem) {
    if (this.serviceConfigs == null) {
      this.serviceConfigs = new ArrayList<>();
    }
    this.serviceConfigs.add(serviceConfigsItem);
    return this;
  }

  /**
   * Service level configuration
   * @return serviceConfigs
  **/
  @ApiModelProperty(value = "Service level configuration")

  @Valid

  public List<ApiClusterTemplateConfig> getServiceConfigs() {
    return serviceConfigs;
  }

  public void setServiceConfigs(List<ApiClusterTemplateConfig> serviceConfigs) {
    this.serviceConfigs = serviceConfigs;
  }

  public ApiClusterTemplateService roleConfigGroups(List<ApiClusterTemplateRoleConfigGroup> roleConfigGroups) {
    this.roleConfigGroups = roleConfigGroups;
    return this;
  }

  public ApiClusterTemplateService addRoleConfigGroupsItem(ApiClusterTemplateRoleConfigGroup roleConfigGroupsItem) {
    if (this.roleConfigGroups == null) {
      this.roleConfigGroups = new ArrayList<>();
    }
    this.roleConfigGroups.add(roleConfigGroupsItem);
    return this;
  }

  /**
   * All role config groups for that service
   * @return roleConfigGroups
  **/
  @ApiModelProperty(value = "All role config groups for that service")

  @Valid

  public List<ApiClusterTemplateRoleConfigGroup> getRoleConfigGroups() {
    return roleConfigGroups;
  }

  public void setRoleConfigGroups(List<ApiClusterTemplateRoleConfigGroup> roleConfigGroups) {
    this.roleConfigGroups = roleConfigGroups;
  }

  public ApiClusterTemplateService roles(List<ApiClusterTemplateRole> roles) {
    this.roles = roles;
    return this;
  }

  public ApiClusterTemplateService addRolesItem(ApiClusterTemplateRole rolesItem) {
    if (this.roles == null) {
      this.roles = new ArrayList<>();
    }
    this.roles.add(rolesItem);
    return this;
  }

  /**
   * List of roles for this service that are referred by some configuration.
   * @return roles
  **/
  @ApiModelProperty(value = "List of roles for this service that are referred by some configuration.")

  @Valid

  public List<ApiClusterTemplateRole> getRoles() {
    return roles;
  }

  public void setRoles(List<ApiClusterTemplateRole> roles) {
    this.roles = roles;
  }

  public ApiClusterTemplateService displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Service display name.
   * @return displayName
  **/
  @ApiModelProperty(value = "Service display name.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiClusterTemplateService tags(List<ApiEntityTag> tags) {
    this.tags = tags;
    return this;
  }

  public ApiClusterTemplateService addTagsItem(ApiEntityTag tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Tags associated with the service
   * @return tags
  **/
  @ApiModelProperty(value = "Tags associated with the service")

  @Valid

  public List<ApiEntityTag> getTags() {
    return tags;
  }

  public void setTags(List<ApiEntityTag> tags) {
    this.tags = tags;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterTemplateService apiClusterTemplateService = (ApiClusterTemplateService) o;
    return Objects.equals(this.refName, apiClusterTemplateService.refName) &&
        Objects.equals(this.serviceType, apiClusterTemplateService.serviceType) &&
        Objects.equals(this.serviceConfigs, apiClusterTemplateService.serviceConfigs) &&
        Objects.equals(this.roleConfigGroups, apiClusterTemplateService.roleConfigGroups) &&
        Objects.equals(this.roles, apiClusterTemplateService.roles) &&
        Objects.equals(this.displayName, apiClusterTemplateService.displayName) &&
        Objects.equals(this.tags, apiClusterTemplateService.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(refName, serviceType, serviceConfigs, roleConfigGroups, roles, displayName, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplateService {\n");
    
    sb.append("    refName: ").append(toIndentedString(refName)).append("\n");
    sb.append("    serviceType: ").append(toIndentedString(serviceType)).append("\n");
    sb.append("    serviceConfigs: ").append(toIndentedString(serviceConfigs)).append("\n");
    sb.append("    roleConfigGroups: ").append(toIndentedString(roleConfigGroups)).append("\n");
    sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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

