package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiCommissionState;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiConfigStalenessStatus;
import com.sequenceiq.mock.swagger.model.ApiEntityStatus;
import com.sequenceiq.mock.swagger.model.ApiEntityTag;
import com.sequenceiq.mock.swagger.model.ApiEntityType;
import com.sequenceiq.mock.swagger.model.ApiHealthCheck;
import com.sequenceiq.mock.swagger.model.ApiHealthSummary;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiRoleConfigGroupRef;
import com.sequenceiq.mock.swagger.model.ApiRoleState;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import com.sequenceiq.mock.swagger.model.HaStatus;
import com.sequenceiq.mock.swagger.model.ZooKeeperServerMode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A role represents a specific entity that participate in a service. Examples are JobTrackers, DataNodes, HBase Masters. Each role is assigned a host where it runs on.
 */
@ApiModel(description = "A role represents a specific entity that participate in a service. Examples are JobTrackers, DataNodes, HBase Masters. Each role is assigned a host where it runs on.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiRole   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("hostRef")
  private ApiHostRef hostRef = null;

  @JsonProperty("serviceRef")
  private ApiServiceRef serviceRef = null;

  @JsonProperty("roleState")
  private ApiRoleState roleState = null;

  @JsonProperty("commissionState")
  private ApiCommissionState commissionState = null;

  @JsonProperty("healthSummary")
  private ApiHealthSummary healthSummary = null;

  @JsonProperty("configStale")
  private Boolean configStale = null;

  @JsonProperty("configStalenessStatus")
  private ApiConfigStalenessStatus configStalenessStatus = null;

  @JsonProperty("healthChecks")
  @Valid
  private List<ApiHealthCheck> healthChecks = null;

  @JsonProperty("haStatus")
  private HaStatus haStatus = null;

  @JsonProperty("roleUrl")
  private String roleUrl = null;

  @JsonProperty("maintenanceMode")
  private Boolean maintenanceMode = null;

  @JsonProperty("maintenanceOwners")
  @Valid
  private List<ApiEntityType> maintenanceOwners = null;

  @JsonProperty("config")
  private ApiConfigList config = null;

  @JsonProperty("roleConfigGroupRef")
  private ApiRoleConfigGroupRef roleConfigGroupRef = null;

  @JsonProperty("zooKeeperServerMode")
  private ZooKeeperServerMode zooKeeperServerMode = null;

  @JsonProperty("entityStatus")
  private ApiEntityStatus entityStatus = null;

  @JsonProperty("tags")
  @Valid
  private List<ApiEntityTag> tags = null;

  public ApiRole name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the role. Optional when creating a role since API v6. If not specified, a name will be automatically generated for the role.
   * @return name
  **/
  @ApiModelProperty(value = "The name of the role. Optional when creating a role since API v6. If not specified, a name will be automatically generated for the role.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiRole type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the role, e.g. NAMENODE, DATANODE, TASKTRACKER.
   * @return type
  **/
  @ApiModelProperty(value = "The type of the role, e.g. NAMENODE, DATANODE, TASKTRACKER.")


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ApiRole hostRef(ApiHostRef hostRef) {
    this.hostRef = hostRef;
    return this;
  }

  /**
   * A reference to the host where this role runs.
   * @return hostRef
  **/
  @ApiModelProperty(value = "A reference to the host where this role runs.")

  @Valid

  public ApiHostRef getHostRef() {
    return hostRef;
  }

  public void setHostRef(ApiHostRef hostRef) {
    this.hostRef = hostRef;
  }

  public ApiRole serviceRef(ApiServiceRef serviceRef) {
    this.serviceRef = serviceRef;
    return this;
  }

  /**
   * Readonly. A reference to the parent service.
   * @return serviceRef
  **/
  @ApiModelProperty(value = "Readonly. A reference to the parent service.")

  @Valid

  public ApiServiceRef getServiceRef() {
    return serviceRef;
  }

  public void setServiceRef(ApiServiceRef serviceRef) {
    this.serviceRef = serviceRef;
  }

  public ApiRole roleState(ApiRoleState roleState) {
    this.roleState = roleState;
    return this;
  }

  /**
   * Readonly. The configured run state of this role. Whether it's running, etc.
   * @return roleState
  **/
  @ApiModelProperty(value = "Readonly. The configured run state of this role. Whether it's running, etc.")

  @Valid

  public ApiRoleState getRoleState() {
    return roleState;
  }

  public void setRoleState(ApiRoleState roleState) {
    this.roleState = roleState;
  }

  public ApiRole commissionState(ApiCommissionState commissionState) {
    this.commissionState = commissionState;
    return this;
  }

  /**
   * Readonly. The commission state of this role. Available since API v2.
   * @return commissionState
  **/
  @ApiModelProperty(value = "Readonly. The commission state of this role. Available since API v2.")

  @Valid

  public ApiCommissionState getCommissionState() {
    return commissionState;
  }

  public void setCommissionState(ApiCommissionState commissionState) {
    this.commissionState = commissionState;
  }

  public ApiRole healthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
    return this;
  }

  /**
   * Readonly. The high-level health status of this role.
   * @return healthSummary
  **/
  @ApiModelProperty(value = "Readonly. The high-level health status of this role.")

  @Valid

  public ApiHealthSummary getHealthSummary() {
    return healthSummary;
  }

  public void setHealthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
  }

  public ApiRole configStale(Boolean configStale) {
    this.configStale = configStale;
    return this;
  }

  /**
   * Readonly. Expresses whether the role configuration is stale.
   * @return configStale
  **/
  @ApiModelProperty(value = "Readonly. Expresses whether the role configuration is stale.")


  public Boolean isConfigStale() {
    return configStale;
  }

  public void setConfigStale(Boolean configStale) {
    this.configStale = configStale;
  }

  public ApiRole configStalenessStatus(ApiConfigStalenessStatus configStalenessStatus) {
    this.configStalenessStatus = configStalenessStatus;
    return this;
  }

  /**
   * Readonly. Expresses the role's configuration staleness status. Available since API v6.
   * @return configStalenessStatus
  **/
  @ApiModelProperty(value = "Readonly. Expresses the role's configuration staleness status. Available since API v6.")

  @Valid

  public ApiConfigStalenessStatus getConfigStalenessStatus() {
    return configStalenessStatus;
  }

  public void setConfigStalenessStatus(ApiConfigStalenessStatus configStalenessStatus) {
    this.configStalenessStatus = configStalenessStatus;
  }

  public ApiRole healthChecks(List<ApiHealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
    return this;
  }

  public ApiRole addHealthChecksItem(ApiHealthCheck healthChecksItem) {
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

  public ApiRole haStatus(HaStatus haStatus) {
    this.haStatus = haStatus;
    return this;
  }

  /**
   * Readonly. The HA status of this role.
   * @return haStatus
  **/
  @ApiModelProperty(value = "Readonly. The HA status of this role.")

  @Valid

  public HaStatus getHaStatus() {
    return haStatus;
  }

  public void setHaStatus(HaStatus haStatus) {
    this.haStatus = haStatus;
  }

  public ApiRole roleUrl(String roleUrl) {
    this.roleUrl = roleUrl;
    return this;
  }

  /**
   * Readonly. Link into the Cloudera Manager web UI for this specific role.
   * @return roleUrl
  **/
  @ApiModelProperty(value = "Readonly. Link into the Cloudera Manager web UI for this specific role.")


  public String getRoleUrl() {
    return roleUrl;
  }

  public void setRoleUrl(String roleUrl) {
    this.roleUrl = roleUrl;
  }

  public ApiRole maintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
    return this;
  }

  /**
   * Readonly. Whether the role is in maintenance mode. Available since API v2.
   * @return maintenanceMode
  **/
  @ApiModelProperty(value = "Readonly. Whether the role is in maintenance mode. Available since API v2.")


  public Boolean isMaintenanceMode() {
    return maintenanceMode;
  }

  public void setMaintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  public ApiRole maintenanceOwners(List<ApiEntityType> maintenanceOwners) {
    this.maintenanceOwners = maintenanceOwners;
    return this;
  }

  public ApiRole addMaintenanceOwnersItem(ApiEntityType maintenanceOwnersItem) {
    if (this.maintenanceOwners == null) {
      this.maintenanceOwners = new ArrayList<>();
    }
    this.maintenanceOwners.add(maintenanceOwnersItem);
    return this;
  }

  /**
   * Readonly. The list of objects that trigger this role to be in maintenance mode. Available since API v2.
   * @return maintenanceOwners
  **/
  @ApiModelProperty(value = "Readonly. The list of objects that trigger this role to be in maintenance mode. Available since API v2.")

  @Valid

  public List<ApiEntityType> getMaintenanceOwners() {
    return maintenanceOwners;
  }

  public void setMaintenanceOwners(List<ApiEntityType> maintenanceOwners) {
    this.maintenanceOwners = maintenanceOwners;
  }

  public ApiRole config(ApiConfigList config) {
    this.config = config;
    return this;
  }

  /**
   * The role configuration. Optional.
   * @return config
  **/
  @ApiModelProperty(value = "The role configuration. Optional.")

  @Valid

  public ApiConfigList getConfig() {
    return config;
  }

  public void setConfig(ApiConfigList config) {
    this.config = config;
  }

  public ApiRole roleConfigGroupRef(ApiRoleConfigGroupRef roleConfigGroupRef) {
    this.roleConfigGroupRef = roleConfigGroupRef;
    return this;
  }

  /**
   * Readonly. The reference to the role configuration group of this role. Available since API v3.
   * @return roleConfigGroupRef
  **/
  @ApiModelProperty(value = "Readonly. The reference to the role configuration group of this role. Available since API v3.")

  @Valid

  public ApiRoleConfigGroupRef getRoleConfigGroupRef() {
    return roleConfigGroupRef;
  }

  public void setRoleConfigGroupRef(ApiRoleConfigGroupRef roleConfigGroupRef) {
    this.roleConfigGroupRef = roleConfigGroupRef;
  }

  public ApiRole zooKeeperServerMode(ZooKeeperServerMode zooKeeperServerMode) {
    this.zooKeeperServerMode = zooKeeperServerMode;
    return this;
  }

  /**
   * Readonly. The ZooKeeper server mode for this role. Note that for non-ZooKeeper Server roles this will be null. Available since API v6.
   * @return zooKeeperServerMode
  **/
  @ApiModelProperty(value = "Readonly. The ZooKeeper server mode for this role. Note that for non-ZooKeeper Server roles this will be null. Available since API v6.")

  @Valid

  public ZooKeeperServerMode getZooKeeperServerMode() {
    return zooKeeperServerMode;
  }

  public void setZooKeeperServerMode(ZooKeeperServerMode zooKeeperServerMode) {
    this.zooKeeperServerMode = zooKeeperServerMode;
  }

  public ApiRole entityStatus(ApiEntityStatus entityStatus) {
    this.entityStatus = entityStatus;
    return this;
  }

  /**
   * Readonly. The entity status for this role. Available since API v11.
   * @return entityStatus
  **/
  @ApiModelProperty(value = "Readonly. The entity status for this role. Available since API v11.")

  @Valid

  public ApiEntityStatus getEntityStatus() {
    return entityStatus;
  }

  public void setEntityStatus(ApiEntityStatus entityStatus) {
    this.entityStatus = entityStatus;
  }

  public ApiRole tags(List<ApiEntityTag> tags) {
    this.tags = tags;
    return this;
  }

  public ApiRole addTagsItem(ApiEntityTag tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Tags associated with the role. Available since V41.
   * @return tags
  **/
  @ApiModelProperty(value = "Tags associated with the role. Available since V41.")

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
    ApiRole apiRole = (ApiRole) o;
    return Objects.equals(this.name, apiRole.name) &&
        Objects.equals(this.type, apiRole.type) &&
        Objects.equals(this.hostRef, apiRole.hostRef) &&
        Objects.equals(this.serviceRef, apiRole.serviceRef) &&
        Objects.equals(this.roleState, apiRole.roleState) &&
        Objects.equals(this.commissionState, apiRole.commissionState) &&
        Objects.equals(this.healthSummary, apiRole.healthSummary) &&
        Objects.equals(this.configStale, apiRole.configStale) &&
        Objects.equals(this.configStalenessStatus, apiRole.configStalenessStatus) &&
        Objects.equals(this.healthChecks, apiRole.healthChecks) &&
        Objects.equals(this.haStatus, apiRole.haStatus) &&
        Objects.equals(this.roleUrl, apiRole.roleUrl) &&
        Objects.equals(this.maintenanceMode, apiRole.maintenanceMode) &&
        Objects.equals(this.maintenanceOwners, apiRole.maintenanceOwners) &&
        Objects.equals(this.config, apiRole.config) &&
        Objects.equals(this.roleConfigGroupRef, apiRole.roleConfigGroupRef) &&
        Objects.equals(this.zooKeeperServerMode, apiRole.zooKeeperServerMode) &&
        Objects.equals(this.entityStatus, apiRole.entityStatus) &&
        Objects.equals(this.tags, apiRole.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, hostRef, serviceRef, roleState, commissionState, healthSummary, configStale, configStalenessStatus, healthChecks, haStatus, roleUrl, maintenanceMode, maintenanceOwners, config, roleConfigGroupRef, zooKeeperServerMode, entityStatus, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiRole {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    hostRef: ").append(toIndentedString(hostRef)).append("\n");
    sb.append("    serviceRef: ").append(toIndentedString(serviceRef)).append("\n");
    sb.append("    roleState: ").append(toIndentedString(roleState)).append("\n");
    sb.append("    commissionState: ").append(toIndentedString(commissionState)).append("\n");
    sb.append("    healthSummary: ").append(toIndentedString(healthSummary)).append("\n");
    sb.append("    configStale: ").append(toIndentedString(configStale)).append("\n");
    sb.append("    configStalenessStatus: ").append(toIndentedString(configStalenessStatus)).append("\n");
    sb.append("    healthChecks: ").append(toIndentedString(healthChecks)).append("\n");
    sb.append("    haStatus: ").append(toIndentedString(haStatus)).append("\n");
    sb.append("    roleUrl: ").append(toIndentedString(roleUrl)).append("\n");
    sb.append("    maintenanceMode: ").append(toIndentedString(maintenanceMode)).append("\n");
    sb.append("    maintenanceOwners: ").append(toIndentedString(maintenanceOwners)).append("\n");
    sb.append("    config: ").append(toIndentedString(config)).append("\n");
    sb.append("    roleConfigGroupRef: ").append(toIndentedString(roleConfigGroupRef)).append("\n");
    sb.append("    zooKeeperServerMode: ").append(toIndentedString(zooKeeperServerMode)).append("\n");
    sb.append("    entityStatus: ").append(toIndentedString(entityStatus)).append("\n");
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

