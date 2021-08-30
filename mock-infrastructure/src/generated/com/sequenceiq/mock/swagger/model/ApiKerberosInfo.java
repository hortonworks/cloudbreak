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
 * Kerberos information of a Cluster or Cloudera Manager.
 */
@ApiModel(description = "Kerberos information of a Cluster or Cloudera Manager.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiKerberosInfo   {
  @JsonProperty("kerberized")
  private Boolean kerberized = null;

  @JsonProperty("kdcType")
  private String kdcType = null;

  @JsonProperty("kerberosRealm")
  private String kerberosRealm = null;

  @JsonProperty("kdcHost")
  private String kdcHost = null;

  @JsonProperty("adminHost")
  private String adminHost = null;

  @JsonProperty("domain")
  @Valid
  private List<String> domain = null;

  public ApiKerberosInfo kerberized(Boolean kerberized) {
    this.kerberized = kerberized;
    return this;
  }

  /**
   * 
   * @return kerberized
  **/
  @ApiModelProperty(value = "")


  public Boolean isKerberized() {
    return kerberized;
  }

  public void setKerberized(Boolean kerberized) {
    this.kerberized = kerberized;
  }

  public ApiKerberosInfo kdcType(String kdcType) {
    this.kdcType = kdcType;
    return this;
  }

  /**
   * 
   * @return kdcType
  **/
  @ApiModelProperty(value = "")


  public String getKdcType() {
    return kdcType;
  }

  public void setKdcType(String kdcType) {
    this.kdcType = kdcType;
  }

  public ApiKerberosInfo kerberosRealm(String kerberosRealm) {
    this.kerberosRealm = kerberosRealm;
    return this;
  }

  /**
   * 
   * @return kerberosRealm
  **/
  @ApiModelProperty(value = "")


  public String getKerberosRealm() {
    return kerberosRealm;
  }

  public void setKerberosRealm(String kerberosRealm) {
    this.kerberosRealm = kerberosRealm;
  }

  public ApiKerberosInfo kdcHost(String kdcHost) {
    this.kdcHost = kdcHost;
    return this;
  }

  /**
   * 
   * @return kdcHost
  **/
  @ApiModelProperty(value = "")


  public String getKdcHost() {
    return kdcHost;
  }

  public void setKdcHost(String kdcHost) {
    this.kdcHost = kdcHost;
  }

  public ApiKerberosInfo adminHost(String adminHost) {
    this.adminHost = adminHost;
    return this;
  }

  /**
   * 
   * @return adminHost
  **/
  @ApiModelProperty(value = "")


  public String getAdminHost() {
    return adminHost;
  }

  public void setAdminHost(String adminHost) {
    this.adminHost = adminHost;
  }

  public ApiKerberosInfo domain(List<String> domain) {
    this.domain = domain;
    return this;
  }

  public ApiKerberosInfo addDomainItem(String domainItem) {
    if (this.domain == null) {
      this.domain = new ArrayList<>();
    }
    this.domain.add(domainItem);
    return this;
  }

  /**
   * 
   * @return domain
  **/
  @ApiModelProperty(value = "")


  public List<String> getDomain() {
    return domain;
  }

  public void setDomain(List<String> domain) {
    this.domain = domain;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiKerberosInfo apiKerberosInfo = (ApiKerberosInfo) o;
    return Objects.equals(this.kerberized, apiKerberosInfo.kerberized) &&
        Objects.equals(this.kdcType, apiKerberosInfo.kdcType) &&
        Objects.equals(this.kerberosRealm, apiKerberosInfo.kerberosRealm) &&
        Objects.equals(this.kdcHost, apiKerberosInfo.kdcHost) &&
        Objects.equals(this.adminHost, apiKerberosInfo.adminHost) &&
        Objects.equals(this.domain, apiKerberosInfo.domain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kerberized, kdcType, kerberosRealm, kdcHost, adminHost, domain);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiKerberosInfo {\n");
    
    sb.append("    kerberized: ").append(toIndentedString(kerberized)).append("\n");
    sb.append("    kdcType: ").append(toIndentedString(kdcType)).append("\n");
    sb.append("    kerberosRealm: ").append(toIndentedString(kerberosRealm)).append("\n");
    sb.append("    kdcHost: ").append(toIndentedString(kdcHost)).append("\n");
    sb.append("    adminHost: ").append(toIndentedString(adminHost)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
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

