package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiParcelRef;
import com.sequenceiq.mock.swagger.model.ApiRoleRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This object is used to represent a role within an ApiParcelUsage.
 */
@ApiModel(description = "This object is used to represent a role within an ApiParcelUsage.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiParcelUsageRole   {
  @JsonProperty("roleRef")
  private ApiRoleRef roleRef = null;

  @JsonProperty("parcelRefs")
  @Valid
  private List<ApiParcelRef> parcelRefs = null;

  public ApiParcelUsageRole roleRef(ApiRoleRef roleRef) {
    this.roleRef = roleRef;
    return this;
  }

  /**
   * A reference to the corresponding Role object.
   * @return roleRef
  **/
  @ApiModelProperty(value = "A reference to the corresponding Role object.")

  @Valid

  public ApiRoleRef getRoleRef() {
    return roleRef;
  }

  public void setRoleRef(ApiRoleRef roleRef) {
    this.roleRef = roleRef;
  }

  public ApiParcelUsageRole parcelRefs(List<ApiParcelRef> parcelRefs) {
    this.parcelRefs = parcelRefs;
    return this;
  }

  public ApiParcelUsageRole addParcelRefsItem(ApiParcelRef parcelRefsItem) {
    if (this.parcelRefs == null) {
      this.parcelRefs = new ArrayList<>();
    }
    this.parcelRefs.add(parcelRefsItem);
    return this;
  }

  /**
   * A collection of references to the parcels being used by the role.
   * @return parcelRefs
  **/
  @ApiModelProperty(value = "A collection of references to the parcels being used by the role.")

  @Valid

  public List<ApiParcelRef> getParcelRefs() {
    return parcelRefs;
  }

  public void setParcelRefs(List<ApiParcelRef> parcelRefs) {
    this.parcelRefs = parcelRefs;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiParcelUsageRole apiParcelUsageRole = (ApiParcelUsageRole) o;
    return Objects.equals(this.roleRef, apiParcelUsageRole.roleRef) &&
        Objects.equals(this.parcelRefs, apiParcelUsageRole.parcelRefs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleRef, parcelRefs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiParcelUsageRole {\n");
    
    sb.append("    roleRef: ").append(toIndentedString(roleRef)).append("\n");
    sb.append("    parcelRefs: ").append(toIndentedString(parcelRefs)).append("\n");
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

