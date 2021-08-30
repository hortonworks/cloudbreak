package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiClusterRef;
import com.sequenceiq.mock.swagger.model.ApiRoleConfigGroupRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A host template belongs to a cluster and contains a set of role config groups for slave roles (such as DataNodes and TaskTrackers) from services in the cluster. At most one role config group per role type can be present in a host template. Host templates can be applied to fresh hosts (those with no roles on them) in order to create a role for each of the role groups on each host.
 */
@ApiModel(description = "A host template belongs to a cluster and contains a set of role config groups for slave roles (such as DataNodes and TaskTrackers) from services in the cluster. At most one role config group per role type can be present in a host template. Host templates can be applied to fresh hosts (those with no roles on them) in order to create a role for each of the role groups on each host.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHostTemplate   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("clusterRef")
  private ApiClusterRef clusterRef = null;

  @JsonProperty("roleConfigGroupRefs")
  @Valid
  private List<ApiRoleConfigGroupRef> roleConfigGroupRefs = null;

  public ApiHostTemplate name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the host template. Unique across clusters.
   * @return name
  **/
  @ApiModelProperty(value = "The name of the host template. Unique across clusters.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiHostTemplate clusterRef(ApiClusterRef clusterRef) {
    this.clusterRef = clusterRef;
    return this;
  }

  /**
   * Readonly. A reference to the cluster the host template belongs to.
   * @return clusterRef
  **/
  @ApiModelProperty(value = "Readonly. A reference to the cluster the host template belongs to.")

  @Valid

  public ApiClusterRef getClusterRef() {
    return clusterRef;
  }

  public void setClusterRef(ApiClusterRef clusterRef) {
    this.clusterRef = clusterRef;
  }

  public ApiHostTemplate roleConfigGroupRefs(List<ApiRoleConfigGroupRef> roleConfigGroupRefs) {
    this.roleConfigGroupRefs = roleConfigGroupRefs;
    return this;
  }

  public ApiHostTemplate addRoleConfigGroupRefsItem(ApiRoleConfigGroupRef roleConfigGroupRefsItem) {
    if (this.roleConfigGroupRefs == null) {
      this.roleConfigGroupRefs = new ArrayList<>();
    }
    this.roleConfigGroupRefs.add(roleConfigGroupRefsItem);
    return this;
  }

  /**
   * The role config groups belonging to this host tempalte.
   * @return roleConfigGroupRefs
  **/
  @ApiModelProperty(value = "The role config groups belonging to this host tempalte.")

  @Valid

  public List<ApiRoleConfigGroupRef> getRoleConfigGroupRefs() {
    return roleConfigGroupRefs;
  }

  public void setRoleConfigGroupRefs(List<ApiRoleConfigGroupRef> roleConfigGroupRefs) {
    this.roleConfigGroupRefs = roleConfigGroupRefs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHostTemplate apiHostTemplate = (ApiHostTemplate) o;
    return Objects.equals(this.name, apiHostTemplate.name) &&
        Objects.equals(this.clusterRef, apiHostTemplate.clusterRef) &&
        Objects.equals(this.roleConfigGroupRefs, apiHostTemplate.roleConfigGroupRefs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, clusterRef, roleConfigGroupRefs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHostTemplate {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    clusterRef: ").append(toIndentedString(clusterRef)).append("\n");
    sb.append("    roleConfigGroupRefs: ").append(toIndentedString(roleConfigGroupRefs)).append("\n");
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

