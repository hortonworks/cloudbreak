/*
 * Cloudera Environments Service
 * Cloudera Environments Service is a web service that manages cloud provider access.
 *
 * The version of the OpenAPI document: __API_VERSION__
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.cloudera.thunderhead.service.environments2api.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Object containing details of encryption parameters for AWS cloud.
 */
@JsonPropertyOrder({
  AwsDiskEncryptionParameters.JSON_PROPERTY_ENCRYPTION_KEY_ARN
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class AwsDiskEncryptionParameters {
  public static final String JSON_PROPERTY_ENCRYPTION_KEY_ARN = "encryptionKeyArn";
  private String encryptionKeyArn;

  public AwsDiskEncryptionParameters() {
  }

  public AwsDiskEncryptionParameters encryptionKeyArn(String encryptionKeyArn) {
    
    this.encryptionKeyArn = encryptionKeyArn;
    return this;
  }

   /**
   * ARN of the CMK which is used to encrypt the AWS EBS volumes.
   * @return encryptionKeyArn
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENCRYPTION_KEY_ARN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getEncryptionKeyArn() {
    return encryptionKeyArn;
  }


  @JsonProperty(JSON_PROPERTY_ENCRYPTION_KEY_ARN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEncryptionKeyArn(String encryptionKeyArn) {
    this.encryptionKeyArn = encryptionKeyArn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AwsDiskEncryptionParameters awsDiskEncryptionParameters = (AwsDiskEncryptionParameters) o;
    return Objects.equals(this.encryptionKeyArn, awsDiskEncryptionParameters.encryptionKeyArn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(encryptionKeyArn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AwsDiskEncryptionParameters {\n");
    sb.append("    encryptionKeyArn: ").append(toIndentedString(encryptionKeyArn)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

