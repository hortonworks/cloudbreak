package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Contains common arguments for commands which require SSH&#39;ing into one or more hosts.
 */
@ApiModel(description = "Contains common arguments for commands which require SSH'ing into one or more hosts.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class BaseApiSshCmdArguments   {
  @JsonProperty("sshPort")
  private Integer sshPort = null;

  @JsonProperty("userName")
  private String userName = null;

  @JsonProperty("password")
  private String password = null;

  @JsonProperty("privateKey")
  private String privateKey = null;

  @JsonProperty("passphrase")
  private String passphrase = null;

  public BaseApiSshCmdArguments sshPort(Integer sshPort) {
    this.sshPort = sshPort;
    return this;
  }

  /**
   * SSH port. If unset, defaults to 22.
   * @return sshPort
  **/
  @ApiModelProperty(example = "22", value = "SSH port. If unset, defaults to 22.")


  public Integer getSshPort() {
    return sshPort;
  }

  public void setSshPort(Integer sshPort) {
    this.sshPort = sshPort;
  }

  public BaseApiSshCmdArguments userName(String userName) {
    this.userName = userName;
    return this;
  }

  /**
   * The username used to authenticate with the hosts. Root access to your hosts is required to install Cloudera packages. The installer will connect to your hosts via SSH and log in either directly as root or as another user with password-less sudo privileges to become root.
   * @return userName
  **/
  @ApiModelProperty(value = "The username used to authenticate with the hosts. Root access to your hosts is required to install Cloudera packages. The installer will connect to your hosts via SSH and log in either directly as root or as another user with password-less sudo privileges to become root.")


  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public BaseApiSshCmdArguments password(String password) {
    this.password = password;
    return this;
  }

  /**
   * The password used to authenticate with the hosts. Specify either this or a private key. For password-less login, use an empty string as password.
   * @return password
  **/
  @ApiModelProperty(value = "The password used to authenticate with the hosts. Specify either this or a private key. For password-less login, use an empty string as password.")


  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public BaseApiSshCmdArguments privateKey(String privateKey) {
    this.privateKey = privateKey;
    return this;
  }

  /**
   * The private key to authenticate with the hosts. Specify either this or a password. <br> The private key, if specified, needs to be a standard PEM-encoded key as a single string, with all line breaks replaced with the line-feed control character '\\n'. <br> A value will typically look like the following string: <br> -----BEGIN RSA PRIVATE KEY-----\\n[base-64 encoded key]\\n-----END RSA PRIVATE KEY----- <br>
   * @return privateKey
  **/
  @ApiModelProperty(value = "The private key to authenticate with the hosts. Specify either this or a password. <br> The private key, if specified, needs to be a standard PEM-encoded key as a single string, with all line breaks replaced with the line-feed control character '\\n'. <br> A value will typically look like the following string: <br> -----BEGIN RSA PRIVATE KEY-----\\n[base-64 encoded key]\\n-----END RSA PRIVATE KEY----- <br>")


  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  public BaseApiSshCmdArguments passphrase(String passphrase) {
    this.passphrase = passphrase;
    return this;
  }

  /**
   * The passphrase associated with the private key used to authenticate with the hosts (optional).
   * @return passphrase
  **/
  @ApiModelProperty(value = "The passphrase associated with the private key used to authenticate with the hosts (optional).")


  public String getPassphrase() {
    return passphrase;
  }

  public void setPassphrase(String passphrase) {
    this.passphrase = passphrase;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BaseApiSshCmdArguments baseApiSshCmdArguments = (BaseApiSshCmdArguments) o;
    return Objects.equals(this.sshPort, baseApiSshCmdArguments.sshPort) &&
        Objects.equals(this.userName, baseApiSshCmdArguments.userName) &&
        Objects.equals(this.password, baseApiSshCmdArguments.password) &&
        Objects.equals(this.privateKey, baseApiSshCmdArguments.privateKey) &&
        Objects.equals(this.passphrase, baseApiSshCmdArguments.passphrase);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sshPort, userName, password, privateKey, passphrase);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BaseApiSshCmdArguments {\n");
    
    sb.append("    sshPort: ").append(toIndentedString(sshPort)).append("\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    privateKey: ").append(toIndentedString(privateKey)).append("\n");
    sb.append("    passphrase: ").append(toIndentedString(passphrase)).append("\n");
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

