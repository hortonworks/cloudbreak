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
 * Associates a hostname with its corresponding certificate and private key
 */
@ApiModel(description = "Associates a hostname with its corresponding certificate and private key")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHostCertInfo   {
  @JsonProperty("hostname")
  private String hostname = null;

  @JsonProperty("certificate")
  private String certificate = null;

  @JsonProperty("key")
  private String key = null;

  @JsonProperty("subjectAltNames")
  @Valid
  private List<String> subjectAltNames = null;

  public ApiHostCertInfo hostname(String hostname) {
    this.hostname = hostname;
    return this;
  }

  /**
   * The FQDN of a host in the deployment.
   * @return hostname
  **/
  @ApiModelProperty(value = "The FQDN of a host in the deployment.")


  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public ApiHostCertInfo certificate(String certificate) {
    this.certificate = certificate;
    return this;
  }

  /**
   * The certificate for this host in PEM format.
   * @return certificate
  **/
  @ApiModelProperty(example = "host-cert.pem", value = "The certificate for this host in PEM format.")


  public String getCertificate() {
    return certificate;
  }

  public void setCertificate(String certificate) {
    this.certificate = certificate;
  }

  public ApiHostCertInfo key(String key) {
    this.key = key;
    return this;
  }

  /**
   * The private key for this host in PEM format.
   * @return key
  **/
  @ApiModelProperty(example = "host-key.pem", value = "The private key for this host in PEM format.")


  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public ApiHostCertInfo subjectAltNames(List<String> subjectAltNames) {
    this.subjectAltNames = subjectAltNames;
    return this;
  }

  public ApiHostCertInfo addSubjectAltNamesItem(String subjectAltNamesItem) {
    if (this.subjectAltNames == null) {
      this.subjectAltNames = new ArrayList<>();
    }
    this.subjectAltNames.add(subjectAltNamesItem);
    return this;
  }

  /**
   * A list of alt names for a host.
   * @return subjectAltNames
  **/
  @ApiModelProperty(example = "\"DNS:example.cloudera.com\"", value = "A list of alt names for a host.")


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
    ApiHostCertInfo apiHostCertInfo = (ApiHostCertInfo) o;
    return Objects.equals(this.hostname, apiHostCertInfo.hostname) &&
        Objects.equals(this.certificate, apiHostCertInfo.certificate) &&
        Objects.equals(this.key, apiHostCertInfo.key) &&
        Objects.equals(this.subjectAltNames, apiHostCertInfo.subjectAltNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostname, certificate, key, subjectAltNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHostCertInfo {\n");
    
    sb.append("    hostname: ").append(toIndentedString(hostname)).append("\n");
    sb.append("    certificate: ").append(toIndentedString(certificate)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
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

