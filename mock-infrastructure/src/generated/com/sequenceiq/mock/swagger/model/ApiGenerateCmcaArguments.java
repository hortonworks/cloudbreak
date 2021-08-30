package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHostCertInfo;
import com.sequenceiq.mock.swagger.model.BaseApiSshCmdArguments;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments to generate a Cloudera Manager Certificate Authority (CMCA).
 */
@ApiModel(description = "Arguments to generate a Cloudera Manager Certificate Authority (CMCA).")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiGenerateCmcaArguments extends BaseApiSshCmdArguments  {
  @JsonProperty("location")
  private String location = null;

  @JsonProperty("customCA")
  private Boolean customCA = null;

  @JsonProperty("interpretAsFilenames")
  private Boolean interpretAsFilenames = null;

  @JsonProperty("cmHostCert")
  private String cmHostCert = null;

  @JsonProperty("cmHostKey")
  private String cmHostKey = null;

  @JsonProperty("caCert")
  private String caCert = null;

  @JsonProperty("keystorePasswd")
  private String keystorePasswd = null;

  @JsonProperty("truststorePasswd")
  private String truststorePasswd = null;

  @JsonProperty("trustedCaCerts")
  private String trustedCaCerts = null;

  @JsonProperty("additionalArguments")
  @Valid
  private List<String> additionalArguments = null;

  @JsonProperty("hostCerts")
  @Valid
  private List<ApiHostCertInfo> hostCerts = null;

  @JsonProperty("configureAllServices")
  private Boolean configureAllServices = null;

  public ApiGenerateCmcaArguments location(String location) {
    this.location = location;
    return this;
  }

  /**
   * The location on disk to store the CMCA directory. If there is already a CMCA created there, it will be backed up, and a new one will be created in its place.
   * @return location
  **/
  @ApiModelProperty(example = "/opt/cloudera/CMCA", value = "The location on disk to store the CMCA directory. If there is already a CMCA created there, it will be backed up, and a new one will be created in its place.")


  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public ApiGenerateCmcaArguments customCA(Boolean customCA) {
    this.customCA = customCA;
    return this;
  }

  /**
   * Whether to generate an internal CMCA (false) or use user-provided certificates (true).  When set to true (user-provided certificates), the following other arguments must be given: * cmHostCert * cmHostKey * caCert * keystorePasswd * truststorePasswd
   * @return customCA
  **/
  @ApiModelProperty(example = "false", value = "Whether to generate an internal CMCA (false) or use user-provided certificates (true).  When set to true (user-provided certificates), the following other arguments must be given: * cmHostCert * cmHostKey * caCert * keystorePasswd * truststorePasswd")


  public Boolean isCustomCA() {
    return customCA;
  }

  public void setCustomCA(Boolean customCA) {
    this.customCA = customCA;
  }

  public ApiGenerateCmcaArguments interpretAsFilenames(Boolean interpretAsFilenames) {
    this.interpretAsFilenames = interpretAsFilenames;
    return this;
  }

  /**
   * Whether the following arguments are interpreted as filenames local to the Cloudera Manager host (true, default) or as the actual data for that argument: * cmHostCert * cmHostKey * caCert * keystorePasswd * truststorePasswd * trustedCaCerts * hostCerts.hostCert * hostCerts.hostKey  If HTTPS has not been enabled on the Cloudera Manager Admin Console and API, we *strongly* recommend that you pass the arguments as filenames local to the Cloudera Manager host (i.e. set to true) to avoid leaking sensitive information over the wire in plaintext.
   * @return interpretAsFilenames
  **/
  @ApiModelProperty(example = "true", value = "Whether the following arguments are interpreted as filenames local to the Cloudera Manager host (true, default) or as the actual data for that argument: * cmHostCert * cmHostKey * caCert * keystorePasswd * truststorePasswd * trustedCaCerts * hostCerts.hostCert * hostCerts.hostKey  If HTTPS has not been enabled on the Cloudera Manager Admin Console and API, we *strongly* recommend that you pass the arguments as filenames local to the Cloudera Manager host (i.e. set to true) to avoid leaking sensitive information over the wire in plaintext.")


  public Boolean isInterpretAsFilenames() {
    return interpretAsFilenames;
  }

  public void setInterpretAsFilenames(Boolean interpretAsFilenames) {
    this.interpretAsFilenames = interpretAsFilenames;
  }

  public ApiGenerateCmcaArguments cmHostCert(String cmHostCert) {
    this.cmHostCert = cmHostCert;
    return this;
  }

  /**
   * The certificate for the CM host in PEM format. Only used if customCA == true.
   * @return cmHostCert
  **/
  @ApiModelProperty(example = "host-cert.pem", value = "The certificate for the CM host in PEM format. Only used if customCA == true.")


  public String getCmHostCert() {
    return cmHostCert;
  }

  public void setCmHostCert(String cmHostCert) {
    this.cmHostCert = cmHostCert;
  }

  public ApiGenerateCmcaArguments cmHostKey(String cmHostKey) {
    this.cmHostKey = cmHostKey;
    return this;
  }

  /**
   * The private key for the CM host in PEM format. Only used if customCA == true.
   * @return cmHostKey
  **/
  @ApiModelProperty(example = "host-key.pem", value = "The private key for the CM host in PEM format. Only used if customCA == true.")


  public String getCmHostKey() {
    return cmHostKey;
  }

  public void setCmHostKey(String cmHostKey) {
    this.cmHostKey = cmHostKey;
  }

  public ApiGenerateCmcaArguments caCert(String caCert) {
    this.caCert = caCert;
    return this;
  }

  /**
   * The certificate for the user-provided certificate authority in PEM format. Only used if customCA == true.
   * @return caCert
  **/
  @ApiModelProperty(example = "ca-cert.pem", value = "The certificate for the user-provided certificate authority in PEM format. Only used if customCA == true.")


  public String getCaCert() {
    return caCert;
  }

  public void setCaCert(String caCert) {
    this.caCert = caCert;
  }

  public ApiGenerateCmcaArguments keystorePasswd(String keystorePasswd) {
    this.keystorePasswd = keystorePasswd;
    return this;
  }

  /**
   * The password used for all Auto-TLS keystores. Only used if customCA == true.
   * @return keystorePasswd
  **/
  @ApiModelProperty(example = "keystore.pw.txt", value = "The password used for all Auto-TLS keystores. Only used if customCA == true.")


  public String getKeystorePasswd() {
    return keystorePasswd;
  }

  public void setKeystorePasswd(String keystorePasswd) {
    this.keystorePasswd = keystorePasswd;
  }

  public ApiGenerateCmcaArguments truststorePasswd(String truststorePasswd) {
    this.truststorePasswd = truststorePasswd;
    return this;
  }

  /**
   * The password used for all Auto-TLS truststores. Only used if customCA == true.
   * @return truststorePasswd
  **/
  @ApiModelProperty(example = "truststore.pw.txt", value = "The password used for all Auto-TLS truststores. Only used if customCA == true.")


  public String getTruststorePasswd() {
    return truststorePasswd;
  }

  public void setTruststorePasswd(String truststorePasswd) {
    this.truststorePasswd = truststorePasswd;
  }

  public ApiGenerateCmcaArguments trustedCaCerts(String trustedCaCerts) {
    this.trustedCaCerts = trustedCaCerts;
    return this;
  }

  /**
   * A list of CA certificates that will be imported into the Auto-TLS truststore and distributed to all hosts.
   * @return trustedCaCerts
  **/
  @ApiModelProperty(example = "cacerts.pem", value = "A list of CA certificates that will be imported into the Auto-TLS truststore and distributed to all hosts.")


  public String getTrustedCaCerts() {
    return trustedCaCerts;
  }

  public void setTrustedCaCerts(String trustedCaCerts) {
    this.trustedCaCerts = trustedCaCerts;
  }

  public ApiGenerateCmcaArguments additionalArguments(List<String> additionalArguments) {
    this.additionalArguments = additionalArguments;
    return this;
  }

  public ApiGenerateCmcaArguments addAdditionalArgumentsItem(String additionalArgumentsItem) {
    if (this.additionalArguments == null) {
      this.additionalArguments = new ArrayList<>();
    }
    this.additionalArguments.add(additionalArgumentsItem);
    return this;
  }

  /**
   * A list of additional arguments that can be passed to the certificate manager
   * @return additionalArguments
  **/
  @ApiModelProperty(example = "\"--override\"", value = "A list of additional arguments that can be passed to the certificate manager")


  public List<String> getAdditionalArguments() {
    return additionalArguments;
  }

  public void setAdditionalArguments(List<String> additionalArguments) {
    this.additionalArguments = additionalArguments;
  }

  public ApiGenerateCmcaArguments hostCerts(List<ApiHostCertInfo> hostCerts) {
    this.hostCerts = hostCerts;
    return this;
  }

  public ApiGenerateCmcaArguments addHostCertsItem(ApiHostCertInfo hostCertsItem) {
    if (this.hostCerts == null) {
      this.hostCerts = new ArrayList<>();
    }
    this.hostCerts.add(hostCertsItem);
    return this;
  }

  /**
   * A list of HostCertInfo objects, which associate a hostname with the corresponding certificate and private key. Only used if customCA == true.
   * @return hostCerts
  **/
  @ApiModelProperty(value = "A list of HostCertInfo objects, which associate a hostname with the corresponding certificate and private key. Only used if customCA == true.")

  @Valid

  public List<ApiHostCertInfo> getHostCerts() {
    return hostCerts;
  }

  public void setHostCerts(List<ApiHostCertInfo> hostCerts) {
    this.hostCerts = hostCerts;
  }

  public ApiGenerateCmcaArguments configureAllServices(Boolean configureAllServices) {
    this.configureAllServices = configureAllServices;
    return this;
  }

  /**
   * Whether to configure all existing services to use Auto-TLS. Defaults to false.  If false, only MGMT services will be configured to use Auto-TLS. Use the cluster-level ConfigureAutoTlsServices command to configure Auto-TLS services for a single cluster only.  All future services will be configured to use Auto-TLS regardless of this setting.
   * @return configureAllServices
  **/
  @ApiModelProperty(value = "Whether to configure all existing services to use Auto-TLS. Defaults to false.  If false, only MGMT services will be configured to use Auto-TLS. Use the cluster-level ConfigureAutoTlsServices command to configure Auto-TLS services for a single cluster only.  All future services will be configured to use Auto-TLS regardless of this setting.")


  public Boolean isConfigureAllServices() {
    return configureAllServices;
  }

  public void setConfigureAllServices(Boolean configureAllServices) {
    this.configureAllServices = configureAllServices;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiGenerateCmcaArguments apiGenerateCmcaArguments = (ApiGenerateCmcaArguments) o;
    return Objects.equals(this.location, apiGenerateCmcaArguments.location) &&
        Objects.equals(this.customCA, apiGenerateCmcaArguments.customCA) &&
        Objects.equals(this.interpretAsFilenames, apiGenerateCmcaArguments.interpretAsFilenames) &&
        Objects.equals(this.cmHostCert, apiGenerateCmcaArguments.cmHostCert) &&
        Objects.equals(this.cmHostKey, apiGenerateCmcaArguments.cmHostKey) &&
        Objects.equals(this.caCert, apiGenerateCmcaArguments.caCert) &&
        Objects.equals(this.keystorePasswd, apiGenerateCmcaArguments.keystorePasswd) &&
        Objects.equals(this.truststorePasswd, apiGenerateCmcaArguments.truststorePasswd) &&
        Objects.equals(this.trustedCaCerts, apiGenerateCmcaArguments.trustedCaCerts) &&
        Objects.equals(this.additionalArguments, apiGenerateCmcaArguments.additionalArguments) &&
        Objects.equals(this.hostCerts, apiGenerateCmcaArguments.hostCerts) &&
        Objects.equals(this.configureAllServices, apiGenerateCmcaArguments.configureAllServices) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, customCA, interpretAsFilenames, cmHostCert, cmHostKey, caCert, keystorePasswd, truststorePasswd, trustedCaCerts, additionalArguments, hostCerts, configureAllServices, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiGenerateCmcaArguments {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    customCA: ").append(toIndentedString(customCA)).append("\n");
    sb.append("    interpretAsFilenames: ").append(toIndentedString(interpretAsFilenames)).append("\n");
    sb.append("    cmHostCert: ").append(toIndentedString(cmHostCert)).append("\n");
    sb.append("    cmHostKey: ").append(toIndentedString(cmHostKey)).append("\n");
    sb.append("    caCert: ").append(toIndentedString(caCert)).append("\n");
    sb.append("    keystorePasswd: ").append(toIndentedString(keystorePasswd)).append("\n");
    sb.append("    truststorePasswd: ").append(toIndentedString(truststorePasswd)).append("\n");
    sb.append("    trustedCaCerts: ").append(toIndentedString(trustedCaCerts)).append("\n");
    sb.append("    additionalArguments: ").append(toIndentedString(additionalArguments)).append("\n");
    sb.append("    hostCerts: ").append(toIndentedString(hostCerts)).append("\n");
    sb.append("    configureAllServices: ").append(toIndentedString(configureAllServices)).append("\n");
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

