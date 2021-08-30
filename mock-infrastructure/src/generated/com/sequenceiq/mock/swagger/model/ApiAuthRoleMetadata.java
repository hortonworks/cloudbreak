package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleAuthority;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This is the model for auth role metadata
 */
@ApiModel(description = "This is the model for auth role metadata")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiAuthRoleMetadata   {
  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("uuid")
  private String uuid = null;

  @JsonProperty("role")
  private String role = null;

  @JsonProperty("authorities")
  @Valid
  private List<ApiAuthRoleAuthority> authorities = null;

  @JsonProperty("allowedScopes")
  @Valid
  private List<String> allowedScopes = null;

  public ApiAuthRoleMetadata displayName(String displayName) {
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

  public ApiAuthRoleMetadata uuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * 
   * @return uuid
  **/
  @ApiModelProperty(value = "")


  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public ApiAuthRoleMetadata role(String role) {
    this.role = role;
    return this;
  }

  /**
   * 
   * @return role
  **/
  @ApiModelProperty(value = "")


  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public ApiAuthRoleMetadata authorities(List<ApiAuthRoleAuthority> authorities) {
    this.authorities = authorities;
    return this;
  }

  public ApiAuthRoleMetadata addAuthoritiesItem(ApiAuthRoleAuthority authoritiesItem) {
    if (this.authorities == null) {
      this.authorities = new ArrayList<>();
    }
    this.authorities.add(authoritiesItem);
    return this;
  }

  /**
   * 
   * @return authorities
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiAuthRoleAuthority> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(List<ApiAuthRoleAuthority> authorities) {
    this.authorities = authorities;
  }

  public ApiAuthRoleMetadata allowedScopes(List<String> allowedScopes) {
    this.allowedScopes = allowedScopes;
    return this;
  }

  public ApiAuthRoleMetadata addAllowedScopesItem(String allowedScopesItem) {
    if (this.allowedScopes == null) {
      this.allowedScopes = new ArrayList<>();
    }
    this.allowedScopes.add(allowedScopesItem);
    return this;
  }

  /**
   * 
   * @return allowedScopes
  **/
  @ApiModelProperty(value = "")


  public List<String> getAllowedScopes() {
    return allowedScopes;
  }

  public void setAllowedScopes(List<String> allowedScopes) {
    this.allowedScopes = allowedScopes;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiAuthRoleMetadata apiAuthRoleMetadata = (ApiAuthRoleMetadata) o;
    return Objects.equals(this.displayName, apiAuthRoleMetadata.displayName) &&
        Objects.equals(this.uuid, apiAuthRoleMetadata.uuid) &&
        Objects.equals(this.role, apiAuthRoleMetadata.role) &&
        Objects.equals(this.authorities, apiAuthRoleMetadata.authorities) &&
        Objects.equals(this.allowedScopes, apiAuthRoleMetadata.allowedScopes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayName, uuid, role, authorities, allowedScopes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiAuthRoleMetadata {\n");
    
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    authorities: ").append(toIndentedString(authorities)).append("\n");
    sb.append("    allowedScopes: ").append(toIndentedString(allowedScopes)).append("\n");
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

