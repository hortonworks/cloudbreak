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
 * Arguments to upload CA certificates, client certificates and client key for CSP
 */
@ApiModel(description = "Arguments to upload CA certificates, client certificates and client key for CSP")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiCspArguments   {
  @JsonProperty("caCertContent")
  private String caCertContent = null;

  @JsonProperty("certContent")
  private String certContent = null;

  @JsonProperty("keyContent")
  private String keyContent = null;

  public ApiCspArguments caCertContent(String caCertContent) {
    this.caCertContent = caCertContent;
    return this;
  }

  /**
   * The content of the public CA certificate.
   * @return caCertContent
  **/
  @ApiModelProperty(value = "The content of the public CA certificate.")


  public String getCaCertContent() {
    return caCertContent;
  }

  public void setCaCertContent(String caCertContent) {
    this.caCertContent = caCertContent;
  }

  public ApiCspArguments certContent(String certContent) {
    this.certContent = certContent;
    return this;
  }

  /**
   * The content of the private client certificate.
   * @return certContent
  **/
  @ApiModelProperty(value = "The content of the private client certificate.")


  public String getCertContent() {
    return certContent;
  }

  public void setCertContent(String certContent) {
    this.certContent = certContent;
  }

  public ApiCspArguments keyContent(String keyContent) {
    this.keyContent = keyContent;
    return this;
  }

  /**
   * The content of the private client key.
   * @return keyContent
  **/
  @ApiModelProperty(value = "The content of the private client key.")


  public String getKeyContent() {
    return keyContent;
  }

  public void setKeyContent(String keyContent) {
    this.keyContent = keyContent;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCspArguments apiCspArguments = (ApiCspArguments) o;
    return Objects.equals(this.caCertContent, apiCspArguments.caCertContent) &&
        Objects.equals(this.certContent, apiCspArguments.certContent) &&
        Objects.equals(this.keyContent, apiCspArguments.keyContent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caCertContent, certContent, keyContent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCspArguments {\n");
    
    sb.append("    caCertContent: ").append(toIndentedString(caCertContent)).append("\n");
    sb.append("    certContent: ").append(toIndentedString(certContent)).append("\n");
    sb.append("    keyContent: ").append(toIndentedString(keyContent)).append("\n");
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

