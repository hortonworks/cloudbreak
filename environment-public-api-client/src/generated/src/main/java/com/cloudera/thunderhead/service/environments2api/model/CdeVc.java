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
 * The CML virtual cluster.
 */
@JsonPropertyOrder({
  CdeVc.JSON_PROPERTY_VC_ID,
  CdeVc.JSON_PROPERTY_VC_NAME,
  CdeVc.JSON_PROPERTY_CLUSTER_ID,
  CdeVc.JSON_PROPERTY_STATUS,
  CdeVc.JSON_PROPERTY_VC_UI_URL
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class CdeVc {
  public static final String JSON_PROPERTY_VC_ID = "vcId";
  private String vcId;

  public static final String JSON_PROPERTY_VC_NAME = "vcName";
  private String vcName;

  public static final String JSON_PROPERTY_CLUSTER_ID = "clusterId";
  private String clusterId;

  public static final String JSON_PROPERTY_STATUS = "status";
  private String status;

  public static final String JSON_PROPERTY_VC_UI_URL = "vcUiUrl";
  private String vcUiUrl;

  public CdeVc() {
  }

  public CdeVc vcId(String vcId) {
    
    this.vcId = vcId;
    return this;
  }

   /**
   * Virtual Cluster ID.
   * @return vcId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_VC_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getVcId() {
    return vcId;
  }


  @JsonProperty(JSON_PROPERTY_VC_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setVcId(String vcId) {
    this.vcId = vcId;
  }


  public CdeVc vcName(String vcName) {
    
    this.vcName = vcName;
    return this;
  }

   /**
   * Virtual Cluster Name.
   * @return vcName
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_VC_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getVcName() {
    return vcName;
  }


  @JsonProperty(JSON_PROPERTY_VC_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setVcName(String vcName) {
    this.vcName = vcName;
  }


  public CdeVc clusterId(String clusterId) {
    
    this.clusterId = clusterId;
    return this;
  }

   /**
   * Cluster ID of the CDE service that contains the virtual cluster.
   * @return clusterId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CLUSTER_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getClusterId() {
    return clusterId;
  }


  @JsonProperty(JSON_PROPERTY_CLUSTER_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setClusterId(String clusterId) {
    this.clusterId = clusterId;
  }


  public CdeVc status(String status) {
    
    this.status = status;
    return this;
  }

   /**
   * Status of the Virtual Cluster.
   * @return status
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getStatus() {
    return status;
  }


  @JsonProperty(JSON_PROPERTY_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStatus(String status) {
    this.status = status;
  }


  public CdeVc vcUiUrl(String vcUiUrl) {
    
    this.vcUiUrl = vcUiUrl;
    return this;
  }

   /**
   * Url for the Virtual Cluster UI.
   * @return vcUiUrl
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_VC_UI_URL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getVcUiUrl() {
    return vcUiUrl;
  }


  @JsonProperty(JSON_PROPERTY_VC_UI_URL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setVcUiUrl(String vcUiUrl) {
    this.vcUiUrl = vcUiUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CdeVc cdeVc = (CdeVc) o;
    return Objects.equals(this.vcId, cdeVc.vcId) &&
        Objects.equals(this.vcName, cdeVc.vcName) &&
        Objects.equals(this.clusterId, cdeVc.clusterId) &&
        Objects.equals(this.status, cdeVc.status) &&
        Objects.equals(this.vcUiUrl, cdeVc.vcUiUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vcId, vcName, clusterId, status, vcUiUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CdeVc {\n");
    sb.append("    vcId: ").append(toIndentedString(vcId)).append("\n");
    sb.append("    vcName: ").append(toIndentedString(vcName)).append("\n");
    sb.append("    clusterId: ").append(toIndentedString(clusterId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    vcUiUrl: ").append(toIndentedString(vcUiUrl)).append("\n");
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

