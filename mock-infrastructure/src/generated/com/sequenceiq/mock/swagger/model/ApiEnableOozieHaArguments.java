package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * 
 */
@ApiModel(description = "")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiEnableOozieHaArguments   {
  @JsonProperty("newOozieServerHostIds")
  @Valid
  private List<String> newOozieServerHostIds = null;

  @JsonProperty("newOozieServerRoleNames")
  @Valid
  private List<String> newOozieServerRoleNames = null;

  @JsonProperty("zkServiceName")
  private String zkServiceName = null;

  @JsonProperty("loadBalancerHostname")
  private String loadBalancerHostname = null;

  @JsonProperty("loadBalancerPort")
  private Integer loadBalancerPort = null;

  @JsonProperty("loadBalancerSslPort")
  private Integer loadBalancerSslPort = null;

  @JsonProperty("loadBalancerHostPort")
  private String loadBalancerHostPort = null;

  public ApiEnableOozieHaArguments newOozieServerHostIds(List<String> newOozieServerHostIds) {
    this.newOozieServerHostIds = newOozieServerHostIds;
    return this;
  }

  public ApiEnableOozieHaArguments addNewOozieServerHostIdsItem(String newOozieServerHostIdsItem) {
    if (this.newOozieServerHostIds == null) {
      this.newOozieServerHostIds = new ArrayList<>();
    }
    this.newOozieServerHostIds.add(newOozieServerHostIdsItem);
    return this;
  }

  /**
   * IDs of the hosts on which new Oozie Servers will be added.
   * @return newOozieServerHostIds
  **/
  @ApiModelProperty(value = "IDs of the hosts on which new Oozie Servers will be added.")


  public List<String> getNewOozieServerHostIds() {
    return newOozieServerHostIds;
  }

  public void setNewOozieServerHostIds(List<String> newOozieServerHostIds) {
    this.newOozieServerHostIds = newOozieServerHostIds;
  }

  public ApiEnableOozieHaArguments newOozieServerRoleNames(List<String> newOozieServerRoleNames) {
    this.newOozieServerRoleNames = newOozieServerRoleNames;
    return this;
  }

  public ApiEnableOozieHaArguments addNewOozieServerRoleNamesItem(String newOozieServerRoleNamesItem) {
    if (this.newOozieServerRoleNames == null) {
      this.newOozieServerRoleNames = new ArrayList<>();
    }
    this.newOozieServerRoleNames.add(newOozieServerRoleNamesItem);
    return this;
  }

  /**
   * Names of the new Oozie Servers. This is an optional argument, but if provided, it should match the length of host IDs provided.
   * @return newOozieServerRoleNames
  **/
  @ApiModelProperty(value = "Names of the new Oozie Servers. This is an optional argument, but if provided, it should match the length of host IDs provided.")


  public List<String> getNewOozieServerRoleNames() {
    return newOozieServerRoleNames;
  }

  public void setNewOozieServerRoleNames(List<String> newOozieServerRoleNames) {
    this.newOozieServerRoleNames = newOozieServerRoleNames;
  }

  public ApiEnableOozieHaArguments zkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
    return this;
  }

  /**
   * Name of the ZooKeeper service that will be used for Oozie HA. This is an optional parameter if the Oozie to ZooKeeper dependency is already set in CM.
   * @return zkServiceName
  **/
  @ApiModelProperty(value = "Name of the ZooKeeper service that will be used for Oozie HA. This is an optional parameter if the Oozie to ZooKeeper dependency is already set in CM.")


  public String getZkServiceName() {
    return zkServiceName;
  }

  public void setZkServiceName(String zkServiceName) {
    this.zkServiceName = zkServiceName;
  }

  public ApiEnableOozieHaArguments loadBalancerHostname(String loadBalancerHostname) {
    this.loadBalancerHostname = loadBalancerHostname;
    return this;
  }

  /**
   * Hostname of the load balancer used for Oozie HA. Optional if load balancer host and ports are already set in CM.
   * @return loadBalancerHostname
  **/
  @ApiModelProperty(value = "Hostname of the load balancer used for Oozie HA. Optional if load balancer host and ports are already set in CM.")


  public String getLoadBalancerHostname() {
    return loadBalancerHostname;
  }

  public void setLoadBalancerHostname(String loadBalancerHostname) {
    this.loadBalancerHostname = loadBalancerHostname;
  }

  public ApiEnableOozieHaArguments loadBalancerPort(Integer loadBalancerPort) {
    this.loadBalancerPort = loadBalancerPort;
    return this;
  }

  /**
   * HTTP port of the load balancer used for Oozie HA. Optional if load balancer host and ports are already set in CM.
   * @return loadBalancerPort
  **/
  @ApiModelProperty(value = "HTTP port of the load balancer used for Oozie HA. Optional if load balancer host and ports are already set in CM.")


  public Integer getLoadBalancerPort() {
    return loadBalancerPort;
  }

  public void setLoadBalancerPort(Integer loadBalancerPort) {
    this.loadBalancerPort = loadBalancerPort;
  }

  public ApiEnableOozieHaArguments loadBalancerSslPort(Integer loadBalancerSslPort) {
    this.loadBalancerSslPort = loadBalancerSslPort;
    return this;
  }

  /**
   * HTTPS port of the load balancer used for Oozie HA when SSL is enabled. This port is only used for oozie.base.url -- the callback is always on HTTP. Optional if load balancer host and ports are already set in CM.
   * @return loadBalancerSslPort
  **/
  @ApiModelProperty(value = "HTTPS port of the load balancer used for Oozie HA when SSL is enabled. This port is only used for oozie.base.url -- the callback is always on HTTP. Optional if load balancer host and ports are already set in CM.")


  public Integer getLoadBalancerSslPort() {
    return loadBalancerSslPort;
  }

  public void setLoadBalancerSslPort(Integer loadBalancerSslPort) {
    this.loadBalancerSslPort = loadBalancerSslPort;
  }

  public ApiEnableOozieHaArguments loadBalancerHostPort(String loadBalancerHostPort) {
    this.loadBalancerHostPort = loadBalancerHostPort;
    return this;
  }

  /**
   * Address of the load balancer used for Oozie HA. This is an optional parameter if this config is already set in CM.
   * @return loadBalancerHostPort
  **/
  @ApiModelProperty(value = "Address of the load balancer used for Oozie HA. This is an optional parameter if this config is already set in CM.")


  public String getLoadBalancerHostPort() {
    return loadBalancerHostPort;
  }

  public void setLoadBalancerHostPort(String loadBalancerHostPort) {
    this.loadBalancerHostPort = loadBalancerHostPort;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEnableOozieHaArguments apiEnableOozieHaArguments = (ApiEnableOozieHaArguments) o;
    return Objects.equals(this.newOozieServerHostIds, apiEnableOozieHaArguments.newOozieServerHostIds) &&
        Objects.equals(this.newOozieServerRoleNames, apiEnableOozieHaArguments.newOozieServerRoleNames) &&
        Objects.equals(this.zkServiceName, apiEnableOozieHaArguments.zkServiceName) &&
        Objects.equals(this.loadBalancerHostname, apiEnableOozieHaArguments.loadBalancerHostname) &&
        Objects.equals(this.loadBalancerPort, apiEnableOozieHaArguments.loadBalancerPort) &&
        Objects.equals(this.loadBalancerSslPort, apiEnableOozieHaArguments.loadBalancerSslPort) &&
        Objects.equals(this.loadBalancerHostPort, apiEnableOozieHaArguments.loadBalancerHostPort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(newOozieServerHostIds, newOozieServerRoleNames, zkServiceName, loadBalancerHostname, loadBalancerPort, loadBalancerSslPort, loadBalancerHostPort);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEnableOozieHaArguments {\n");
    
    sb.append("    newOozieServerHostIds: ").append(toIndentedString(newOozieServerHostIds)).append("\n");
    sb.append("    newOozieServerRoleNames: ").append(toIndentedString(newOozieServerRoleNames)).append("\n");
    sb.append("    zkServiceName: ").append(toIndentedString(zkServiceName)).append("\n");
    sb.append("    loadBalancerHostname: ").append(toIndentedString(loadBalancerHostname)).append("\n");
    sb.append("    loadBalancerPort: ").append(toIndentedString(loadBalancerPort)).append("\n");
    sb.append("    loadBalancerSslPort: ").append(toIndentedString(loadBalancerSslPort)).append("\n");
    sb.append("    loadBalancerHostPort: ").append(toIndentedString(loadBalancerHostPort)).append("\n");
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

