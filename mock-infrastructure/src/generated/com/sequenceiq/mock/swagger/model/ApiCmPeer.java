package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiCmPeerType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Information about a Cloudera Manager peer instance. &lt;p&gt; The requirement and usage of &lt;i&gt;username&lt;/i&gt; and &lt;i&gt;password&lt;/i&gt; properties are dependent on the &lt;i&gt;clouderaManagerCreatedUser&lt;/i&gt; flag. &lt;p&gt; When creating peers, if &#39;clouderaManagerCreatedUser&#39; is true, the username/password should be the credentials of a user with administrator privileges on the remote Cloudera Manager. These credentials are not stored, they are used to connect to the peer and create a user in that peer. The newly created user is stored and used for communication with that peer. If &#39;clouderaManagerCreatedUser&#39; is false, which is not applicable to REPLICATION peer type, the username/password to the remote Cloudera Manager are directly stored and used for all communications with that peer. &lt;p&gt; When updating peers, if &#39;clouderaManagerCreatedUser&#39; is true and username/password are set, a new remote user will be created. If &#39;clouderaManagerCreatedUser&#39; is false and username/password are set, the stored username/password will be updated.
 */
@ApiModel(description = "Information about a Cloudera Manager peer instance. <p> The requirement and usage of <i>username</i> and <i>password</i> properties are dependent on the <i>clouderaManagerCreatedUser</i> flag. <p> When creating peers, if 'clouderaManagerCreatedUser' is true, the username/password should be the credentials of a user with administrator privileges on the remote Cloudera Manager. These credentials are not stored, they are used to connect to the peer and create a user in that peer. The newly created user is stored and used for communication with that peer. If 'clouderaManagerCreatedUser' is false, which is not applicable to REPLICATION peer type, the username/password to the remote Cloudera Manager are directly stored and used for all communications with that peer. <p> When updating peers, if 'clouderaManagerCreatedUser' is true and username/password are set, a new remote user will be created. If 'clouderaManagerCreatedUser' is false and username/password are set, the stored username/password will be updated.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiCmPeer   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("type")
  private ApiCmPeerType type = null;

  @JsonProperty("url")
  private String url = null;

  @JsonProperty("username")
  private String username = null;

  @JsonProperty("password")
  private String password = null;

  @JsonProperty("clouderaManagerCreatedUser")
  private Boolean clouderaManagerCreatedUser = null;

  public ApiCmPeer name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the remote CM instance. Immutable during update.
   * @return name
  **/
  @ApiModelProperty(value = "The name of the remote CM instance. Immutable during update.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiCmPeer type(ApiCmPeerType type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the remote CM instance. Immutable during update.  Available since API v11.
   * @return type
  **/
  @ApiModelProperty(value = "The type of the remote CM instance. Immutable during update.  Available since API v11.")

  @Valid

  public ApiCmPeerType getType() {
    return type;
  }

  public void setType(ApiCmPeerType type) {
    this.type = type;
  }

  public ApiCmPeer url(String url) {
    this.url = url;
    return this;
  }

  /**
   * The URL of the remote CM instance. Mutable during update.
   * @return url
  **/
  @ApiModelProperty(value = "The URL of the remote CM instance. Mutable during update.")


  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public ApiCmPeer username(String username) {
    this.username = username;
    return this;
  }

  /**
   * When creating peers, if 'clouderaManagerCreatedUser' is true, this should be the remote admin username for creating a user in remote Cloudera Manager. The created remote user will then be stored in the local Cloudera Manager DB and used in later communication. If 'clouderaManagerCreatedUser' is false, which is not applicable to REPLICATION peer type, Cloudera Manager will store this username in the local DB directly and use it together with 'password' for communication.  Mutable during update. When set during update, if 'clouderaManagerCreatedUser' is true, a new user in remote Cloudera Manager is created, the newly created remote user will be stored in the local DB. An attempt to delete the previously created remote user will be made; If 'clouderaManagerCreatedUser' is false, the username/password in the local DB will be updated.
   * @return username
  **/
  @ApiModelProperty(value = "When creating peers, if 'clouderaManagerCreatedUser' is true, this should be the remote admin username for creating a user in remote Cloudera Manager. The created remote user will then be stored in the local Cloudera Manager DB and used in later communication. If 'clouderaManagerCreatedUser' is false, which is not applicable to REPLICATION peer type, Cloudera Manager will store this username in the local DB directly and use it together with 'password' for communication.  Mutable during update. When set during update, if 'clouderaManagerCreatedUser' is true, a new user in remote Cloudera Manager is created, the newly created remote user will be stored in the local DB. An attempt to delete the previously created remote user will be made; If 'clouderaManagerCreatedUser' is false, the username/password in the local DB will be updated.")


  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public ApiCmPeer password(String password) {
    this.password = password;
    return this;
  }

  /**
   * When creating peers, if 'clouderaManagerCreatedUser' is true, this should be the remote admin password for creating a user in remote Cloudera Manager. The created remote user will then be stored in the local Cloudera Manager DB and used in later communication. If 'clouderaManagerCreatedUser' is false, which is not applicable to REPLICATION peer type, Cloudera Manager will store this password in the local DB directly and use it together with 'username' for communication.  Mutable during update. When set during update, if 'clouderaManagerCreatedUser' is true, a new user in remote Cloudera Manager is created, the newly created remote user will be stored in the local DB. An attempt to delete the previously created remote user will be made; If 'clouderaManagerCreatedUser' is false, the username/password in the local DB will be updated.
   * @return password
  **/
  @ApiModelProperty(value = "When creating peers, if 'clouderaManagerCreatedUser' is true, this should be the remote admin password for creating a user in remote Cloudera Manager. The created remote user will then be stored in the local Cloudera Manager DB and used in later communication. If 'clouderaManagerCreatedUser' is false, which is not applicable to REPLICATION peer type, Cloudera Manager will store this password in the local DB directly and use it together with 'username' for communication.  Mutable during update. When set during update, if 'clouderaManagerCreatedUser' is true, a new user in remote Cloudera Manager is created, the newly created remote user will be stored in the local DB. An attempt to delete the previously created remote user will be made; If 'clouderaManagerCreatedUser' is false, the username/password in the local DB will be updated.")


  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public ApiCmPeer clouderaManagerCreatedUser(Boolean clouderaManagerCreatedUser) {
    this.clouderaManagerCreatedUser = clouderaManagerCreatedUser;
    return this;
  }

  /**
   * If true, Cloudera Manager creates a remote user using the given username/password and stores the created user in local DB for use in later communication. Cloudera Manager will also try to delete the created remote user when deleting such peers.  If false, Cloudera Manager will store the provided username/password in the local DB and use them in later communication. 'false' value on this field is not applicable to REPLICATION peer type.  Available since API v11.  Immutable during update. Should not be set when updating peers.
   * @return clouderaManagerCreatedUser
  **/
  @ApiModelProperty(value = "If true, Cloudera Manager creates a remote user using the given username/password and stores the created user in local DB for use in later communication. Cloudera Manager will also try to delete the created remote user when deleting such peers.  If false, Cloudera Manager will store the provided username/password in the local DB and use them in later communication. 'false' value on this field is not applicable to REPLICATION peer type.  Available since API v11.  Immutable during update. Should not be set when updating peers.")


  public Boolean isClouderaManagerCreatedUser() {
    return clouderaManagerCreatedUser;
  }

  public void setClouderaManagerCreatedUser(Boolean clouderaManagerCreatedUser) {
    this.clouderaManagerCreatedUser = clouderaManagerCreatedUser;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCmPeer apiCmPeer = (ApiCmPeer) o;
    return Objects.equals(this.name, apiCmPeer.name) &&
        Objects.equals(this.type, apiCmPeer.type) &&
        Objects.equals(this.url, apiCmPeer.url) &&
        Objects.equals(this.username, apiCmPeer.username) &&
        Objects.equals(this.password, apiCmPeer.password) &&
        Objects.equals(this.clouderaManagerCreatedUser, apiCmPeer.clouderaManagerCreatedUser);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, url, username, password, clouderaManagerCreatedUser);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCmPeer {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    clouderaManagerCreatedUser: ").append(toIndentedString(clouderaManagerCreatedUser)).append("\n");
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

