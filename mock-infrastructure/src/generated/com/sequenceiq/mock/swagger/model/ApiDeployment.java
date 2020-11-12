package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiCluster;
import com.sequenceiq.mock.swagger.model.ApiCmPeer;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiHost;
import com.sequenceiq.mock.swagger.model.ApiHostTemplateList;
import com.sequenceiq.mock.swagger.model.ApiService;
import com.sequenceiq.mock.swagger.model.ApiUser;
import com.sequenceiq.mock.swagger.model.ApiVersionInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This objects represents a deployment including all clusters, hosts, services, roles, etc in the system.  It can be used to save and restore all settings.
 */
@ApiModel(description = "This objects represents a deployment including all clusters, hosts, services, roles, etc in the system.  It can be used to save and restore all settings.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-10-26T08:01:08.932+01:00")




public class ApiDeployment   {
  @JsonProperty("timestamp")
  private String timestamp = null;

  @JsonProperty("clusters")
  @Valid
  private List<ApiCluster> clusters = null;

  @JsonProperty("hosts")
  @Valid
  private List<ApiHost> hosts = null;

  @JsonProperty("users")
  @Valid
  private List<ApiUser> users = null;

  @JsonProperty("versionInfo")
  private ApiVersionInfo versionInfo = null;

  @JsonProperty("managementService")
  private ApiService managementService = null;

  @JsonProperty("managerSettings")
  private ApiConfigList managerSettings = null;

  @JsonProperty("allHostsConfig")
  private ApiConfigList allHostsConfig = null;

  @JsonProperty("peers")
  @Valid
  private List<ApiCmPeer> peers = null;

  @JsonProperty("hostTemplates")
  private ApiHostTemplateList hostTemplates = null;

