package com.sequenceiq.mock.swagger.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A cluster represents a set of interdependent services running on a set of hosts. All services on a given cluster are of the same software version (e.g. CDH4 or CDH5).
 */
@ApiModel(description = "A cluster represents a set of interdependent services running on a set of hosts. All services on a given cluster are of the same software version (e.g. CDH4 or CDH5).")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiCluster   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("version")
  private ApiClusterVersion version = null;

  @JsonProperty("fullVersion")
  private String fullVersion = null;

  @JsonProperty("maintenanceMode")
  private Boolean maintenanceMode = null;

  @JsonProperty("maintenanceOwners")
  @Valid
  private List<ApiEntityType> maintenanceOwners = null;

  @JsonProperty("services")
  @Valid
  private List<ApiService> services = null;

  @JsonProperty("parcels")
  @Valid
  private List<ApiParcel> parcels = null;

  @JsonProperty("clusterUrl")
  private String clusterUrl = null;

  @JsonProperty("hostsUrl")
  private String hostsUrl = null;

  @JsonProperty("entityStatus")
  private ApiEntityStatus entityStatus = null;

  @JsonProperty("uuid")
  private String uuid = null;

  @JsonProperty("dataContextRefs")
  @Valid
  private List<ApiDataContextRef> dataContextRefs = null;

  @JsonProperty("clusterType")
  private String clusterType = null;

  @JsonProperty("tags")
  @Valid
  private List<ApiEntityTag> tags = null;

  public ApiCluster name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the cluster. <p> Immutable since API v6. <p> Prior to API v6, will contain the display name of the cluster.
   * @return name
  **/
  @ApiModelProperty(value = "The name of the cluster. <p> Immutable since API v6. <p> Prior to API v6, will contain the display name of the cluster.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiCluster displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * The display name of the cluster that is shown in the UI. <p> Available since API v6.
   * @return displayName
  **/
  @ApiModelProperty(value = "The display name of the cluster that is shown in the UI. <p> Available since API v6.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiCluster version(ApiClusterVersion version) {
    this.version = version;
    return this;
  }

  /**
   * The CDH version of the cluster.
   * @return version
  **/
  @ApiModelProperty(value = "The CDH version of the cluster.")

  @Valid

  public ApiClusterVersion getVersion() {
    return version;
  }

  public void setVersion(ApiClusterVersion version) {
    this.version = version;
  }

  public ApiCluster fullVersion(String fullVersion) {
    this.fullVersion = fullVersion;
    return this;
  }

  /**
   * The full CDH version of the cluster. The expected format is three dot separated version numbers, e.g. \"4.2.1\" or \"5.0.0\". The full version takes precedence over the version field during cluster creation. <p> Available since API v6.
   * @return fullVersion
  **/
  @ApiModelProperty(value = "The full CDH version of the cluster. The expected format is three dot separated version numbers, e.g. \"4.2.1\" or \"5.0.0\". The full version takes precedence over the version field during cluster creation. <p> Available since API v6.")


  public String getFullVersion() {
    return fullVersion;
  }

  public void setFullVersion(String fullVersion) {
    this.fullVersion = fullVersion;
  }

  public ApiCluster maintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
    return this;
  }

  /**
   * Readonly. Whether the cluster is in maintenance mode. Available since API v2.
   * @return maintenanceMode
  **/
  @ApiModelProperty(value = "Readonly. Whether the cluster is in maintenance mode. Available since API v2.")


  public Boolean isMaintenanceMode() {
    return maintenanceMode;
  }

  public void setMaintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  public ApiCluster maintenanceOwners(List<ApiEntityType> maintenanceOwners) {
    this.maintenanceOwners = maintenanceOwners;
    return this;
  }

  public ApiCluster addMaintenanceOwnersItem(ApiEntityType maintenanceOwnersItem) {
    if (this.maintenanceOwners == null) {
      this.maintenanceOwners = new ArrayList<>();
    }
    this.maintenanceOwners.add(maintenanceOwnersItem);
    return this;
  }

  /**
   * Readonly. The list of objects that trigger this cluster to be in maintenance mode. Available since API v2.
   * @return maintenanceOwners
  **/
  @ApiModelProperty(value = "Readonly. The list of objects that trigger this cluster to be in maintenance mode. Available since API v2.")

  @Valid

  public List<ApiEntityType> getMaintenanceOwners() {
    return maintenanceOwners;
  }

  public void setMaintenanceOwners(List<ApiEntityType> maintenanceOwners) {
    this.maintenanceOwners = maintenanceOwners;
  }

  public ApiCluster services(List<ApiService> services) {
    this.services = services;
    return this;
  }

  public ApiCluster addServicesItem(ApiService servicesItem) {
    if (this.services == null) {
      this.services = new ArrayList<>();
    }
    this.services.add(servicesItem);
    return this;
  }

  /**
   * Optional. Used during import/export of settings.
   * @return services
  **/
  @ApiModelProperty(value = "Optional. Used during import/export of settings.")

  @Valid

  public List<ApiService> getServices() {
    return services;
  }

  public void setServices(List<ApiService> services) {
    this.services = services;
  }

  public ApiCluster parcels(List<ApiParcel> parcels) {
    this.parcels = parcels;
    return this;
  }

  public ApiCluster addParcelsItem(ApiParcel parcelsItem) {
    if (this.parcels == null) {
      this.parcels = new ArrayList<>();
    }
    this.parcels.add(parcelsItem);
    return this;
  }

  /**
   * Optional. Used during import/export of settings. Available since API v4.
   * @return parcels
  **/
  @ApiModelProperty(value = "Optional. Used during import/export of settings. Available since API v4.")

  @Valid

  public List<ApiParcel> getParcels() {
    return parcels;
  }

  public void setParcels(List<ApiParcel> parcels) {
    this.parcels = parcels;
  }

  public ApiCluster clusterUrl(String clusterUrl) {
    this.clusterUrl = clusterUrl;
    return this;
  }

  /**
   * Readonly. Link into the Cloudera Manager web UI for this specific cluster. <p> Available since API v10.
   * @return clusterUrl
  **/
  @ApiModelProperty(value = "Readonly. Link into the Cloudera Manager web UI for this specific cluster. <p> Available since API v10.")


  public String getClusterUrl() {
    return clusterUrl;
  }

  public void setClusterUrl(String clusterUrl) {
    this.clusterUrl = clusterUrl;
  }

  public ApiCluster hostsUrl(String hostsUrl) {
    this.hostsUrl = hostsUrl;
    return this;
  }

  /**
   * Readonly. Link into the Cloudera Manager web UI for host table for this cluster. <p> Available since API v11.
   * @return hostsUrl
  **/
  @ApiModelProperty(value = "Readonly. Link into the Cloudera Manager web UI for host table for this cluster. <p> Available since API v11.")


  public String getHostsUrl() {
    return hostsUrl;
  }

  public void setHostsUrl(String hostsUrl) {
    this.hostsUrl = hostsUrl;
  }

  public ApiCluster entityStatus(ApiEntityStatus entityStatus) {
    this.entityStatus = entityStatus;
    return this;
  }

  /**
   * Readonly. The entity status for this cluster. Available since API v11.
   * @return entityStatus
  **/
  @ApiModelProperty(value = "Readonly. The entity status for this cluster. Available since API v11.")

  @Valid

  public ApiEntityStatus getEntityStatus() {
    return entityStatus;
  }

  public void setEntityStatus(ApiEntityStatus entityStatus) {
    this.entityStatus = entityStatus;
  }

  public ApiCluster uuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * Readonly. The UUID of the cluster. <p> Available since API v15.
   * @return uuid
  **/
  @ApiModelProperty(value = "Readonly. The UUID of the cluster. <p> Available since API v15.")


  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public ApiCluster dataContextRefs(List<ApiDataContextRef> dataContextRefs) {
    this.dataContextRefs = dataContextRefs;
    return this;
  }

  public ApiCluster addDataContextRefsItem(ApiDataContextRef dataContextRefsItem) {
    if (this.dataContextRefs == null) {
      this.dataContextRefs = new ArrayList<>();
    }
    this.dataContextRefs.add(dataContextRefsItem);
    return this;
  }

  /**
   * 
   * @return dataContextRefs
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiDataContextRef> getDataContextRefs() {
    return dataContextRefs;
  }

  public void setDataContextRefs(List<ApiDataContextRef> dataContextRefs) {
    this.dataContextRefs = dataContextRefs;
  }

  public ApiCluster clusterType(String clusterType) {
    this.clusterType = clusterType;
    return this;
  }

  /**
   * The type of cluster. If unspecified, defaults to either BASE_CLUSTER (if no data contexts are provided) or COMPUTE_CLUSTER (if one or more data contexts are provided). Available since APIv32.
   * @return clusterType
  **/
  @ApiModelProperty(value = "The type of cluster. If unspecified, defaults to either BASE_CLUSTER (if no data contexts are provided) or COMPUTE_CLUSTER (if one or more data contexts are provided). Available since APIv32.")


  public String getClusterType() {
    return clusterType;
  }

  public void setClusterType(String clusterType) {
    this.clusterType = clusterType;
  }

  public ApiCluster tags(List<ApiEntityTag> tags) {
    this.tags = tags;
    return this;
  }

  public ApiCluster addTagsItem(ApiEntityTag tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Tags associated with the cluster. Available since V41.
   * @return tags
  **/
  @ApiModelProperty(value = "Tags associated with the cluster. Available since V41.")

  @Valid

  public List<ApiEntityTag> getTags() {
    return tags;
  }

  public void setTags(List<ApiEntityTag> tags) {
    this.tags = tags;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCluster apiCluster = (ApiCluster) o;
    return Objects.equals(this.name, apiCluster.name) &&
        Objects.equals(this.displayName, apiCluster.displayName) &&
        Objects.equals(this.version, apiCluster.version) &&
        Objects.equals(this.fullVersion, apiCluster.fullVersion) &&
        Objects.equals(this.maintenanceMode, apiCluster.maintenanceMode) &&
        Objects.equals(this.maintenanceOwners, apiCluster.maintenanceOwners) &&
        Objects.equals(this.services, apiCluster.services) &&
        Objects.equals(this.parcels, apiCluster.parcels) &&
        Objects.equals(this.clusterUrl, apiCluster.clusterUrl) &&
        Objects.equals(this.hostsUrl, apiCluster.hostsUrl) &&
        Objects.equals(this.entityStatus, apiCluster.entityStatus) &&
        Objects.equals(this.uuid, apiCluster.uuid) &&
        Objects.equals(this.dataContextRefs, apiCluster.dataContextRefs) &&
        Objects.equals(this.clusterType, apiCluster.clusterType) &&
        Objects.equals(this.tags, apiCluster.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, displayName, version, fullVersion, maintenanceMode, maintenanceOwners, services, parcels, clusterUrl, hostsUrl, entityStatus, uuid, dataContextRefs, clusterType, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCluster {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    fullVersion: ").append(toIndentedString(fullVersion)).append("\n");
    sb.append("    maintenanceMode: ").append(toIndentedString(maintenanceMode)).append("\n");
    sb.append("    maintenanceOwners: ").append(toIndentedString(maintenanceOwners)).append("\n");
    sb.append("    services: ").append(toIndentedString(services)).append("\n");
    sb.append("    parcels: ").append(toIndentedString(parcels)).append("\n");
    sb.append("    clusterUrl: ").append(toIndentedString(clusterUrl)).append("\n");
    sb.append("    hostsUrl: ").append(toIndentedString(hostsUrl)).append("\n");
    sb.append("    entityStatus: ").append(toIndentedString(entityStatus)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    dataContextRefs: ").append(toIndentedString(dataContextRefs)).append("\n");
    sb.append("    clusterType: ").append(toIndentedString(clusterType)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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

