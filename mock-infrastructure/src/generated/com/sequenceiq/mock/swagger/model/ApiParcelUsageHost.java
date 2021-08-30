package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiParcelUsageRole;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This object is used to represent a host within an ApiParcelUsage.
 */
@ApiModel(description = "This object is used to represent a host within an ApiParcelUsage.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiParcelUsageHost   {
  @JsonProperty("hostRef")
  private ApiHostRef hostRef = null;

  @JsonProperty("roles")
  @Valid
  private List<ApiParcelUsageRole> roles = null;

  public ApiParcelUsageHost hostRef(ApiHostRef hostRef) {
    this.hostRef = hostRef;
    return this;
  }

  /**
   * A reference to the corresponding Host object.
   * @return hostRef
  **/
  @ApiModelProperty(value = "A reference to the corresponding Host object.")

  @Valid

  public ApiHostRef getHostRef() {
    return hostRef;
  }

  public void setHostRef(ApiHostRef hostRef) {
    this.hostRef = hostRef;
  }

  public ApiParcelUsageHost roles(List<ApiParcelUsageRole> roles) {
    this.roles = roles;
    return this;
  }

  public ApiParcelUsageHost addRolesItem(ApiParcelUsageRole rolesItem) {
    if (this.roles == null) {
      this.roles = new ArrayList<>();
    }
    this.roles.add(rolesItem);
    return this;
  }

  /**
   * A collection of the roles present on the host.
   * @return roles
  **/
  @ApiModelProperty(value = "A collection of the roles present on the host.")

  @Valid

  public List<ApiParcelUsageRole> getRoles() {
    return roles;
  }

  public void setRoles(List<ApiParcelUsageRole> roles) {
    this.roles = roles;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiParcelUsageHost apiParcelUsageHost = (ApiParcelUsageHost) o;
    return Objects.equals(this.hostRef, apiParcelUsageHost.hostRef) &&
        Objects.equals(this.roles, apiParcelUsageHost.roles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostRef, roles);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiParcelUsageHost {\n");
    
    sb.append("    hostRef: ").append(toIndentedString(hostRef)).append("\n");
    sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
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

