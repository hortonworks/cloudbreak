package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleRef;
import com.sequenceiq.mock.swagger.model.ApiExternalUserMappingType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This is the model for external user mapping information in the API, v19 and beyond. These can be of 4 types : LDAP group, SAML, SAML attribute and External Script. &lt;p&gt;
 */
@ApiModel(description = "This is the model for external user mapping information in the API, v19 and beyond. These can be of 4 types : LDAP group, SAML, SAML attribute and External Script. <p>")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiExternalUserMapping   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("type")
  private ApiExternalUserMappingType type = null;

  @JsonProperty("uuid")
  private String uuid = null;

  @JsonProperty("authRoles")
  @Valid
  private List<ApiAuthRoleRef> authRoles = null;

  public ApiExternalUserMapping name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the external mapping
   * @return name
  **/
  @ApiModelProperty(value = "The name of the external mapping")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiExternalUserMapping type(ApiExternalUserMappingType type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the external mapping
   * @return type
  **/
  @ApiModelProperty(value = "The type of the external mapping")

  @Valid

  public ApiExternalUserMappingType getType() {
    return type;
  }

  public void setType(ApiExternalUserMappingType type) {
    this.type = type;
  }

  public ApiExternalUserMapping uuid(String uuid) {
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

  public ApiExternalUserMapping authRoles(List<ApiAuthRoleRef> authRoles) {
    this.authRoles = authRoles;
    return this;
  }

  public ApiExternalUserMapping addAuthRolesItem(ApiAuthRoleRef authRolesItem) {
    if (this.authRoles == null) {
      this.authRoles = new ArrayList<>();
    }
    this.authRoles.add(authRolesItem);
    return this;
  }

  /**
   * A list of ApiAuthRole that this user possesses.  Each custom role with be a built-in role with a set of scopes. ApiAuthRole is the model for specifying custom roles. Only admins and user admins can create/delete/update external user mappings.
   * @return authRoles
  **/
  @ApiModelProperty(value = "A list of ApiAuthRole that this user possesses.  Each custom role with be a built-in role with a set of scopes. ApiAuthRole is the model for specifying custom roles. Only admins and user admins can create/delete/update external user mappings.")

  @Valid

  public List<ApiAuthRoleRef> getAuthRoles() {
    return authRoles;
  }

  public void setAuthRoles(List<ApiAuthRoleRef> authRoles) {
    this.authRoles = authRoles;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiExternalUserMapping apiExternalUserMapping = (ApiExternalUserMapping) o;
    return Objects.equals(this.name, apiExternalUserMapping.name) &&
        Objects.equals(this.type, apiExternalUserMapping.type) &&
        Objects.equals(this.uuid, apiExternalUserMapping.uuid) &&
        Objects.equals(this.authRoles, apiExternalUserMapping.authRoles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, uuid, authRoles);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiExternalUserMapping {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    authRoles: ").append(toIndentedString(authRoles)).append("\n");
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

