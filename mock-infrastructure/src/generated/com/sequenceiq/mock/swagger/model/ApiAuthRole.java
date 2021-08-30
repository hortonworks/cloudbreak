package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleRef;
import com.sequenceiq.mock.swagger.model.ApiClusterRef;
import com.sequenceiq.mock.swagger.model.ApiExternalUserMappingRef;
import com.sequenceiq.mock.swagger.model.ApiUser2Ref;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This is the model for user role scope in the API since v18. This is used to support granular permissions.
 */
@ApiModel(description = "This is the model for user role scope in the API since v18. This is used to support granular permissions.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiAuthRole   {
  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("clusters")
  @Valid
  private List<ApiClusterRef> clusters = null;

  @JsonProperty("users")
  @Valid
  private List<ApiUser2Ref> users = null;

  @JsonProperty("externalUserMappings")
  @Valid
  private List<ApiExternalUserMappingRef> externalUserMappings = null;

  @JsonProperty("baseRole")
  private ApiAuthRoleRef baseRole = null;

  @JsonProperty("uuid")
  private String uuid = null;

  @JsonProperty("isCustom")
  private Boolean isCustom = null;

  public ApiAuthRole displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * 
   * @return displayName
  **/
  @ApiModelProperty(value = "")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiAuthRole name(String name) {
    this.name = name;
    return this;
  }

  /**
   * 
   * @return name
  **/
  @ApiModelProperty(value = "")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiAuthRole clusters(List<ApiClusterRef> clusters) {
    this.clusters = clusters;
    return this;
  }

  public ApiAuthRole addClustersItem(ApiClusterRef clustersItem) {
    if (this.clusters == null) {
      this.clusters = new ArrayList<>();
    }
    this.clusters.add(clustersItem);
    return this;
  }

  /**
   * 
   * @return clusters
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiClusterRef> getClusters() {
    return clusters;
  }

  public void setClusters(List<ApiClusterRef> clusters) {
    this.clusters = clusters;
  }

  public ApiAuthRole users(List<ApiUser2Ref> users) {
    this.users = users;
    return this;
  }

  public ApiAuthRole addUsersItem(ApiUser2Ref usersItem) {
    if (this.users == null) {
      this.users = new ArrayList<>();
    }
    this.users.add(usersItem);
    return this;
  }

  /**
   * 
   * @return users
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiUser2Ref> getUsers() {
    return users;
  }

  public void setUsers(List<ApiUser2Ref> users) {
    this.users = users;
  }

  public ApiAuthRole externalUserMappings(List<ApiExternalUserMappingRef> externalUserMappings) {
    this.externalUserMappings = externalUserMappings;
    return this;
  }

  public ApiAuthRole addExternalUserMappingsItem(ApiExternalUserMappingRef externalUserMappingsItem) {
    if (this.externalUserMappings == null) {
      this.externalUserMappings = new ArrayList<>();
    }
    this.externalUserMappings.add(externalUserMappingsItem);
    return this;
  }

  /**
   * 
   * @return externalUserMappings
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiExternalUserMappingRef> getExternalUserMappings() {
    return externalUserMappings;
  }

  public void setExternalUserMappings(List<ApiExternalUserMappingRef> externalUserMappings) {
    this.externalUserMappings = externalUserMappings;
  }

  public ApiAuthRole baseRole(ApiAuthRoleRef baseRole) {
    this.baseRole = baseRole;
    return this;
  }

  /**
   * A role this user possesses. In Cloudera Enterprise Datahub Edition, possible values are: <ul> <li><b>ROLE_ADMIN</b></li> <li><b>ROLE_USER</b></li> <li><b>ROLE_LIMITED</b>: Added in Cloudera Manager 5.0</li> <li><b>ROLE_OPERATOR</b>: Added in Cloudera Manager 5.1</li> <li><b>ROLE_CONFIGURATOR</b>: Added in Cloudera Manager 5.1</li> <li><b>ROLE_CLUSTER_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_BDR_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_NAVIGATOR_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_USER_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_KEY_ADMIN</b>: Added in Cloudera Manager 5.5</li> </ul> An empty role implies ROLE_USER. <p>
   * @return baseRole
  **/
  @ApiModelProperty(value = "A role this user possesses. In Cloudera Enterprise Datahub Edition, possible values are: <ul> <li><b>ROLE_ADMIN</b></li> <li><b>ROLE_USER</b></li> <li><b>ROLE_LIMITED</b>: Added in Cloudera Manager 5.0</li> <li><b>ROLE_OPERATOR</b>: Added in Cloudera Manager 5.1</li> <li><b>ROLE_CONFIGURATOR</b>: Added in Cloudera Manager 5.1</li> <li><b>ROLE_CLUSTER_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_BDR_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_NAVIGATOR_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_USER_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_KEY_ADMIN</b>: Added in Cloudera Manager 5.5</li> </ul> An empty role implies ROLE_USER. <p>")

  @Valid

  public ApiAuthRoleRef getBaseRole() {
    return baseRole;
  }

  public void setBaseRole(ApiAuthRoleRef baseRole) {
    this.baseRole = baseRole;
  }

  public ApiAuthRole uuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * Readonly. The UUID of the authRole. <p>
   * @return uuid
  **/
  @ApiModelProperty(value = "Readonly. The UUID of the authRole. <p>")


  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public ApiAuthRole isCustom(Boolean isCustom) {
    this.isCustom = isCustom;
    return this;
  }

  /**
   * 
   * @return isCustom
  **/
  @ApiModelProperty(value = "")


  public Boolean isIsCustom() {
    return isCustom;
  }

  public void setIsCustom(Boolean isCustom) {
    this.isCustom = isCustom;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiAuthRole apiAuthRole = (ApiAuthRole) o;
    return Objects.equals(this.displayName, apiAuthRole.displayName) &&
        Objects.equals(this.name, apiAuthRole.name) &&
        Objects.equals(this.clusters, apiAuthRole.clusters) &&
        Objects.equals(this.users, apiAuthRole.users) &&
        Objects.equals(this.externalUserMappings, apiAuthRole.externalUserMappings) &&
        Objects.equals(this.baseRole, apiAuthRole.baseRole) &&
        Objects.equals(this.uuid, apiAuthRole.uuid) &&
        Objects.equals(this.isCustom, apiAuthRole.isCustom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayName, name, clusters, users, externalUserMappings, baseRole, uuid, isCustom);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiAuthRole {\n");
    
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    clusters: ").append(toIndentedString(clusters)).append("\n");
    sb.append("    users: ").append(toIndentedString(users)).append("\n");
    sb.append("    externalUserMappings: ").append(toIndentedString(externalUserMappings)).append("\n");
    sb.append("    baseRole: ").append(toIndentedString(baseRole)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    isCustom: ").append(toIndentedString(isCustom)).append("\n");
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