  public ApiDeployment timestamp(String timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * Readonly. This timestamp is provided when you request a deployment and is not required (or even read) when creating a deployment. This timestamp is useful if you have multiple deployments saved and want to determine which one to use as a restore point.
   * @return timestamp
  **/
  @ApiModelProperty(value = "Readonly. This timestamp is provided when you request a deployment and is not required (or even read) when creating a deployment. This timestamp is useful if you have multiple deployments saved and want to determine which one to use as a restore point.")


  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public ApiDeployment clusters(List<ApiCluster> clusters) {
    this.clusters = clusters;
    return this;
  }

  public ApiDeployment addClustersItem(ApiCluster clustersItem) {
    if (this.clusters == null) {
      this.clusters = new ArrayList<>();
    }
    this.clusters.add(clustersItem);
    return this;
  }

  /**
   * List of clusters in the system including their services, roles and complete config values.
   * @return clusters
  **/
  @ApiModelProperty(value = "List of clusters in the system including their services, roles and complete config values.")

  @Valid

  public List<ApiCluster> getClusters() {
    return clusters;
  }

  public void setClusters(List<ApiCluster> clusters) {
    this.clusters = clusters;
  }

  public ApiDeployment hosts(List<ApiHost> hosts) {
    this.hosts = hosts;
    return this;
  }

  public ApiDeployment addHostsItem(ApiHost hostsItem) {
    if (this.hosts == null) {
      this.hosts = new ArrayList<>();
    }
    this.hosts.add(hostsItem);
    return this;
  }

  /**
   * List of hosts in the system
   * @return hosts
  **/
  @ApiModelProperty(value = "List of hosts in the system")

  @Valid

  public List<ApiHost> getHosts() {
    return hosts;
  }

  public void setHosts(List<ApiHost> hosts) {
    this.hosts = hosts;
  }

  public ApiDeployment users(List<ApiUser> users) {
    this.users = users;
    return this;
  }

  public ApiDeployment addUsersItem(ApiUser usersItem) {
    if (this.users == null) {
      this.users = new ArrayList<>();
    }
    this.users.add(usersItem);
    return this;
  }

  /**
   * List of all users in the system
   * @return users
  **/
  @ApiModelProperty(value = "List of all users in the system")

  @Valid

  public List<ApiUser> getUsers() {
    return users;
  }

  public void setUsers(List<ApiUser> users) {
    this.users = users;
  }

  public ApiDeployment versionInfo(ApiVersionInfo versionInfo) {
    this.versionInfo = versionInfo;
    return this;
  }

  /**
   * Full version information about the running Cloudera Manager instance
   * @return versionInfo
  **/
  @ApiModelProperty(value = "Full version information about the running Cloudera Manager instance")

  @Valid

  public ApiVersionInfo getVersionInfo() {
    return versionInfo;
  }

  public void setVersionInfo(ApiVersionInfo versionInfo) {
    this.versionInfo = versionInfo;
  }

  public ApiDeployment managementService(ApiService managementService) {
    this.managementService = managementService;
    return this;
  }

  /**
   * The full configuration of the Cloudera Manager management service including all the management roles and their config values
   * @return managementService
  **/
  @ApiModelProperty(value = "The full configuration of the Cloudera Manager management service including all the management roles and their config values")

  @Valid

  public ApiService getManagementService() {
    return managementService;
  }

  public void setManagementService(ApiService managementService) {
    this.managementService = managementService;
  }

  public ApiDeployment managerSettings(ApiConfigList managerSettings) {
    this.managerSettings = managerSettings;
    return this;
  }

  /**
   * The full configuration of Cloudera Manager itself including licensing info
   * @return managerSettings
  **/
  @ApiModelProperty(value = "The full configuration of Cloudera Manager itself including licensing info")

  @Valid

  public ApiConfigList getManagerSettings() {
    return managerSettings;
  }

  public void setManagerSettings(ApiConfigList managerSettings) {
    this.managerSettings = managerSettings;
  }

  public ApiDeployment allHostsConfig(ApiConfigList allHostsConfig) {
    this.allHostsConfig = allHostsConfig;
    return this;
  }

  /**
   * Configuration parameters that apply to all hosts, unless overridden at the host level. Available since API v3.
   * @return allHostsConfig
  **/
  @ApiModelProperty(value = "Configuration parameters that apply to all hosts, unless overridden at the host level. Available since API v3.")

  @Valid

  public ApiConfigList getAllHostsConfig() {
    return allHostsConfig;
  }

  public void setAllHostsConfig(ApiConfigList allHostsConfig) {
    this.allHostsConfig = allHostsConfig;
  }

  public ApiDeployment peers(List<ApiCmPeer> peers) {
    this.peers = peers;
    return this;
  }

  public ApiDeployment addPeersItem(ApiCmPeer peersItem) {
    if (this.peers == null) {
      this.peers = new ArrayList<>();
    }
    this.peers.add(peersItem);
    return this;
  }

  /**
   * The list of peers configured in Cloudera Manager. Available since API v3.
   * @return peers
  **/
  @ApiModelProperty(value = "The list of peers configured in Cloudera Manager. Available since API v3.")

  @Valid

  public List<ApiCmPeer> getPeers() {
    return peers;
  }

  public void setPeers(List<ApiCmPeer> peers) {
    this.peers = peers;
  }

  public ApiDeployment hostTemplates(ApiHostTemplateList hostTemplates) {
    this.hostTemplates = hostTemplates;
    return this;
  }

  /**
   * The list of all host templates in Cloudera Manager.
   * @return hostTemplates
  **/
  @ApiModelProperty(value = "The list of all host templates in Cloudera Manager.")

  @Valid

  public ApiHostTemplateList getHostTemplates() {
    return hostTemplates;
  }

  public void setHostTemplates(ApiHostTemplateList hostTemplates) {
    this.hostTemplates = hostTemplates;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiDeployment apiDeployment = (ApiDeployment) o;
    return Objects.equals(this.timestamp, apiDeployment.timestamp) &&
        Objects.equals(this.clusters, apiDeployment.clusters) &&
        Objects.equals(this.hosts, apiDeployment.hosts) &&
        Objects.equals(this.users, apiDeployment.users) &&
        Objects.equals(this.versionInfo, apiDeployment.versionInfo) &&
        Objects.equals(this.managementService, apiDeployment.managementService) &&
        Objects.equals(this.managerSettings, apiDeployment.managerSettings) &&
        Objects.equals(this.allHostsConfig, apiDeployment.allHostsConfig) &&
        Objects.equals(this.peers, apiDeployment.peers) &&
        Objects.equals(this.hostTemplates, apiDeployment.hostTemplates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, clusters, hosts, users, versionInfo, managementService, managerSettings, allHostsConfig, peers, hostTemplates);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiDeployment {\n");
    
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    clusters: ").append(toIndentedString(clusters)).append("\n");
    sb.append("    hosts: ").append(toIndentedString(hosts)).append("\n");
    sb.append("    users: ").append(toIndentedString(users)).append("\n");
    sb.append("    versionInfo: ").append(toIndentedString(versionInfo)).append("\n");
    sb.append("    managementService: ").append(toIndentedString(managementService)).append("\n");
    sb.append("    managerSettings: ").append(toIndentedString(managerSettings)).append("\n");
    sb.append("    allHostsConfig: ").append(toIndentedString(allHostsConfig)).append("\n");
    sb.append("    peers: ").append(toIndentedString(peers)).append("\n");
    sb.append("    hostTemplates: ").append(toIndentedString(hostTemplates)).append("\n");
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

