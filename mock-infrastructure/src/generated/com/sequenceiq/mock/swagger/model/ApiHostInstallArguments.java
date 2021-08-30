package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiMapEntry;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments to perform installation on one or more hosts
 */
@ApiModel(description = "Arguments to perform installation on one or more hosts")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHostInstallArguments   {
  @JsonProperty("hostNames")
  @Valid
  private List<String> hostNames = null;

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

  @JsonProperty("parallelInstallCount")
  private Integer parallelInstallCount = null;

  @JsonProperty("cmRepoUrl")
  private String cmRepoUrl = null;

  @JsonProperty("gpgKeyCustomUrl")
  private String gpgKeyCustomUrl = null;

  @JsonProperty("javaInstallStrategy")
  private String javaInstallStrategy = null;

  @JsonProperty("unlimitedJCE")
  private Boolean unlimitedJCE = null;

  @JsonProperty("gpgKeyOverrideBundle")
  private String gpgKeyOverrideBundle = null;

  @JsonProperty("agentReportedHostnames")
  @Valid
  private List<ApiMapEntry> agentReportedHostnames = null;

  @JsonProperty("subjectAltNames")
  @Valid
  private List<String> subjectAltNames = null;

  public ApiHostInstallArguments hostNames(List<String> hostNames) {
    this.hostNames = hostNames;
    return this;
  }

  public ApiHostInstallArguments addHostNamesItem(String hostNamesItem) {
    if (this.hostNames == null) {
      this.hostNames = new ArrayList<>();
    }
    this.hostNames.add(hostNamesItem);
    return this;
  }

  /**
   * List of hosts to configure for use with Cloudera Manager. A host may be specified by a hostname (FQDN) or an IP address.
   * @return hostNames
  **/
  @ApiModelProperty(value = "List of hosts to configure for use with Cloudera Manager. A host may be specified by a hostname (FQDN) or an IP address.")


  public List<String> getHostNames() {
    return hostNames;
  }

  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }

  public ApiHostInstallArguments sshPort(Integer sshPort) {
    this.sshPort = sshPort;
    return this;
  }

  /**
   * SSH port. If unset, defaults to 22.
   * @return sshPort
  **/
  @ApiModelProperty(value = "SSH port. If unset, defaults to 22.")


  public Integer getSshPort() {
    return sshPort;
  }

  public void setSshPort(Integer sshPort) {
    this.sshPort = sshPort;
  }

  public ApiHostInstallArguments userName(String userName) {
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

  public ApiHostInstallArguments password(String password) {
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

  public ApiHostInstallArguments privateKey(String privateKey) {
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

  public ApiHostInstallArguments passphrase(String passphrase) {
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

  public ApiHostInstallArguments parallelInstallCount(Integer parallelInstallCount) {
    this.parallelInstallCount = parallelInstallCount;
    return this;
  }

  /**
   * Number of simultaneous installations. Defaults to 10. Running a large number of installations at once can consume large amounts of network bandwidth and other system resources.
   * @return parallelInstallCount
  **/
  @ApiModelProperty(value = "Number of simultaneous installations. Defaults to 10. Running a large number of installations at once can consume large amounts of network bandwidth and other system resources.")


  public Integer getParallelInstallCount() {
    return parallelInstallCount;
  }

  public void setParallelInstallCount(Integer parallelInstallCount) {
    this.parallelInstallCount = parallelInstallCount;
  }

  public ApiHostInstallArguments cmRepoUrl(String cmRepoUrl) {
    this.cmRepoUrl = cmRepoUrl;
    return this;
  }

  /**
   * The Cloudera Manager repository URL to use (optional). Example for SLES, Redhat or Debian based distributions: https://archive.cloudera.com/cm6/6.0.0
   * @return cmRepoUrl
  **/
  @ApiModelProperty(value = "The Cloudera Manager repository URL to use (optional). Example for SLES, Redhat or Debian based distributions: https://archive.cloudera.com/cm6/6.0.0")


  public String getCmRepoUrl() {
    return cmRepoUrl;
  }

  public void setCmRepoUrl(String cmRepoUrl) {
    this.cmRepoUrl = cmRepoUrl;
  }

  public ApiHostInstallArguments gpgKeyCustomUrl(String gpgKeyCustomUrl) {
    this.gpgKeyCustomUrl = gpgKeyCustomUrl;
    return this;
  }

  /**
   * The Cloudera Manager public GPG key (optional). Example for SLES, Redhat or other RPM based distributions: https://archive.cloudera.com/cm5/redhat/5/x86_64/cm/RPM-GPG-KEY-cloudera Example for Ubuntu or other Debian based distributions: https://archive.cloudera.com/cm5/ubuntu/lucid/amd64/cm/archive.key
   * @return gpgKeyCustomUrl
  **/
  @ApiModelProperty(value = "The Cloudera Manager public GPG key (optional). Example for SLES, Redhat or other RPM based distributions: https://archive.cloudera.com/cm5/redhat/5/x86_64/cm/RPM-GPG-KEY-cloudera Example for Ubuntu or other Debian based distributions: https://archive.cloudera.com/cm5/ubuntu/lucid/amd64/cm/archive.key")


  public String getGpgKeyCustomUrl() {
    return gpgKeyCustomUrl;
  }

  public void setGpgKeyCustomUrl(String gpgKeyCustomUrl) {
    this.gpgKeyCustomUrl = gpgKeyCustomUrl;
  }

  public ApiHostInstallArguments javaInstallStrategy(String javaInstallStrategy) {
    this.javaInstallStrategy = javaInstallStrategy;
    return this;
  }

  /**
   * Added in v8: Strategy to use for JDK installation. Valid values are <br> 1. AUTO: Cloudera Manager will install the JDK versions that are required when the \"AUTO\" option is selected. This package will be downloaded from Cloudera repository. This operation may overwrite any of the existing JDK installations. <br> 2. NONE(default): Cloudera Manager will not install any JDK when \"NONE\" option is selected. It should be used if an existing JDK installation can be used. NOTE: Selecting the \"NONE\" option makes it the customer's responsibility to ensure that the unlimited strength JCE policy files are installed and enabled on each host, as appropriate for the version of Java in use. <br> 3. OS_PROVIDED_JDK: Option added in v40, Cloudera Manager will install OpenJDK packages provided by the [non-cloudera] Operating System repositories configured on each host. This operation may overwrite any of the existing JDK installations.
   * @return javaInstallStrategy
  **/
  @ApiModelProperty(value = "Added in v8: Strategy to use for JDK installation. Valid values are <br> 1. AUTO: Cloudera Manager will install the JDK versions that are required when the \"AUTO\" option is selected. This package will be downloaded from Cloudera repository. This operation may overwrite any of the existing JDK installations. <br> 2. NONE(default): Cloudera Manager will not install any JDK when \"NONE\" option is selected. It should be used if an existing JDK installation can be used. NOTE: Selecting the \"NONE\" option makes it the customer's responsibility to ensure that the unlimited strength JCE policy files are installed and enabled on each host, as appropriate for the version of Java in use. <br> 3. OS_PROVIDED_JDK: Option added in v40, Cloudera Manager will install OpenJDK packages provided by the [non-cloudera] Operating System repositories configured on each host. This operation may overwrite any of the existing JDK installations.")


  public String getJavaInstallStrategy() {
    return javaInstallStrategy;
  }

  public void setJavaInstallStrategy(String javaInstallStrategy) {
    this.javaInstallStrategy = javaInstallStrategy;
  }

  public ApiHostInstallArguments unlimitedJCE(Boolean unlimitedJCE) {
    this.unlimitedJCE = unlimitedJCE;
    return this;
  }

  /**
   * Added in v8: Flag for unlimited strength JCE policy files installation If unset, defaults to false <br> Deprecated in CM 7.0.2: Flag for unlimited strength JCE policy files installation will be set to False. NOTE: With OpenJDK11 (and OpenJDK8 u232), JCE is installed and enabled by default.
   * @return unlimitedJCE
  **/
  @ApiModelProperty(value = "Added in v8: Flag for unlimited strength JCE policy files installation If unset, defaults to false <br> Deprecated in CM 7.0.2: Flag for unlimited strength JCE policy files installation will be set to False. NOTE: With OpenJDK11 (and OpenJDK8 u232), JCE is installed and enabled by default.")


  public Boolean isUnlimitedJCE() {
    return unlimitedJCE;
  }

  public void setUnlimitedJCE(Boolean unlimitedJCE) {
    this.unlimitedJCE = unlimitedJCE;
  }

  public ApiHostInstallArguments gpgKeyOverrideBundle(String gpgKeyOverrideBundle) {
    this.gpgKeyOverrideBundle = gpgKeyOverrideBundle;
    return this;
  }

  /**
   * The Cloudera Manager public GPG key (optional). This points to the actual bundle contents and not a URL.
   * @return gpgKeyOverrideBundle
  **/
  @ApiModelProperty(value = "The Cloudera Manager public GPG key (optional). This points to the actual bundle contents and not a URL.")


  public String getGpgKeyOverrideBundle() {
    return gpgKeyOverrideBundle;
  }

  public void setGpgKeyOverrideBundle(String gpgKeyOverrideBundle) {
    this.gpgKeyOverrideBundle = gpgKeyOverrideBundle;
  }

  public ApiHostInstallArguments agentReportedHostnames(List<ApiMapEntry> agentReportedHostnames) {
    this.agentReportedHostnames = agentReportedHostnames;
    return this;
  }

  public ApiHostInstallArguments addAgentReportedHostnamesItem(ApiMapEntry agentReportedHostnamesItem) {
    if (this.agentReportedHostnames == null) {
      this.agentReportedHostnames = new ArrayList<>();
    }
    this.agentReportedHostnames.add(agentReportedHostnamesItem);
    return this;
  }

  /**
   * Optional. A map from hostname to reported_hostname value to be set in the agent configuration file.
   * @return agentReportedHostnames
  **/
  @ApiModelProperty(value = "Optional. A map from hostname to reported_hostname value to be set in the agent configuration file.")

  @Valid

  public List<ApiMapEntry> getAgentReportedHostnames() {
    return agentReportedHostnames;
  }

  public void setAgentReportedHostnames(List<ApiMapEntry> agentReportedHostnames) {
    this.agentReportedHostnames = agentReportedHostnames;
  }

  public ApiHostInstallArguments subjectAltNames(List<String> subjectAltNames) {
    this.subjectAltNames = subjectAltNames;
    return this;
  }

  public ApiHostInstallArguments addSubjectAltNamesItem(String subjectAltNamesItem) {
    if (this.subjectAltNames == null) {
      this.subjectAltNames = new ArrayList<>();
    }
    this.subjectAltNames.add(subjectAltNamesItem);
    return this;
  }

  /**
   * 
   * @return subjectAltNames
  **/
  @ApiModelProperty(value = "")


  public List<String> getSubjectAltNames() {
    return subjectAltNames;
  }

  public void setSubjectAltNames(List<String> subjectAltNames) {
    this.subjectAltNames = subjectAltNames;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHostInstallArguments apiHostInstallArguments = (ApiHostInstallArguments) o;
    return Objects.equals(this.hostNames, apiHostInstallArguments.hostNames) &&
        Objects.equals(this.sshPort, apiHostInstallArguments.sshPort) &&
        Objects.equals(this.userName, apiHostInstallArguments.userName) &&
        Objects.equals(this.password, apiHostInstallArguments.password) &&
        Objects.equals(this.privateKey, apiHostInstallArguments.privateKey) &&
        Objects.equals(this.passphrase, apiHostInstallArguments.passphrase) &&
        Objects.equals(this.parallelInstallCount, apiHostInstallArguments.parallelInstallCount) &&
        Objects.equals(this.cmRepoUrl, apiHostInstallArguments.cmRepoUrl) &&
        Objects.equals(this.gpgKeyCustomUrl, apiHostInstallArguments.gpgKeyCustomUrl) &&
        Objects.equals(this.javaInstallStrategy, apiHostInstallArguments.javaInstallStrategy) &&
        Objects.equals(this.unlimitedJCE, apiHostInstallArguments.unlimitedJCE) &&
        Objects.equals(this.gpgKeyOverrideBundle, apiHostInstallArguments.gpgKeyOverrideBundle) &&
        Objects.equals(this.agentReportedHostnames, apiHostInstallArguments.agentReportedHostnames) &&
        Objects.equals(this.subjectAltNames, apiHostInstallArguments.subjectAltNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostNames, sshPort, userName, password, privateKey, passphrase, parallelInstallCount, cmRepoUrl, gpgKeyCustomUrl, javaInstallStrategy, unlimitedJCE, gpgKeyOverrideBundle, agentReportedHostnames, subjectAltNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHostInstallArguments {\n");
    
    sb.append("    hostNames: ").append(toIndentedString(hostNames)).append("\n");
    sb.append("    sshPort: ").append(toIndentedString(sshPort)).append("\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    privateKey: ").append(toIndentedString(privateKey)).append("\n");
    sb.append("    passphrase: ").append(toIndentedString(passphrase)).append("\n");
    sb.append("    parallelInstallCount: ").append(toIndentedString(parallelInstallCount)).append("\n");
    sb.append("    cmRepoUrl: ").append(toIndentedString(cmRepoUrl)).append("\n");
    sb.append("    gpgKeyCustomUrl: ").append(toIndentedString(gpgKeyCustomUrl)).append("\n");
    sb.append("    javaInstallStrategy: ").append(toIndentedString(javaInstallStrategy)).append("\n");
    sb.append("    unlimitedJCE: ").append(toIndentedString(unlimitedJCE)).append("\n");
    sb.append("    gpgKeyOverrideBundle: ").append(toIndentedString(gpgKeyOverrideBundle)).append("\n");
    sb.append("    agentReportedHostnames: ").append(toIndentedString(agentReportedHostnames)).append("\n");
    sb.append("    subjectAltNames: ").append(toIndentedString(subjectAltNames)).append("\n");
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

