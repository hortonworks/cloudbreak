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
 * This is the model for user information in the API prior to v18. Post v18, please refer to ApiUser2.java. &lt;p&gt; Note that any method that returns user information will not contain any password information. The password property is only used when creating or updating users.
 */
@ApiModel(description = "This is the model for user information in the API prior to v18. Post v18, please refer to ApiUser2.java. <p> Note that any method that returns user information will not contain any password information. The password property is only used when creating or updating users.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiUser   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("password")
  private String password = null;

  @JsonProperty("roles")
  @Valid
  private List<String> roles = null;

  @JsonProperty("pwHash")
  private String pwHash = null;

  @JsonProperty("pwSalt")
  private Integer pwSalt = null;

  @JsonProperty("pwLogin")
  private Boolean pwLogin = null;

  public ApiUser name(String name) {
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

  public ApiUser password(String password) {
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

  public ApiUser roles(List<String> roles) {
    this.roles = roles;
    return this;
  }

  public ApiUser addRolesItem(String rolesItem) {
    if (this.roles == null) {
      this.roles = new ArrayList<>();
    }
    this.roles.add(rolesItem);
    return this;
  }

  /**
   * A list of roles this user belongs to. <p> In Cloudera Express, possible values are: <ul> <li><b>ROLE_ADMIN</b></li> <li><b>ROLE_USER</b></li> </ul> In Cloudera Enterprise Datahub Edition, additional possible values are: <ul> <li><b>ROLE_LIMITED</b>: Added in Cloudera Manager 5.0</li> <li><b>ROLE_OPERATOR</b>: Added in Cloudera Manager 5.1</li> <li><b>ROLE_CONFIGURATOR</b>: Added in Cloudera Manager 5.1</li> <li><b>ROLE_CLUSTER_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_BDR_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_NAVIGATOR_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_USER_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_KEY_ADMIN</b>: Added in Cloudera Manager 5.5</li> </ul> An empty list implies ROLE_USER. <p> Note that although this interface provides a list of roles, a user should only be assigned a single role at a time.
   * @return roles
  **/
  @ApiModelProperty(value = "A list of roles this user belongs to. <p> In Cloudera Express, possible values are: <ul> <li><b>ROLE_ADMIN</b></li> <li><b>ROLE_USER</b></li> </ul> In Cloudera Enterprise Datahub Edition, additional possible values are: <ul> <li><b>ROLE_LIMITED</b>: Added in Cloudera Manager 5.0</li> <li><b>ROLE_OPERATOR</b>: Added in Cloudera Manager 5.1</li> <li><b>ROLE_CONFIGURATOR</b>: Added in Cloudera Manager 5.1</li> <li><b>ROLE_CLUSTER_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_BDR_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_NAVIGATOR_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_USER_ADMIN</b>: Added in Cloudera Manager 5.2</li> <li><b>ROLE_KEY_ADMIN</b>: Added in Cloudera Manager 5.5</li> </ul> An empty list implies ROLE_USER. <p> Note that although this interface provides a list of roles, a user should only be assigned a single role at a time.")


  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public ApiUser pwHash(String pwHash) {
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

  public ApiUser pwSalt(Integer pwSalt) {
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

  public ApiUser pwLogin(Boolean pwLogin) {
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
    ApiUser apiUser = (ApiUser) o;
    return Objects.equals(this.name, apiUser.name) &&
        Objects.equals(this.password, apiUser.password) &&
        Objects.equals(this.roles, apiUser.roles) &&
        Objects.equals(this.pwHash, apiUser.pwHash) &&
        Objects.equals(this.pwSalt, apiUser.pwSalt) &&
        Objects.equals(this.pwLogin, apiUser.pwLogin);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, password, roles, pwHash, pwSalt, pwLogin);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiUser {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
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

