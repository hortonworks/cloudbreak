package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This is the model for user information in the API, v18 and beyond. &lt;p&gt; Note that any method that returns user information will not contain any password information. The password property is only used when creating or updating users.
 */
@ApiModel(description = "This is the model for user information in the API, v18 and beyond. <p> Note that any method that returns user information will not contain any password information. The password property is only used when creating or updating users.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiUser2   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("password")
  private String password = null;

  @JsonProperty("authRoles")
  @Valid
  private List<ApiAuthRoleRef> authRoles = null;

  @JsonProperty("pwHash")
  private String pwHash = null;

  @JsonProperty("pwSalt")
  private Integer pwSalt = null;

  @JsonProperty("pwLogin")
  private Boolean pwLogin = null;

  public ApiUser2 name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The username, which is unique within a Cloudera Manager installation.
   * @return name
  **/
  @ApiModelProperty(value = "The username, which is unique within a Cloudera Manager installation.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiUser2 password(String password) {
    this.password = password;
    return this;
  }

  /**
   * Returns the user password. <p> Passwords are not returned when querying user information, so this property will always be empty when reading information from a server.
   * @return password
  **/
  @ApiModelProperty(value = "Returns the user password. <p> Passwords are not returned when querying user information, so this property will always be empty when reading information from a server.")


  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public ApiUser2 authRoles(List<ApiAuthRoleRef> authRoles) {
    this.authRoles = authRoles;
    return this;
  }

  public ApiUser2 addAuthRolesItem(ApiAuthRoleRef authRolesItem) {
    if (this.authRoles == null) {
      this.authRoles = new ArrayList<>();
    }
    this.authRoles.add(authRolesItem);
    return this;
  }

  /**
   * A list of ApiAuthRole that this user possesses.
   * @return authRoles
  **/
  @ApiModelProperty(value = "A list of ApiAuthRole that this user possesses.")

  @Valid

  public List<ApiAuthRoleRef> getAuthRoles() {
    return authRoles;
  }

  public void setAuthRoles(List<ApiAuthRoleRef> authRoles) {
    this.authRoles = authRoles;
  }

  public ApiUser2 pwHash(String pwHash) {
    this.pwHash = pwHash;
    return this;
  }

  /**
   * NOTE: Only available in the \"export\" view
   * @return pwHash
  **/
  @ApiModelProperty(value = "NOTE: Only available in the \"export\" view")


  public String getPwHash() {
    return pwHash;
  }

  public void setPwHash(String pwHash) {
    this.pwHash = pwHash;
  }

  public ApiUser2 pwSalt(Integer pwSalt) {
    this.pwSalt = pwSalt;
    return this;
  }

  /**
   * NOTE: Only available in the \"export\" view
   * @return pwSalt
  **/
  @ApiModelProperty(value = "NOTE: Only available in the \"export\" view")


  public Integer getPwSalt() {
    return pwSalt;
  }

  public void setPwSalt(Integer pwSalt) {
    this.pwSalt = pwSalt;
  }

  public ApiUser2 pwLogin(Boolean pwLogin) {
    this.pwLogin = pwLogin;
    return this;
  }

  /**
   * NOTE: Only available in the \"export\" view
   * @return pwLogin
  **/
  @ApiModelProperty(value = "NOTE: Only available in the \"export\" view")


  public Boolean isPwLogin() {
    return pwLogin;
  }

  public void setPwLogin(Boolean pwLogin) {
    this.pwLogin = pwLogin;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiUser2 apiUser2 = (ApiUser2) o;
    return Objects.equals(this.name, apiUser2.name) &&
        Objects.equals(this.password, apiUser2.password) &&
        Objects.equals(this.authRoles, apiUser2.authRoles) &&
        Objects.equals(this.pwHash, apiUser2.pwHash) &&
        Objects.equals(this.pwSalt, apiUser2.pwSalt) &&
        Objects.equals(this.pwLogin, apiUser2.pwLogin);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, password, authRoles, pwHash, pwSalt, pwLogin);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiUser2 {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    authRoles: ").append(toIndentedString(authRoles)).append("\n");
    sb.append("    pwHash: ").append(toIndentedString(pwHash)).append("\n");
    sb.append("    pwSalt: ").append(toIndentedString(pwSalt)).append("\n");
    sb.append("    pwLogin: ").append(toIndentedString(pwLogin)).append("\n");
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

