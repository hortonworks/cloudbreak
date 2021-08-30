package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiClusterRef;
import com.sequenceiq.mock.swagger.model.ApiConfigStalenessStatus;
import com.sequenceiq.mock.swagger.model.ApiEntityStatus;
import com.sequenceiq.mock.swagger.model.ApiEntityTag;
import com.sequenceiq.mock.swagger.model.ApiEntityType;
import com.sequenceiq.mock.swagger.model.ApiHealthCheck;
import com.sequenceiq.mock.swagger.model.ApiHealthSummary;
import com.sequenceiq.mock.swagger.model.ApiReplicationSchedule;
import com.sequenceiq.mock.swagger.model.ApiRole;
import com.sequenceiq.mock.swagger.model.ApiRoleConfigGroup;
import com.sequenceiq.mock.swagger.model.ApiServiceConfig;
import com.sequenceiq.mock.swagger.model.ApiServiceState;
import com.sequenceiq.mock.swagger.model.ApiSnapshotPolicy;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A service (such as HDFS, MapReduce, HBase) runs in a cluster. It has roles, which are the actual entities (NameNode, DataNodes, etc.) that perform the service&#39;s functions.  &lt;h3&gt;HDFS services and health checks&lt;/h3&gt;  In CDH4, HDFS services may not present any health checks. This will happen if the service has more than one nameservice configured. In those cases, the health information will be available by fetching information about the nameservices instead. &lt;p&gt; The health summary is still available, and reflects a service-wide summary.
 */
