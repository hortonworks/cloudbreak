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
import com.cloudera.thunderhead.service.environments2api.model.Instance;
import com.cloudera.thunderhead.service.environments2api.model.KerberosInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Configuration details specific to the on premises datalake cluster. Only returned when the output view is set to &#39;FULL&#39;.
 */
@JsonPropertyOrder({
  PrivateDatalakeDetails.JSON_PROPERTY_DATALAKE_NAME,
  PrivateDatalakeDetails.JSON_PROPERTY_ENABLE_RANGER_RAZ,
  PrivateDatalakeDetails.JSON_PROPERTY_CREATION_TIME_EPOCH_MILLIS,
  PrivateDatalakeDetails.JSON_PROPERTY_CM_F_Q_D_N,
  PrivateDatalakeDetails.JSON_PROPERTY_CM_I_P,
  PrivateDatalakeDetails.JSON_PROPERTY_CM_SERVER_ID,
  PrivateDatalakeDetails.JSON_PROPERTY_STATUS,
  PrivateDatalakeDetails.JSON_PROPERTY_INSTANCES,
  PrivateDatalakeDetails.JSON_PROPERTY_KERBEROS_INFO
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class PrivateDatalakeDetails {
  public static final String JSON_PROPERTY_DATALAKE_NAME = "datalakeName";
  private String datalakeName;

  public static final String JSON_PROPERTY_ENABLE_RANGER_RAZ = "enableRangerRaz";
  private Boolean enableRangerRaz;

  public static final String JSON_PROPERTY_CREATION_TIME_EPOCH_MILLIS = "creationTimeEpochMillis";
  private Long creationTimeEpochMillis;

  public static final String JSON_PROPERTY_CM_F_Q_D_N = "cmFQDN";
  private String cmFQDN;

  public static final String JSON_PROPERTY_CM_I_P = "cmIP";
  private String cmIP;

  public static final String JSON_PROPERTY_CM_SERVER_ID = "cmServerId";
  private String cmServerId;

  /**
   * Status of the datalake.
   */
  public enum StatusEnum {
    AVAILABLE("AVAILABLE"),
    
    NOT_AVAILABLE("NOT_AVAILABLE");

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_STATUS = "status";
  private StatusEnum status;

  public static final String JSON_PROPERTY_INSTANCES = "instances";
  private List<Instance> instances = new ArrayList<>();

  public static final String JSON_PROPERTY_KERBEROS_INFO = "kerberosInfo";
  private KerberosInfo kerberosInfo;

  public PrivateDatalakeDetails() {
  }

  public PrivateDatalakeDetails datalakeName(String datalakeName) {
    
    this.datalakeName = datalakeName;
    return this;
  }

   /**
   * Name of the datalake.
   * @return datalakeName
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DATALAKE_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getDatalakeName() {
    return datalakeName;
  }


  @JsonProperty(JSON_PROPERTY_DATALAKE_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDatalakeName(String datalakeName) {
    this.datalakeName = datalakeName;
  }


  public PrivateDatalakeDetails enableRangerRaz(Boolean enableRangerRaz) {
    
    this.enableRangerRaz = enableRangerRaz;
    return this;
  }

   /**
   * Whether Ranger RAZ is enabled for the datalake.
   * @return enableRangerRaz
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ENABLE_RANGER_RAZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getEnableRangerRaz() {
    return enableRangerRaz;
  }


  @JsonProperty(JSON_PROPERTY_ENABLE_RANGER_RAZ)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEnableRangerRaz(Boolean enableRangerRaz) {
    this.enableRangerRaz = enableRangerRaz;
  }


  public PrivateDatalakeDetails creationTimeEpochMillis(Long creationTimeEpochMillis) {
    
    this.creationTimeEpochMillis = creationTimeEpochMillis;
    return this;
  }

   /**
   * The timestamp in Unix epoch when the datalake was created.
   * @return creationTimeEpochMillis
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CREATION_TIME_EPOCH_MILLIS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getCreationTimeEpochMillis() {
    return creationTimeEpochMillis;
  }


  @JsonProperty(JSON_PROPERTY_CREATION_TIME_EPOCH_MILLIS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCreationTimeEpochMillis(Long creationTimeEpochMillis) {
    this.creationTimeEpochMillis = creationTimeEpochMillis;
  }


  public PrivateDatalakeDetails cmFQDN(String cmFQDN) {
    
    this.cmFQDN = cmFQDN;
    return this;
  }

   /**
   * The Cloudera Manager FQDN.
   * @return cmFQDN
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CM_F_Q_D_N)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getCmFQDN() {
    return cmFQDN;
  }


  @JsonProperty(JSON_PROPERTY_CM_F_Q_D_N)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCmFQDN(String cmFQDN) {
    this.cmFQDN = cmFQDN;
  }


  public PrivateDatalakeDetails cmIP(String cmIP) {
    
    this.cmIP = cmIP;
    return this;
  }

   /**
   * The Cloudera Manager IP.
   * @return cmIP
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CM_I_P)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getCmIP() {
    return cmIP;
  }


  @JsonProperty(JSON_PROPERTY_CM_I_P)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCmIP(String cmIP) {
    this.cmIP = cmIP;
  }


  public PrivateDatalakeDetails cmServerId(String cmServerId) {
    
    this.cmServerId = cmServerId;
    return this;
  }

   /**
   * The Cloudera Manager server ID.
   * @return cmServerId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CM_SERVER_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getCmServerId() {
    return cmServerId;
  }


  @JsonProperty(JSON_PROPERTY_CM_SERVER_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCmServerId(String cmServerId) {
    this.cmServerId = cmServerId;
  }


  public PrivateDatalakeDetails status(StatusEnum status) {
    
    this.status = status;
    return this;
  }

   /**
   * Status of the datalake.
   * @return status
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public StatusEnum getStatus() {
    return status;
  }


  @JsonProperty(JSON_PROPERTY_STATUS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStatus(StatusEnum status) {
    this.status = status;
  }


  public PrivateDatalakeDetails instances(List<Instance> instances) {
    
    this.instances = instances;
    return this;
  }

  public PrivateDatalakeDetails addInstancesItem(Instance instancesItem) {
    if (this.instances == null) {
      this.instances = new ArrayList<>();
    }
    this.instances.add(instancesItem);
    return this;
  }

   /**
   * Hosts information for instances within the on premises datalake cluster.
   * @return instances
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_INSTANCES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<Instance> getInstances() {
    return instances;
  }


  @JsonProperty(JSON_PROPERTY_INSTANCES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setInstances(List<Instance> instances) {
    this.instances = instances;
  }


  public PrivateDatalakeDetails kerberosInfo(KerberosInfo kerberosInfo) {
    
    this.kerberosInfo = kerberosInfo;
    return this;
  }

   /**
   * Get kerberosInfo
   * @return kerberosInfo
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_KERBEROS_INFO)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public KerberosInfo getKerberosInfo() {
    return kerberosInfo;
  }


  @JsonProperty(JSON_PROPERTY_KERBEROS_INFO)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setKerberosInfo(KerberosInfo kerberosInfo) {
    this.kerberosInfo = kerberosInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrivateDatalakeDetails privateDatalakeDetails = (PrivateDatalakeDetails) o;
    return Objects.equals(this.datalakeName, privateDatalakeDetails.datalakeName) &&
        Objects.equals(this.enableRangerRaz, privateDatalakeDetails.enableRangerRaz) &&
        Objects.equals(this.creationTimeEpochMillis, privateDatalakeDetails.creationTimeEpochMillis) &&
        Objects.equals(this.cmFQDN, privateDatalakeDetails.cmFQDN) &&
        Objects.equals(this.cmIP, privateDatalakeDetails.cmIP) &&
        Objects.equals(this.cmServerId, privateDatalakeDetails.cmServerId) &&
        Objects.equals(this.status, privateDatalakeDetails.status) &&
        Objects.equals(this.instances, privateDatalakeDetails.instances) &&
        Objects.equals(this.kerberosInfo, privateDatalakeDetails.kerberosInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datalakeName, enableRangerRaz, creationTimeEpochMillis, cmFQDN, cmIP, cmServerId, status, instances, kerberosInfo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PrivateDatalakeDetails {\n");
    sb.append("    datalakeName: ").append(toIndentedString(datalakeName)).append("\n");
    sb.append("    enableRangerRaz: ").append(toIndentedString(enableRangerRaz)).append("\n");
    sb.append("    creationTimeEpochMillis: ").append(toIndentedString(creationTimeEpochMillis)).append("\n");
    sb.append("    cmFQDN: ").append(toIndentedString(cmFQDN)).append("\n");
    sb.append("    cmIP: ").append(toIndentedString(cmIP)).append("\n");
    sb.append("    cmServerId: ").append(toIndentedString(cmServerId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    instances: ").append(toIndentedString(instances)).append("\n");
    sb.append("    kerberosInfo: ").append(toIndentedString(kerberosInfo)).append("\n");
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

