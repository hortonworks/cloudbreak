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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.LinkedHashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Request object for creating an Azure environment using existing VNet and subnets.
 */
@JsonPropertyOrder({
  ExistingAzureNetworkRequest.JSON_PROPERTY_NETWORK_ID,
  ExistingAzureNetworkRequest.JSON_PROPERTY_RESOURCE_GROUP_NAME,
  ExistingAzureNetworkRequest.JSON_PROPERTY_SUBNET_IDS,
  ExistingAzureNetworkRequest.JSON_PROPERTY_NETWORK_NAME,
  ExistingAzureNetworkRequest.JSON_PROPERTY_SUBNET_NAMES,
  ExistingAzureNetworkRequest.JSON_PROPERTY_DATABASE_PRIVATE_DNS_ZONE_ID,
  ExistingAzureNetworkRequest.JSON_PROPERTY_AKS_PRIVATE_DNS_ZONE_ID,
  ExistingAzureNetworkRequest.JSON_PROPERTY_USE_PUBLIC_DNS_FOR_PRIVATE_AKS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.5.0")
public class ExistingAzureNetworkRequest {
  public static final String JSON_PROPERTY_NETWORK_ID = "networkId";
  private String networkId;

  public static final String JSON_PROPERTY_RESOURCE_GROUP_NAME = "resourceGroupName";
  private String resourceGroupName;

  public static final String JSON_PROPERTY_SUBNET_IDS = "subnetIds";
  private Set<String> subnetIds = new LinkedHashSet<>();

  public static final String JSON_PROPERTY_NETWORK_NAME = "networkName";
  private String networkName;

  public static final String JSON_PROPERTY_SUBNET_NAMES = "subnetNames";
  private Set<String> subnetNames = new LinkedHashSet<>();

  public static final String JSON_PROPERTY_DATABASE_PRIVATE_DNS_ZONE_ID = "databasePrivateDnsZoneId";
  private String databasePrivateDnsZoneId;

  public static final String JSON_PROPERTY_AKS_PRIVATE_DNS_ZONE_ID = "aksPrivateDnsZoneId";
  private String aksPrivateDnsZoneId;

  public static final String JSON_PROPERTY_USE_PUBLIC_DNS_FOR_PRIVATE_AKS = "usePublicDnsForPrivateAks";
  private Boolean usePublicDnsForPrivateAks;

  public ExistingAzureNetworkRequest() {
  }

  public ExistingAzureNetworkRequest networkId(String networkId) {
    
    this.networkId = networkId;
    return this;
  }

   /**
   * The id of the Azure VNet.
   * @return networkId
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_NETWORK_ID)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getNetworkId() {
    return networkId;
  }


  @JsonProperty(JSON_PROPERTY_NETWORK_ID)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setNetworkId(String networkId) {
    this.networkId = networkId;
  }


  public ExistingAzureNetworkRequest resourceGroupName(String resourceGroupName) {
    
    this.resourceGroupName = resourceGroupName;
    return this;
  }

   /**
   * The name of the resource group associated with the VNet.
   * @return resourceGroupName
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_RESOURCE_GROUP_NAME)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public String getResourceGroupName() {
    return resourceGroupName;
  }


  @JsonProperty(JSON_PROPERTY_RESOURCE_GROUP_NAME)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setResourceGroupName(String resourceGroupName) {
    this.resourceGroupName = resourceGroupName;
  }


  public ExistingAzureNetworkRequest subnetIds(Set<String> subnetIds) {
    
    this.subnetIds = subnetIds;
    return this;
  }

  public ExistingAzureNetworkRequest addSubnetIdsItem(String subnetIdsItem) {
    if (this.subnetIds == null) {
      this.subnetIds = new LinkedHashSet<>();
    }
    this.subnetIds.add(subnetIdsItem);
    return this;
  }

   /**
   * One or more subnet ids within the VNet.
   * @return subnetIds
  **/
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_SUBNET_IDS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public Set<String> getSubnetIds() {
    return subnetIds;
  }


  @JsonDeserialize(as = LinkedHashSet.class)
  @JsonProperty(JSON_PROPERTY_SUBNET_IDS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setSubnetIds(Set<String> subnetIds) {
    this.subnetIds = subnetIds;
  }


  public ExistingAzureNetworkRequest networkName(String networkName) {
    
    this.networkName = networkName;
    return this;
  }

   /**
   * The name of the Azure VNet.
   * @return networkName
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NETWORK_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getNetworkName() {
    return networkName;
  }


  @JsonProperty(JSON_PROPERTY_NETWORK_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNetworkName(String networkName) {
    this.networkName = networkName;
  }


  public ExistingAzureNetworkRequest subnetNames(Set<String> subnetNames) {
    
    this.subnetNames = subnetNames;
    return this;
  }

  public ExistingAzureNetworkRequest addSubnetNamesItem(String subnetNamesItem) {
    if (this.subnetNames == null) {
      this.subnetNames = new LinkedHashSet<>();
    }
    this.subnetNames.add(subnetNamesItem);
    return this;
  }

   /**
   * One or more subnet names within the VNet.
   * @return subnetNames
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SUBNET_NAMES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Set<String> getSubnetNames() {
    return subnetNames;
  }


  @JsonDeserialize(as = LinkedHashSet.class)
  @JsonProperty(JSON_PROPERTY_SUBNET_NAMES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSubnetNames(Set<String> subnetNames) {
    this.subnetNames = subnetNames;
  }


  public ExistingAzureNetworkRequest databasePrivateDnsZoneId(String databasePrivateDnsZoneId) {
    
    this.databasePrivateDnsZoneId = databasePrivateDnsZoneId;
    return this;
  }

   /**
   * The full Azure resource ID of the existing Private DNS Zone used for Flexible Server and Single Server Databases.
   * @return databasePrivateDnsZoneId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DATABASE_PRIVATE_DNS_ZONE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getDatabasePrivateDnsZoneId() {
    return databasePrivateDnsZoneId;
  }


  @JsonProperty(JSON_PROPERTY_DATABASE_PRIVATE_DNS_ZONE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDatabasePrivateDnsZoneId(String databasePrivateDnsZoneId) {
    this.databasePrivateDnsZoneId = databasePrivateDnsZoneId;
  }


  public ExistingAzureNetworkRequest aksPrivateDnsZoneId(String aksPrivateDnsZoneId) {
    
    this.aksPrivateDnsZoneId = aksPrivateDnsZoneId;
    return this;
  }

   /**
   * The full Azure resource ID of an existing Private DNS zone used for the AKS.
   * @return aksPrivateDnsZoneId
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_AKS_PRIVATE_DNS_ZONE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getAksPrivateDnsZoneId() {
    return aksPrivateDnsZoneId;
  }


  @JsonProperty(JSON_PROPERTY_AKS_PRIVATE_DNS_ZONE_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setAksPrivateDnsZoneId(String aksPrivateDnsZoneId) {
    this.aksPrivateDnsZoneId = aksPrivateDnsZoneId;
  }


  public ExistingAzureNetworkRequest usePublicDnsForPrivateAks(Boolean usePublicDnsForPrivateAks) {
    
    this.usePublicDnsForPrivateAks = usePublicDnsForPrivateAks;
    return this;
  }

   /**
   * Use public DNS for all DNS records in a private cluster.
   * @return usePublicDnsForPrivateAks
  **/
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_USE_PUBLIC_DNS_FOR_PRIVATE_AKS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean getUsePublicDnsForPrivateAks() {
    return usePublicDnsForPrivateAks;
  }


  @JsonProperty(JSON_PROPERTY_USE_PUBLIC_DNS_FOR_PRIVATE_AKS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUsePublicDnsForPrivateAks(Boolean usePublicDnsForPrivateAks) {
    this.usePublicDnsForPrivateAks = usePublicDnsForPrivateAks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExistingAzureNetworkRequest existingAzureNetworkRequest = (ExistingAzureNetworkRequest) o;
    return Objects.equals(this.networkId, existingAzureNetworkRequest.networkId) &&
        Objects.equals(this.resourceGroupName, existingAzureNetworkRequest.resourceGroupName) &&
        Objects.equals(this.subnetIds, existingAzureNetworkRequest.subnetIds) &&
        Objects.equals(this.networkName, existingAzureNetworkRequest.networkName) &&
        Objects.equals(this.subnetNames, existingAzureNetworkRequest.subnetNames) &&
        Objects.equals(this.databasePrivateDnsZoneId, existingAzureNetworkRequest.databasePrivateDnsZoneId) &&
        Objects.equals(this.aksPrivateDnsZoneId, existingAzureNetworkRequest.aksPrivateDnsZoneId) &&
        Objects.equals(this.usePublicDnsForPrivateAks, existingAzureNetworkRequest.usePublicDnsForPrivateAks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(networkId, resourceGroupName, subnetIds, networkName, subnetNames, databasePrivateDnsZoneId, aksPrivateDnsZoneId, usePublicDnsForPrivateAks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExistingAzureNetworkRequest {\n");
    sb.append("    networkId: ").append(toIndentedString(networkId)).append("\n");
    sb.append("    resourceGroupName: ").append(toIndentedString(resourceGroupName)).append("\n");
    sb.append("    subnetIds: ").append(toIndentedString(subnetIds)).append("\n");
    sb.append("    networkName: ").append(toIndentedString(networkName)).append("\n");
    sb.append("    subnetNames: ").append(toIndentedString(subnetNames)).append("\n");
    sb.append("    databasePrivateDnsZoneId: ").append(toIndentedString(databasePrivateDnsZoneId)).append("\n");
    sb.append("    aksPrivateDnsZoneId: ").append(toIndentedString(aksPrivateDnsZoneId)).append("\n");
    sb.append("    usePublicDnsForPrivateAks: ").append(toIndentedString(usePublicDnsForPrivateAks)).append("\n");
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

