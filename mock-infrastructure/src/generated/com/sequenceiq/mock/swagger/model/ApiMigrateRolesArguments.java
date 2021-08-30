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
 * 
 */
@ApiModel(description = "")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiMigrateRolesArguments   {
  @JsonProperty("roleNamesToMigrate")
  @Valid
  private List<String> roleNamesToMigrate = null;

  @JsonProperty("destinationHostId")
  private String destinationHostId = null;

  @JsonProperty("clearStaleRoleData")
  private Boolean clearStaleRoleData = null;

  public ApiMigrateRolesArguments roleNamesToMigrate(List<String> roleNamesToMigrate) {
    this.roleNamesToMigrate = roleNamesToMigrate;
    return this;
  }

  public ApiMigrateRolesArguments addRoleNamesToMigrateItem(String roleNamesToMigrateItem) {
    if (this.roleNamesToMigrate == null) {
      this.roleNamesToMigrate = new ArrayList<>();
    }
    this.roleNamesToMigrate.add(roleNamesToMigrateItem);
    return this;
  }

  /**
   * The list of role names to migrate.
   * @return roleNamesToMigrate
  **/
  @ApiModelProperty(value = "The list of role names to migrate.")


  public List<String> getRoleNamesToMigrate() {
    return roleNamesToMigrate;
  }

  public void setRoleNamesToMigrate(List<String> roleNamesToMigrate) {
    this.roleNamesToMigrate = roleNamesToMigrate;
  }

  public ApiMigrateRolesArguments destinationHostId(String destinationHostId) {
    this.destinationHostId = destinationHostId;
    return this;
  }

  /**
   * The ID of the host to which the roles should be migrated.
   * @return destinationHostId
  **/
  @ApiModelProperty(value = "The ID of the host to which the roles should be migrated.")


  public String getDestinationHostId() {
    return destinationHostId;
  }

  public void setDestinationHostId(String destinationHostId) {
    this.destinationHostId = destinationHostId;
  }

  public ApiMigrateRolesArguments clearStaleRoleData(Boolean clearStaleRoleData) {
    this.clearStaleRoleData = clearStaleRoleData;
    return this;
  }

  /**
   * Delete existing stale role data, if any. For example, when migrating a NameNode, if the destination host has stale data in the NameNode data directories (possibly because a NameNode role was previously located there), this stale data will be deleted before migrating the role. Defaults to false.
   * @return clearStaleRoleData
  **/
  @ApiModelProperty(value = "Delete existing stale role data, if any. For example, when migrating a NameNode, if the destination host has stale data in the NameNode data directories (possibly because a NameNode role was previously located there), this stale data will be deleted before migrating the role. Defaults to false.")


  public Boolean isClearStaleRoleData() {
    return clearStaleRoleData;
  }

  public void setClearStaleRoleData(Boolean clearStaleRoleData) {
    this.clearStaleRoleData = clearStaleRoleData;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiMigrateRolesArguments apiMigrateRolesArguments = (ApiMigrateRolesArguments) o;
    return Objects.equals(this.roleNamesToMigrate, apiMigrateRolesArguments.roleNamesToMigrate) &&
        Objects.equals(this.destinationHostId, apiMigrateRolesArguments.destinationHostId) &&
        Objects.equals(this.clearStaleRoleData, apiMigrateRolesArguments.clearStaleRoleData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleNamesToMigrate, destinationHostId, clearStaleRoleData);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiMigrateRolesArguments {\n");
    
    sb.append("    roleNamesToMigrate: ").append(toIndentedString(roleNamesToMigrate)).append("\n");
    sb.append("    destinationHostId: ").append(toIndentedString(destinationHostId)).append("\n");
    sb.append("    clearStaleRoleData: ").append(toIndentedString(clearStaleRoleData)).append("\n");
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

