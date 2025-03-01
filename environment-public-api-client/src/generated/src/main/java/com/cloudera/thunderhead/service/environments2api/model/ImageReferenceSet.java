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
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Detailed set of cloud providers region to image mappings.
 */
@JsonPropertyOrder({
  ImageReferenceSet.JSON_PROPERTY_AWS,
  ImageReferenceSet.JSON_PROPERTY_AZURE,
  ImageReferenceSet.JSON_PROPERTY_GCP
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class ImageReferenceSet {
  public static final String JSON_PROPERTY_AWS = "aws";
  private Map<String, String> aws = new HashMap<>();

  public static final String JSON_PROPERTY_AZURE = "azure";
  private Map<String, String> azure = new HashMap<>();

  public static final String JSON_PROPERTY_GCP = "gcp";
  private Map<String, String> gcp = new HashMap<>();

  public ImageReferenceSet() {
  }

  public ImageReferenceSet aws(Map<String, String> aws) {
    
    this.aws = aws;
    return this;
  }

  public ImageReferenceSet putAwsItem(String key, String awsItem) {
    if (this.aws == null) {
      this.aws = new HashMap<>();
    }
    this.aws.put(key, awsItem);
    return this;
  }

   /**
   * AWS-related region-to-image mappings.
   * @return aws
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_AWS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Map<String, String> getAws() {
    return aws;
  }


  @JsonProperty(JSON_PROPERTY_AWS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAws(Map<String, String> aws) {
    this.aws = aws;
  }


  public ImageReferenceSet azure(Map<String, String> azure) {
    
    this.azure = azure;
    return this;
  }

  public ImageReferenceSet putAzureItem(String key, String azureItem) {
    if (this.azure == null) {
      this.azure = new HashMap<>();
    }
    this.azure.put(key, azureItem);
    return this;
  }

   /**
   * Azure-related region-to-image mappings.
   * @return azure
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_AZURE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Map<String, String> getAzure() {
    return azure;
  }


  @JsonProperty(JSON_PROPERTY_AZURE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAzure(Map<String, String> azure) {
    this.azure = azure;
  }


  public ImageReferenceSet gcp(Map<String, String> gcp) {
    
    this.gcp = gcp;
    return this;
  }

  public ImageReferenceSet putGcpItem(String key, String gcpItem) {
    if (this.gcp == null) {
      this.gcp = new HashMap<>();
    }
    this.gcp.put(key, gcpItem);
    return this;
  }

   /**
   * GCP-related region-to-image mappings.
   * @return gcp
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_GCP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Map<String, String> getGcp() {
    return gcp;
  }


  @JsonProperty(JSON_PROPERTY_GCP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setGcp(Map<String, String> gcp) {
    this.gcp = gcp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImageReferenceSet imageReferenceSet = (ImageReferenceSet) o;
    return Objects.equals(this.aws, imageReferenceSet.aws) &&
        Objects.equals(this.azure, imageReferenceSet.azure) &&
        Objects.equals(this.gcp, imageReferenceSet.gcp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aws, azure, gcp);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImageReferenceSet {\n");
    sb.append("    aws: ").append(toIndentedString(aws)).append("\n");
    sb.append("    azure: ").append(toIndentedString(azure)).append("\n");
    sb.append("    gcp: ").append(toIndentedString(gcp)).append("\n");
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

