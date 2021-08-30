package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiHostCertInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments to add custom certificates to the Auto-TLS certificate database
 */
@ApiModel(description = "Arguments to add custom certificates to the Auto-TLS certificate database")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiAddCustomCertsArguments   {
  @JsonProperty("location")
  private String location = null;

  @JsonProperty("interpretAsFilenames")
  private Boolean interpretAsFilenames = null;

  @JsonProperty("hostCerts")
  @Valid
  private List<ApiHostCertInfo> hostCerts = null;

  public ApiAddCustomCertsArguments location(String location) {
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

  public ApiAddCustomCertsArguments interpretAsFilenames(Boolean interpretAsFilenames) {
    this.interpretAsFilenames = interpretAsFilenames;
    return this;
  }

  /**
   * Whether the following arguments are interpreted as filenames local to the Cloudera Manager host (true, default) or as the actual data for that argument: * hostCerts.hostCert * hostCerts.hostKey  If HTTPS has not been enabled on the Cloudera Manager Admin Console and API, we *strongly* recommend that you pass the arguments as filenames local to the Cloudera Manager host (i.e. set to true) to avoid leaking sensitive information over the wire in plaintext.
   * @return interpretAsFilenames
  **/
  @ApiModelProperty(example = "true", value = "Whether the following arguments are interpreted as filenames local to the Cloudera Manager host (true, default) or as the actual data for that argument: * hostCerts.hostCert * hostCerts.hostKey  If HTTPS has not been enabled on the Cloudera Manager Admin Console and API, we *strongly* recommend that you pass the arguments as filenames local to the Cloudera Manager host (i.e. set to true) to avoid leaking sensitive information over the wire in plaintext.")


  public Boolean isInterpretAsFilenames() {
    return interpretAsFilenames;
  }

  public void setInterpretAsFilenames(Boolean interpretAsFilenames) {
    this.interpretAsFilenames = interpretAsFilenames;
  }

  public ApiAddCustomCertsArguments hostCerts(List<ApiHostCertInfo> hostCerts) {
    this.hostCerts = hostCerts;
    return this;
  }

  public ApiAddCustomCertsArguments addHostCertsItem(ApiHostCertInfo hostCertsItem) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiAddCustomCertsArguments apiAddCustomCertsArguments = (ApiAddCustomCertsArguments) o;
    return Objects.equals(this.location, apiAddCustomCertsArguments.location) &&
        Objects.equals(this.interpretAsFilenames, apiAddCustomCertsArguments.interpretAsFilenames) &&
        Objects.equals(this.hostCerts, apiAddCustomCertsArguments.hostCerts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, interpretAsFilenames, hostCerts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiAddCustomCertsArguments {\n");
    
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    interpretAsFilenames: ").append(toIndentedString(interpretAsFilenames)).append("\n");
    sb.append("    hostCerts: ").append(toIndentedString(hostCerts)).append("\n");
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

