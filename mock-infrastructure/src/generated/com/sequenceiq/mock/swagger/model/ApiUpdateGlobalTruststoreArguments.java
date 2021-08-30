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
 * Arguments to update certificates from the Auto-TLS truststore
 */
@ApiModel(description = "Arguments to update certificates from the Auto-TLS truststore")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiUpdateGlobalTruststoreArguments   {
  @JsonProperty("newCertLocation")
  private String newCertLocation = null;

  @JsonProperty("newCertContent")
  private String newCertContent = null;

  public ApiUpdateGlobalTruststoreArguments newCertLocation(String newCertLocation) {
    this.newCertLocation = newCertLocation;
    return this;
  }

  /**
   * The location of the new certificate on the disk to be added to the Auto-TLS truststore. PEM format is required. Specify either this or a NewCertContent.
   * @return newCertLocation
  **/
  @ApiModelProperty(value = "The location of the new certificate on the disk to be added to the Auto-TLS truststore. PEM format is required. Specify either this or a NewCertContent.")


  public String getNewCertLocation() {
    return newCertLocation;
  }

  public void setNewCertLocation(String newCertLocation) {
    this.newCertLocation = newCertLocation;
  }

  public ApiUpdateGlobalTruststoreArguments newCertContent(String newCertContent) {
    this.newCertContent = newCertContent;
    return this;
  }

  /**
   * The content of the new certificate to be added to the Auto-TLS truststore. Specify either this or a NewCertLocation. <br> The certificate, if specified, needs to be a standard PEM-encoded key as a single string, with all line breaks replaced with the line-feed control character '\\n'. <br> A value will typically look like the following string: <br> -----BEGIN CERTIFICATE-----\\n[base-64 encoded key]\\n-----END CERTIFICATE----- <br>
   * @return newCertContent
  **/
  @ApiModelProperty(value = "The content of the new certificate to be added to the Auto-TLS truststore. Specify either this or a NewCertLocation. <br> The certificate, if specified, needs to be a standard PEM-encoded key as a single string, with all line breaks replaced with the line-feed control character '\\n'. <br> A value will typically look like the following string: <br> -----BEGIN CERTIFICATE-----\\n[base-64 encoded key]\\n-----END CERTIFICATE----- <br>")


  public String getNewCertContent() {
    return newCertContent;
  }

  public void setNewCertContent(String newCertContent) {
    this.newCertContent = newCertContent;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiUpdateGlobalTruststoreArguments apiUpdateGlobalTruststoreArguments = (ApiUpdateGlobalTruststoreArguments) o;
    return Objects.equals(this.newCertLocation, apiUpdateGlobalTruststoreArguments.newCertLocation) &&
        Objects.equals(this.newCertContent, apiUpdateGlobalTruststoreArguments.newCertContent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(newCertLocation, newCertContent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiUpdateGlobalTruststoreArguments {\n");
    
    sb.append("    newCertLocation: ").append(toIndentedString(newCertLocation)).append("\n");
    sb.append("    newCertContent: ").append(toIndentedString(newCertContent)).append("\n");
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

