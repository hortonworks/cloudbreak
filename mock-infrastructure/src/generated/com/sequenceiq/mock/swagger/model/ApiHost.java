package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiClusterRef;
import com.sequenceiq.mock.swagger.model.ApiCommissionState;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiEntityStatus;
import com.sequenceiq.mock.swagger.model.ApiEntityTag;
import com.sequenceiq.mock.swagger.model.ApiEntityType;
import com.sequenceiq.mock.swagger.model.ApiHealthCheck;
import com.sequenceiq.mock.swagger.model.ApiHealthSummary;
import com.sequenceiq.mock.swagger.model.ApiOsDistribution;
import com.sequenceiq.mock.swagger.model.ApiRoleRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This is the model for a host in the system.
 */
@ApiModel(description = "This is the model for a host in the system.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHost   {
  @JsonProperty("hostId")
  private String hostId = null;

  @JsonProperty("ipAddress")
  private String ipAddress = null;

  @JsonProperty("hostname")
  private String hostname = null;

  @JsonProperty("rackId")
  private String rackId = null;

  @JsonProperty("lastHeartbeat")
  private String lastHeartbeat = null;

  @JsonProperty("roleRefs")
  @Valid
  private List<ApiRoleRef> roleRefs = null;

  @JsonProperty("healthSummary")
  private ApiHealthSummary healthSummary = null;

  @JsonProperty("healthChecks")
  @Valid
  private List<ApiHealthCheck> healthChecks = null;

  @JsonProperty("hostUrl")
  private String hostUrl = null;

  @JsonProperty("maintenanceMode")
  private Boolean maintenanceMode = null;

  @JsonProperty("commissionState")
  private ApiCommissionState commissionState = null;

  @JsonProperty("maintenanceOwners")
  @Valid
  private List<ApiEntityType> maintenanceOwners = null;

  @JsonProperty("config")
  private ApiConfigList config = null;

  @JsonProperty("numCores")
  private Integer numCores = null;

  @JsonProperty("numPhysicalCores")
  private Integer numPhysicalCores = null;

  @JsonProperty("totalPhysMemBytes")
  private BigDecimal totalPhysMemBytes = null;

  @JsonProperty("entityStatus")
  private ApiEntityStatus entityStatus = null;

  @JsonProperty("clusterRef")
  private ApiClusterRef clusterRef = null;

  @JsonProperty("distribution")
  private ApiOsDistribution distribution = null;

  @JsonProperty("tags")
  @Valid
  private List<ApiEntityTag> tags = null;

  public ApiHost hostId(String hostId) {
    this.hostId = hostId;
    return this;
  }

  /**
   * A unique host identifier. This is not the same as the hostname (FQDN). It is a distinct value that remains the same even if the hostname changes.
   * @return hostId
  **/
  @ApiModelProperty(value = "A unique host identifier. This is not the same as the hostname (FQDN). It is a distinct value that remains the same even if the hostname changes.")


  public String getHostId() {
    return hostId;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public ApiHost ipAddress(String ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }

  /**
   * The host IP address. This field is not mutable after the initial creation.
   * @return ipAddress
  **/
  @ApiModelProperty(value = "The host IP address. This field is not mutable after the initial creation.")


  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public ApiHost hostname(String hostname) {
    this.hostname = hostname;
    return this;
  }

  /**
   * The hostname. This field is not mutable after the initial creation.
   * @return hostname
  **/
  @ApiModelProperty(value = "The hostname. This field is not mutable after the initial creation.")


  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public ApiHost rackId(String rackId) {
    this.rackId = rackId;
    return this;
  }

  /**
   * The rack ID for this host.
   * @return rackId
  **/
  @ApiModelProperty(value = "The rack ID for this host.")


  public String getRackId() {
    return rackId;
  }

  public void setRackId(String rackId) {
    this.rackId = rackId;
  }

  public ApiHost lastHeartbeat(String lastHeartbeat) {
    this.lastHeartbeat = lastHeartbeat;
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. When the host agent sent the last heartbeat.
   * @return lastHeartbeat
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. When the host agent sent the last heartbeat.")


  public String getLastHeartbeat() {
    return lastHeartbeat;
  }

  public void setLastHeartbeat(String lastHeartbeat) {
    this.lastHeartbeat = lastHeartbeat;
  }

  public ApiHost roleRefs(List<ApiRoleRef> roleRefs) {
    this.roleRefs = roleRefs;
    return this;
  }

  public ApiHost addRoleRefsItem(ApiRoleRef roleRefsItem) {
    if (this.roleRefs == null) {
      this.roleRefs = new ArrayList<>();
    }
    this.roleRefs.add(roleRefsItem);
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. The list of roles assigned to this host.
   * @return roleRefs
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. The list of roles assigned to this host.")

  @Valid

  public List<ApiRoleRef> getRoleRefs() {
    return roleRefs;
  }

  public void setRoleRefs(List<ApiRoleRef> roleRefs) {
    this.roleRefs = roleRefs;
  }

  public ApiHost healthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. The high-level health status of this host.
   * @return healthSummary
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. The high-level health status of this host.")

  @Valid

  public ApiHealthSummary getHealthSummary() {
    return healthSummary;
  }

  public void setHealthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
  }

  public ApiHost healthChecks(List<ApiHealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
    return this;
  }

  public ApiHost addHealthChecksItem(ApiHealthCheck healthChecksItem) {
    if (this.healthChecks == null) {
      this.healthChecks = new ArrayList<>();
    }
    this.healthChecks.add(healthChecksItem);
    return this;
  }

  /**
   * Readonly. Requires \"full\" view. The list of health checks performed on the host, with their results.
   * @return healthChecks
  **/
  @ApiModelProperty(value = "Readonly. Requires \"full\" view. The list of health checks performed on the host, with their results.")

  @Valid

  public List<ApiHealthCheck> getHealthChecks() {
    return healthChecks;
  }

  public void setHealthChecks(List<ApiHealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
  }

  public ApiHost hostUrl(String hostUrl) {
    this.hostUrl = hostUrl;
    return this;
  }

  /**
   * Readonly. A URL into the Cloudera Manager web UI for this specific host.
   * @return hostUrl
  **/
  @ApiModelProperty(value = "Readonly. A URL into the Cloudera Manager web UI for this specific host.")


  public String getHostUrl() {
    return hostUrl;
  }

  public void setHostUrl(String hostUrl) {
    this.hostUrl = hostUrl;
  }

  public ApiHost maintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
    return this;
  }

  /**
   * Readonly. Whether the host is in maintenance mode. Available since API v2.
   * @return maintenanceMode
  **/
  @ApiModelProperty(value = "Readonly. Whether the host is in maintenance mode. Available since API v2.")


  public Boolean isMaintenanceMode() {
    return maintenanceMode;
  }

  public void setMaintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  public ApiHost commissionState(ApiCommissionState commissionState) {
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

  public ApiHost maintenanceOwners(List<ApiEntityType> maintenanceOwners) {
    this.maintenanceOwners = maintenanceOwners;
    return this;
  }

  public ApiHost addMaintenanceOwnersItem(ApiEntityType maintenanceOwnersItem) {
    if (this.maintenanceOwners == null) {
      this.maintenanceOwners = new ArrayList<>();
    }
    this.maintenanceOwners.add(maintenanceOwnersItem);
    return this;
  }

  /**
   * Readonly. The list of objects that trigger this host to be in maintenance mode. Available since API v2.
   * @return maintenanceOwners
  **/
  @ApiModelProperty(value = "Readonly. The list of objects that trigger this host to be in maintenance mode. Available since API v2.")

  @Valid

  public List<ApiEntityType> getMaintenanceOwners() {
    return maintenanceOwners;
  }

  public void setMaintenanceOwners(List<ApiEntityType> maintenanceOwners) {
    this.maintenanceOwners = maintenanceOwners;
  }

  public ApiHost config(ApiConfigList config) {
    this.config = config;
    return this;
  }

  /**
   * 
   * @return config
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiConfigList getConfig() {
    return config;
  }

  public void setConfig(ApiConfigList config) {
    this.config = config;
  }

  public ApiHost numCores(Integer numCores) {
    this.numCores = numCores;
    return this;
  }

  /**
   * Readonly. The number of logical CPU cores on this host. Only populated after the host has heartbeated to the server. Available since API v4.
   * @return numCores
  **/
  @ApiModelProperty(value = "Readonly. The number of logical CPU cores on this host. Only populated after the host has heartbeated to the server. Available since API v4.")


  public Integer getNumCores() {
    return numCores;
  }

  public void setNumCores(Integer numCores) {
    this.numCores = numCores;
  }

  public ApiHost numPhysicalCores(Integer numPhysicalCores) {
    this.numPhysicalCores = numPhysicalCores;
    return this;
  }

  /**
   * Readonly. The number of physical CPU cores on this host. Only populated after the host has heartbeated to the server. Available since API v9.
   * @return numPhysicalCores
  **/
  @ApiModelProperty(value = "Readonly. The number of physical CPU cores on this host. Only populated after the host has heartbeated to the server. Available since API v9.")


  public Integer getNumPhysicalCores() {
    return numPhysicalCores;
  }

  public void setNumPhysicalCores(Integer numPhysicalCores) {
    this.numPhysicalCores = numPhysicalCores;
  }

  public ApiHost totalPhysMemBytes(BigDecimal totalPhysMemBytes) {
    this.totalPhysMemBytes = totalPhysMemBytes;
    return this;
  }

  /**
   * Readonly. The amount of physical RAM on this host, in bytes. Only populated after the host has heartbeated to the server. Available since API v4.
   * @return totalPhysMemBytes
  **/
  @ApiModelProperty(value = "Readonly. The amount of physical RAM on this host, in bytes. Only populated after the host has heartbeated to the server. Available since API v4.")

  @Valid

  public BigDecimal getTotalPhysMemBytes() {
    return totalPhysMemBytes;
  }

  public void setTotalPhysMemBytes(BigDecimal totalPhysMemBytes) {
    this.totalPhysMemBytes = totalPhysMemBytes;
  }

  public ApiHost entityStatus(ApiEntityStatus entityStatus) {
    this.entityStatus = entityStatus;
    return this;
  }

  /**
   * Readonly. The entity status for this host. Available since API v11.
   * @return entityStatus
  **/
  @ApiModelProperty(value = "Readonly. The entity status for this host. Available since API v11.")

  @Valid

  public ApiEntityStatus getEntityStatus() {
    return entityStatus;
  }

  public void setEntityStatus(ApiEntityStatus entityStatus) {
    this.entityStatus = entityStatus;
  }

  public ApiHost clusterRef(ApiClusterRef clusterRef) {
    this.clusterRef = clusterRef;
    return this;
  }

  /**
   * Readonly. A reference to the enclosing cluster. This might be null if the host is not yet assigned to a cluster. Available since API v11.
   * @return clusterRef
  **/
  @ApiModelProperty(value = "Readonly. A reference to the enclosing cluster. This might be null if the host is not yet assigned to a cluster. Available since API v11.")

  @Valid

  public ApiClusterRef getClusterRef() {
    return clusterRef;
  }

  public void setClusterRef(ApiClusterRef clusterRef) {
    this.clusterRef = clusterRef;
  }

  public ApiHost distribution(ApiOsDistribution distribution) {
    this.distribution = distribution;
    return this;
  }

  /**
   * Readonly. OS distribution available on the host. Available since API v40.
   * @return distribution
  **/
  @ApiModelProperty(value = "Readonly. OS distribution available on the host. Available since API v40.")

  @Valid

  public ApiOsDistribution getDistribution() {
    return distribution;
  }

  public void setDistribution(ApiOsDistribution distribution) {
    this.distribution = distribution;
  }

  public ApiHost tags(List<ApiEntityTag> tags) {
    this.tags = tags;
    return this;
  }

  public ApiHost addTagsItem(ApiEntityTag tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Tags associated with the host. Available since V41.
   * @return tags
  **/
  @ApiModelProperty(value = "Tags associated with the host. Available since V41.")

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
    ApiHost apiHost = (ApiHost) o;
    return Objects.equals(this.hostId, apiHost.hostId) &&
        Objects.equals(this.ipAddress, apiHost.ipAddress) &&
        Objects.equals(this.hostname, apiHost.hostname) &&
        Objects.equals(this.rackId, apiHost.rackId) &&
        Objects.equals(this.lastHeartbeat, apiHost.lastHeartbeat) &&
        Objects.equals(this.roleRefs, apiHost.roleRefs) &&
        Objects.equals(this.healthSummary, apiHost.healthSummary) &&
        Objects.equals(this.healthChecks, apiHost.healthChecks) &&
        Objects.equals(this.hostUrl, apiHost.hostUrl) &&
        Objects.equals(this.maintenanceMode, apiHost.maintenanceMode) &&
        Objects.equals(this.commissionState, apiHost.commissionState) &&
        Objects.equals(this.maintenanceOwners, apiHost.maintenanceOwners) &&
        Objects.equals(this.config, apiHost.config) &&
        Objects.equals(this.numCores, apiHost.numCores) &&
        Objects.equals(this.numPhysicalCores, apiHost.numPhysicalCores) &&
        Objects.equals(this.totalPhysMemBytes, apiHost.totalPhysMemBytes) &&
        Objects.equals(this.entityStatus, apiHost.entityStatus) &&
        Objects.equals(this.clusterRef, apiHost.clusterRef) &&
        Objects.equals(this.distribution, apiHost.distribution) &&
        Objects.equals(this.tags, apiHost.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostId, ipAddress, hostname, rackId, lastHeartbeat, roleRefs, healthSummary, healthChecks, hostUrl, maintenanceMode, commissionState, maintenanceOwners, config, numCores, numPhysicalCores, totalPhysMemBytes, entityStatus, clusterRef, distribution, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHost {\n");
    
    sb.append("    hostId: ").append(toIndentedString(hostId)).append("\n");
    sb.append("    ipAddress: ").append(toIndentedString(ipAddress)).append("\n");
    sb.append("    hostname: ").append(toIndentedString(hostname)).append("\n");
    sb.append("    rackId: ").append(toIndentedString(rackId)).append("\n");
    sb.append("    lastHeartbeat: ").append(toIndentedString(lastHeartbeat)).append("\n");
    sb.append("    roleRefs: ").append(toIndentedString(roleRefs)).append("\n");
    sb.append("    healthSummary: ").append(toIndentedString(healthSummary)).append("\n");
    sb.append("    healthChecks: ").append(toIndentedString(healthChecks)).append("\n");
    sb.append("    hostUrl: ").append(toIndentedString(hostUrl)).append("\n");
    sb.append("    maintenanceMode: ").append(toIndentedString(maintenanceMode)).append("\n");
    sb.append("    commissionState: ").append(toIndentedString(commissionState)).append("\n");
    sb.append("    maintenanceOwners: ").append(toIndentedString(maintenanceOwners)).append("\n");
    sb.append("    config: ").append(toIndentedString(config)).append("\n");
    sb.append("    numCores: ").append(toIndentedString(numCores)).append("\n");
    sb.append("    numPhysicalCores: ").append(toIndentedString(numPhysicalCores)).append("\n");
    sb.append("    totalPhysMemBytes: ").append(toIndentedString(totalPhysMemBytes)).append("\n");
    sb.append("    entityStatus: ").append(toIndentedString(entityStatus)).append("\n");
    sb.append("    clusterRef: ").append(toIndentedString(clusterRef)).append("\n");
    sb.append("    distribution: ").append(toIndentedString(distribution)).append("\n");
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

