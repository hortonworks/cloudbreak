package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHealthSummary;
import com.sequenceiq.mock.swagger.model.ApiRoleState;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A roleRef references a role. Each role is identified by its \&quot;roleName\&quot;, the \&quot;serviceName\&quot; for the service it belongs to, and the \&quot;clusterName\&quot; in which the service resides. To operate on the role object, use the API with the those fields as parameters.
 */
@ApiModel(description = "A roleRef references a role. Each role is identified by its \"roleName\", the \"serviceName\" for the service it belongs to, and the \"clusterName\" in which the service resides. To operate on the role object, use the API with the those fields as parameters.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRoleRef   {
  @JsonProperty("clusterName")
  private String clusterName = null;

  @JsonProperty("serviceName")
  private String serviceName = null;

  @JsonProperty("roleName")
  private String roleName = null;

  @JsonProperty("healthSummary")
  private ApiHealthSummary healthSummary = null;

  @JsonProperty("roleStatus")
  private ApiRoleState roleStatus = null;

  public ApiRoleRef clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  /**
   * 
   * @return clusterName
  **/
  @ApiModelProperty(value = "")


  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public ApiRoleRef serviceName(String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  /**
   * 
   * @return serviceName
  **/
  @ApiModelProperty(value = "")


  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public ApiRoleRef roleName(String roleName) {
    this.roleName = roleName;
    return this;
  }

  /**
   * 
   * @return roleName
  **/
  @ApiModelProperty(value = "")


  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public ApiRoleRef healthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
    return this;
  }

  /**
   * The high-level health status of this role.
   * @return healthSummary
  **/
  @ApiModelProperty(value = "The high-level health status of this role.")

  @Valid

  public ApiHealthSummary getHealthSummary() {
    return healthSummary;
  }

  public void setHealthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
  }

  public ApiRoleRef roleStatus(ApiRoleState roleStatus) {
    this.roleStatus = roleStatus;
    return this;
  }

  /**
   * Readonly. The entity status for this role. Available since API v11.
   * @return roleStatus
  **/
  @ApiModelProperty(value = "Readonly. The entity status for this role. Available since API v11.")

  @Valid

  public ApiRoleState getRoleStatus() {
    return roleStatus;
  }

  public void setRoleStatus(ApiRoleState roleStatus) {
    this.roleStatus = roleStatus;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiRoleRef apiRoleRef = (ApiRoleRef) o;
    return Objects.equals(this.clusterName, apiRoleRef.clusterName) &&
        Objects.equals(this.serviceName, apiRoleRef.serviceName) &&
        Objects.equals(this.roleName, apiRoleRef.roleName) &&
        Objects.equals(this.healthSummary, apiRoleRef.healthSummary) &&
        Objects.equals(this.roleStatus, apiRoleRef.roleStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterName, serviceName, roleName, healthSummary, roleStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRoleRef {\n");
    
    sb.append("    clusterName: ").append(toIndentedString(clusterName)).append("\n");
    sb.append("    serviceName: ").append(toIndentedString(serviceName)).append("\n");
    sb.append("    roleName: ").append(toIndentedString(roleName)).append("\n");
    sb.append("    healthSummary: ").append(toIndentedString(healthSummary)).append("\n");
    sb.append("    roleStatus: ").append(toIndentedString(roleStatus)).append("\n");
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