@ApiModel(description = "A service (such as HDFS, MapReduce, HBase) runs in a cluster. It has roles, which are the actual entities (NameNode, DataNodes, etc.) that perform the service's functions.  <h3>HDFS services and health checks</h3>  In CDH4, HDFS services may not present any health checks. This will happen if the service has more than one nameservice configured. In those cases, the health information will be available by fetching information about the nameservices instead. <p> The health summary is still available, and reflects a service-wide summary.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiService   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("clusterRef")
  private ApiClusterRef clusterRef = null;

  @JsonProperty("serviceState")
  private ApiServiceState serviceState = null;

  @JsonProperty("healthSummary")
  private ApiHealthSummary healthSummary = null;

  @JsonProperty("configStale")
  private Boolean configStale = null;

  @JsonProperty("configStalenessStatus")
  private ApiConfigStalenessStatus configStalenessStatus = null;

  @JsonProperty("clientConfigStalenessStatus")
  private ApiConfigStalenessStatus clientConfigStalenessStatus = null;

  @JsonProperty("healthChecks")
  @Valid
  private List<ApiHealthCheck> healthChecks = null;

  @JsonProperty("serviceUrl")
  private String serviceUrl = null;

  @JsonProperty("roleInstancesUrl")
  private String roleInstancesUrl = null;

  @JsonProperty("maintenanceMode")
  private Boolean maintenanceMode = null;

  @JsonProperty("maintenanceOwners")
  @Valid
  private List<ApiEntityType> maintenanceOwners = null;

  @JsonProperty("config")
  private ApiServiceConfig config = null;

  @JsonProperty("roles")
  @Valid
  private List<ApiRole> roles = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("roleConfigGroups")
  @Valid
  private List<ApiRoleConfigGroup> roleConfigGroups = null;

  @JsonProperty("replicationSchedules")
  @Valid
  private List<ApiReplicationSchedule> replicationSchedules = null;

  @JsonProperty("snapshotPolicies")
  @Valid
  private List<ApiSnapshotPolicy> snapshotPolicies = null;

  @JsonProperty("entityStatus")
  private ApiEntityStatus entityStatus = null;

  @JsonProperty("tags")
  @Valid
  private List<ApiEntityTag> tags = null;

  @JsonProperty("serviceVersion")
  private String serviceVersion = null;

  public ApiService name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the service.
   * @return name
  **/
  @ApiModelProperty(value = "The name of the service.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiService type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the service, e.g. HDFS, MAPREDUCE, HBASE.
   * @return type
  **/
  @ApiModelProperty(value = "The type of the service, e.g. HDFS, MAPREDUCE, HBASE.")


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ApiService clusterRef(ApiClusterRef clusterRef) {
    this.clusterRef = clusterRef;
    return this;
  }

  /**
   * Readonly. A reference to the enclosing cluster.
   * @return clusterRef
  **/
  @ApiModelProperty(value = "Readonly. A reference to the enclosing cluster.")

  @Valid

  public ApiClusterRef getClusterRef() {
    return clusterRef;
  }

  public void setClusterRef(ApiClusterRef clusterRef) {
    this.clusterRef = clusterRef;
  }

  public ApiService serviceState(ApiServiceState serviceState) {
    this.serviceState = serviceState;
    return this;
  }

  /**
   * Readonly. The configured run state of this service. Whether it's running, etc.
   * @return serviceState
  **/
  @ApiModelProperty(value = "Readonly. The configured run state of this service. Whether it's running, etc.")

  @Valid

  public ApiServiceState getServiceState() {
    return serviceState;
  }

  public void setServiceState(ApiServiceState serviceState) {
    this.serviceState = serviceState;
  }

  public ApiService healthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
    return this;
  }

  /**
   * Readonly. The high-level health status of this service.
   * @return healthSummary
  **/
  @ApiModelProperty(value = "Readonly. The high-level health status of this service.")

  @Valid

  public ApiHealthSummary getHealthSummary() {
    return healthSummary;
  }

  public void setHealthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
  }

  public ApiService configStale(Boolean configStale) {
    this.configStale = configStale;
    return this;
  }

  /**
   * Readonly. Expresses whether the service configuration is stale.
   * @return configStale
  **/
  @ApiModelProperty(value = "Readonly. Expresses whether the service configuration is stale.")


  public Boolean isConfigStale() {
    return configStale;
  }

  public void setConfigStale(Boolean configStale) {
    this.configStale = configStale;
  }

  public ApiService configStalenessStatus(ApiConfigStalenessStatus configStalenessStatus) {
    this.configStalenessStatus = configStalenessStatus;
    return this;
  }

  /**
   * Readonly. Expresses the service's configuration staleness status which is based on the staleness status of its roles. Available since API v6.
   * @return configStalenessStatus
  **/
  @ApiModelProperty(value = "Readonly. Expresses the service's configuration staleness status which is based on the staleness status of its roles. Available since API v6.")

  @Valid

  public ApiConfigStalenessStatus getConfigStalenessStatus() {
    return configStalenessStatus;
  }

  public void setConfigStalenessStatus(ApiConfigStalenessStatus configStalenessStatus) {
    this.configStalenessStatus = configStalenessStatus;
  }

  public ApiService clientConfigStalenessStatus(ApiConfigStalenessStatus clientConfigStalenessStatus) {
    this.clientConfigStalenessStatus = clientConfigStalenessStatus;
    return this;
  }

  /**
   * Readonly. Expresses the service's client configuration staleness status which is marked as stale if any of the service's hosts have missing client configurations or if any of the deployed client configurations are stale. Available since API v6.
   * @return clientConfigStalenessStatus
  **/
  @ApiModelProperty(value = "Readonly. Expresses the service's client configuration staleness status which is marked as stale if any of the service's hosts have missing client configurations or if any of the deployed client configurations are stale. Available since API v6.")

  @Valid

  public ApiConfigStalenessStatus getClientConfigStalenessStatus() {
    return clientConfigStalenessStatus;
  }

  public void setClientConfigStalenessStatus(ApiConfigStalenessStatus clientConfigStalenessStatus) {
    this.clientConfigStalenessStatus = clientConfigStalenessStatus;
  }

  public ApiService healthChecks(List<ApiHealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
    return this;
  }

  public ApiService addHealthChecksItem(ApiHealthCheck healthChecksItem) {
    if (this.healthChecks == null) {
      this.healthChecks = new ArrayList<>();
    }
    this.healthChecks.add(healthChecksItem);
    return this;
  }

  /**
   * Readonly. The list of health checks of this service.
   * @return healthChecks
  **/
  @ApiModelProperty(value = "Readonly. The list of health checks of this service.")

  @Valid

  public List<ApiHealthCheck> getHealthChecks() {
    return healthChecks;
  }

  public void setHealthChecks(List<ApiHealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
  }

  public ApiService serviceUrl(String serviceUrl) {
    this.serviceUrl = serviceUrl;
    return this;
  }

  /**
   * Readonly. Link into the Cloudera Manager web UI for this specific service.
   * @return serviceUrl
  **/
  @ApiModelProperty(value = "Readonly. Link into the Cloudera Manager web UI for this specific service.")


  public String getServiceUrl() {
    return serviceUrl;
  }

  public void setServiceUrl(String serviceUrl) {
    this.serviceUrl = serviceUrl;
  }

  public ApiService roleInstancesUrl(String roleInstancesUrl) {
    this.roleInstancesUrl = roleInstancesUrl;
    return this;
  }

  /**
   * Readonly. Link into the Cloudera Manager web UI for role instances table for this specific service. Available since API v11.
   * @return roleInstancesUrl
  **/
  @ApiModelProperty(value = "Readonly. Link into the Cloudera Manager web UI for role instances table for this specific service. Available since API v11.")


  public String getRoleInstancesUrl() {
    return roleInstancesUrl;
  }

  public void setRoleInstancesUrl(String roleInstancesUrl) {
    this.roleInstancesUrl = roleInstancesUrl;
  }

  public ApiService maintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
    return this;
  }

  /**
   * Readonly. Whether the service is in maintenance mode. Available since API v2.
   * @return maintenanceMode
  **/
  @ApiModelProperty(value = "Readonly. Whether the service is in maintenance mode. Available since API v2.")


  public Boolean isMaintenanceMode() {
    return maintenanceMode;
  }

  public void setMaintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  public ApiService maintenanceOwners(List<ApiEntityType> maintenanceOwners) {
    this.maintenanceOwners = maintenanceOwners;
    return this;
  }

  public ApiService addMaintenanceOwnersItem(ApiEntityType maintenanceOwnersItem) {
    if (this.maintenanceOwners == null) {
      this.maintenanceOwners = new ArrayList<>();
    }
    this.maintenanceOwners.add(maintenanceOwnersItem);
    return this;
  }

  /**
   * Readonly. The list of objects that trigger this service to be in maintenance mode. Available since API v2.
   * @return maintenanceOwners
  **/
  @ApiModelProperty(value = "Readonly. The list of objects that trigger this service to be in maintenance mode. Available since API v2.")

  @Valid

  public List<ApiEntityType> getMaintenanceOwners() {
    return maintenanceOwners;
  }

  public void setMaintenanceOwners(List<ApiEntityType> maintenanceOwners) {
    this.maintenanceOwners = maintenanceOwners;
  }

  public ApiService config(ApiServiceConfig config) {
    this.config = config;
    return this;
  }

  /**
   * Configuration of the service being created. Optional.
   * @return config
  **/
  @ApiModelProperty(value = "Configuration of the service being created. Optional.")

  @Valid

  public ApiServiceConfig getConfig() {
    return config;
  }

  public void setConfig(ApiServiceConfig config) {
    this.config = config;
  }

  public ApiService roles(List<ApiRole> roles) {
    this.roles = roles;
    return this;
  }

  public ApiService addRolesItem(ApiRole rolesItem) {
    if (this.roles == null) {
      this.roles = new ArrayList<>();
    }
    this.roles.add(rolesItem);
    return this;
  }

  /**
   * The list of service roles. Optional.
   * @return roles
  **/
  @ApiModelProperty(value = "The list of service roles. Optional.")

  @Valid

  public List<ApiRole> getRoles() {
    return roles;
  }

  public void setRoles(List<ApiRole> roles) {
    this.roles = roles;
  }

  public ApiService displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * The display name for the service that is shown in the UI. Available since API v2.
   * @return displayName
  **/
  @ApiModelProperty(value = "The display name for the service that is shown in the UI. Available since API v2.")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiService roleConfigGroups(List<ApiRoleConfigGroup> roleConfigGroups) {
    this.roleConfigGroups = roleConfigGroups;
    return this;
  }

  public ApiService addRoleConfigGroupsItem(ApiRoleConfigGroup roleConfigGroupsItem) {
    if (this.roleConfigGroups == null) {
      this.roleConfigGroups = new ArrayList<>();
    }
    this.roleConfigGroups.add(roleConfigGroupsItem);
    return this;
  }

  /**
   * The list of role configuration groups in this service. Optional. Available since API v3.
   * @return roleConfigGroups
  **/
  @ApiModelProperty(value = "The list of role configuration groups in this service. Optional. Available since API v3.")

  @Valid

  public List<ApiRoleConfigGroup> getRoleConfigGroups() {
    return roleConfigGroups;
  }

  public void setRoleConfigGroups(List<ApiRoleConfigGroup> roleConfigGroups) {
    this.roleConfigGroups = roleConfigGroups;
  }

  public ApiService replicationSchedules(List<ApiReplicationSchedule> replicationSchedules) {
    this.replicationSchedules = replicationSchedules;
    return this;
  }

  public ApiService addReplicationSchedulesItem(ApiReplicationSchedule replicationSchedulesItem) {
    if (this.replicationSchedules == null) {
      this.replicationSchedules = new ArrayList<>();
    }
    this.replicationSchedules.add(replicationSchedulesItem);
    return this;
  }

  /**
   * The list of replication schedules for this service. Optional. Available since API v6.
   * @return replicationSchedules
  **/
  @ApiModelProperty(value = "The list of replication schedules for this service. Optional. Available since API v6.")

  @Valid

  public List<ApiReplicationSchedule> getReplicationSchedules() {
    return replicationSchedules;
  }

  public void setReplicationSchedules(List<ApiReplicationSchedule> replicationSchedules) {
    this.replicationSchedules = replicationSchedules;
  }

  public ApiService snapshotPolicies(List<ApiSnapshotPolicy> snapshotPolicies) {
    this.snapshotPolicies = snapshotPolicies;
    return this;
  }

  public ApiService addSnapshotPoliciesItem(ApiSnapshotPolicy snapshotPoliciesItem) {
    if (this.snapshotPolicies == null) {
      this.snapshotPolicies = new ArrayList<>();
    }
    this.snapshotPolicies.add(snapshotPoliciesItem);
    return this;
  }

  /**
   * The list of snapshot policies for this service. Optional. Available since API v6.
   * @return snapshotPolicies
  **/
  @ApiModelProperty(value = "The list of snapshot policies for this service. Optional. Available since API v6.")

  @Valid

  public List<ApiSnapshotPolicy> getSnapshotPolicies() {
    return snapshotPolicies;
  }

  public void setSnapshotPolicies(List<ApiSnapshotPolicy> snapshotPolicies) {
    this.snapshotPolicies = snapshotPolicies;
  }

  public ApiService entityStatus(ApiEntityStatus entityStatus) {
    this.entityStatus = entityStatus;
    return this;
  }

  /**
   * Readonly. The entity status for this service. Available since API v11.
   * @return entityStatus
  **/
  @ApiModelProperty(value = "Readonly. The entity status for this service. Available since API v11.")

  @Valid

  public ApiEntityStatus getEntityStatus() {
    return entityStatus;
  }

  public void setEntityStatus(ApiEntityStatus entityStatus) {
    this.entityStatus = entityStatus;
  }

  public ApiService tags(List<ApiEntityTag> tags) {
    this.tags = tags;
    return this;
  }

  public ApiService addTagsItem(ApiEntityTag tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Tags associated with the service. Available since V41.
   * @return tags
  **/
  @ApiModelProperty(value = "Tags associated with the service. Available since V41.")

  @Valid

  public List<ApiEntityTag> getTags() {
    return tags;
  }

  public void setTags(List<ApiEntityTag> tags) {
    this.tags = tags;
  }

  public ApiService serviceVersion(String serviceVersion) {
    this.serviceVersion = serviceVersion;
    return this;
  }

  /**
   * Service version (optional) Available since V41.
   * @return serviceVersion
  **/
  @ApiModelProperty(value = "Service version (optional) Available since V41.")


  public String getServiceVersion() {
    return serviceVersion;
  }

  public void setServiceVersion(String serviceVersion) {
    this.serviceVersion = serviceVersion;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiService apiService = (ApiService) o;
    return Objects.equals(this.name, apiService.name) &&
        Objects.equals(this.type, apiService.type) &&
        Objects.equals(this.clusterRef, apiService.clusterRef) &&
        Objects.equals(this.serviceState, apiService.serviceState) &&
        Objects.equals(this.healthSummary, apiService.healthSummary) &&
        Objects.equals(this.configStale, apiService.configStale) &&
        Objects.equals(this.configStalenessStatus, apiService.configStalenessStatus) &&
        Objects.equals(this.clientConfigStalenessStatus, apiService.clientConfigStalenessStatus) &&
        Objects.equals(this.healthChecks, apiService.healthChecks) &&
        Objects.equals(this.serviceUrl, apiService.serviceUrl) &&
        Objects.equals(this.roleInstancesUrl, apiService.roleInstancesUrl) &&
        Objects.equals(this.maintenanceMode, apiService.maintenanceMode) &&
        Objects.equals(this.maintenanceOwners, apiService.maintenanceOwners) &&
        Objects.equals(this.config, apiService.config) &&
        Objects.equals(this.roles, apiService.roles) &&
        Objects.equals(this.displayName, apiService.displayName) &&
        Objects.equals(this.roleConfigGroups, apiService.roleConfigGroups) &&
        Objects.equals(this.replicationSchedules, apiService.replicationSchedules) &&
        Objects.equals(this.snapshotPolicies, apiService.snapshotPolicies) &&
        Objects.equals(this.entityStatus, apiService.entityStatus) &&
        Objects.equals(this.tags, apiService.tags) &&
        Objects.equals(this.serviceVersion, apiService.serviceVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, clusterRef, serviceState, healthSummary, configStale, configStalenessStatus, clientConfigStalenessStatus, healthChecks, serviceUrl, roleInstancesUrl, maintenanceMode, maintenanceOwners, config, roles, displayName, roleConfigGroups, replicationSchedules, snapshotPolicies, entityStatus, tags, serviceVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiService {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    clusterRef: ").append(toIndentedString(clusterRef)).append("\n");
    sb.append("    serviceState: ").append(toIndentedString(serviceState)).append("\n");
    sb.append("    healthSummary: ").append(toIndentedString(healthSummary)).append("\n");
    sb.append("    configStale: ").append(toIndentedString(configStale)).append("\n");
    sb.append("    configStalenessStatus: ").append(toIndentedString(configStalenessStatus)).append("\n");
    sb.append("    clientConfigStalenessStatus: ").append(toIndentedString(clientConfigStalenessStatus)).append("\n");
    sb.append("    healthChecks: ").append(toIndentedString(healthChecks)).append("\n");
    sb.append("    serviceUrl: ").append(toIndentedString(serviceUrl)).append("\n");
    sb.append("    roleInstancesUrl: ").append(toIndentedString(roleInstancesUrl)).append("\n");
    sb.append("    maintenanceMode: ").append(toIndentedString(maintenanceMode)).append("\n");
    sb.append("    maintenanceOwners: ").append(toIndentedString(maintenanceOwners)).append("\n");
    sb.append("    config: ").append(toIndentedString(config)).append("\n");
    sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    roleConfigGroups: ").append(toIndentedString(roleConfigGroups)).append("\n");
    sb.append("    replicationSchedules: ").append(toIndentedString(replicationSchedules)).append("\n");
    sb.append("    snapshotPolicies: ").append(toIndentedString(snapshotPolicies)).append("\n");
    sb.append("    entityStatus: ").append(toIndentedString(entityStatus)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    serviceVersion: ").append(toIndentedString(serviceVersion)).append("\n");
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

