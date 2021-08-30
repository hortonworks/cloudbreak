package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateClusterSpec;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateHostInfo;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateRoleConfigGroupInfo;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.mock.swagger.model.ApiConfigureForKerberosArguments;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Details of cluster template
 */
@ApiModel(description = "Details of cluster template")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterTemplateInstantiator   {
  @JsonProperty("clusterName")
  private String clusterName = null;

  @JsonProperty("hosts")
  @Valid
  private List<ApiClusterTemplateHostInfo> hosts = null;

  @JsonProperty("variables")
  @Valid
  private List<ApiClusterTemplateVariable> variables = null;

  @JsonProperty("roleConfigGroups")
  @Valid
  private List<ApiClusterTemplateRoleConfigGroupInfo> roleConfigGroups = null;

  @JsonProperty("clusterSpec")
  private ApiClusterTemplateClusterSpec clusterSpec = null;

  @JsonProperty("keepHostTemplates")
  private Boolean keepHostTemplates = null;

  @JsonProperty("lenient")
  private Boolean lenient = null;

  @JsonProperty("enableKerberos")
  private ApiConfigureForKerberosArguments enableKerberos = null;

  public ApiClusterTemplateInstantiator clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  /**
   * Cluster name
   * @return clusterName
  **/
  @ApiModelProperty(value = "Cluster name")


  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public ApiClusterTemplateInstantiator hosts(List<ApiClusterTemplateHostInfo> hosts) {
    this.hosts = hosts;
    return this;
  }

  public ApiClusterTemplateInstantiator addHostsItem(ApiClusterTemplateHostInfo hostsItem) {
    if (this.hosts == null) {
      this.hosts = new ArrayList<>();
    }
    this.hosts.add(hostsItem);
    return this;
  }

  /**
   * All the hosts that are part of that cluster
   * @return hosts
  **/
  @ApiModelProperty(value = "All the hosts that are part of that cluster")

  @Valid

  public List<ApiClusterTemplateHostInfo> getHosts() {
    return hosts;
  }

  public void setHosts(List<ApiClusterTemplateHostInfo> hosts) {
    this.hosts = hosts;
  }

  public ApiClusterTemplateInstantiator variables(List<ApiClusterTemplateVariable> variables) {
    this.variables = variables;
    return this;
  }

  public ApiClusterTemplateInstantiator addVariablesItem(ApiClusterTemplateVariable variablesItem) {
    if (this.variables == null) {
      this.variables = new ArrayList<>();
    }
    this.variables.add(variablesItem);
    return this;
  }

  /**
   * All the variables the are referred by the cluster template
   * @return variables
  **/
  @ApiModelProperty(value = "All the variables the are referred by the cluster template")

  @Valid

  public List<ApiClusterTemplateVariable> getVariables() {
    return variables;
  }

  public void setVariables(List<ApiClusterTemplateVariable> variables) {
    this.variables = variables;
  }

  public ApiClusterTemplateInstantiator roleConfigGroups(List<ApiClusterTemplateRoleConfigGroupInfo> roleConfigGroups) {
    this.roleConfigGroups = roleConfigGroups;
    return this;
  }

  public ApiClusterTemplateInstantiator addRoleConfigGroupsItem(ApiClusterTemplateRoleConfigGroupInfo roleConfigGroupsItem) {
    if (this.roleConfigGroups == null) {
      this.roleConfigGroups = new ArrayList<>();
    }
    this.roleConfigGroups.add(roleConfigGroupsItem);
    return this;
  }

  /**
   * All the role config group informations for non-base RCGs.
   * @return roleConfigGroups
  **/
  @ApiModelProperty(value = "All the role config group informations for non-base RCGs.")

  @Valid

  public List<ApiClusterTemplateRoleConfigGroupInfo> getRoleConfigGroups() {
    return roleConfigGroups;
  }

  public void setRoleConfigGroups(List<ApiClusterTemplateRoleConfigGroupInfo> roleConfigGroups) {
    this.roleConfigGroups = roleConfigGroups;
  }

  public ApiClusterTemplateInstantiator clusterSpec(ApiClusterTemplateClusterSpec clusterSpec) {
    this.clusterSpec = clusterSpec;
    return this;
  }

  /**
   * Cluster specification.
   * @return clusterSpec
  **/
  @ApiModelProperty(value = "Cluster specification.")

  @Valid

  public ApiClusterTemplateClusterSpec getClusterSpec() {
    return clusterSpec;
  }

  public void setClusterSpec(ApiClusterTemplateClusterSpec clusterSpec) {
    this.clusterSpec = clusterSpec;
  }

  public ApiClusterTemplateInstantiator keepHostTemplates(Boolean keepHostTemplates) {
    this.keepHostTemplates = keepHostTemplates;
    return this;
  }

  /**
   * Keep the hosts templates from cluster template.
   * @return keepHostTemplates
  **/
  @ApiModelProperty(value = "Keep the hosts templates from cluster template.")


  public Boolean isKeepHostTemplates() {
    return keepHostTemplates;
  }

  public void setKeepHostTemplates(Boolean keepHostTemplates) {
    this.keepHostTemplates = keepHostTemplates;
  }

  public ApiClusterTemplateInstantiator lenient(Boolean lenient) {
    this.lenient = lenient;
    return this;
  }

  /**
   * Allow setting service parameters that may not currently be supported by the current CM version but will be in the future.
   * @return lenient
  **/
  @ApiModelProperty(value = "Allow setting service parameters that may not currently be supported by the current CM version but will be in the future.")


  public Boolean isLenient() {
    return lenient;
  }

  public void setLenient(Boolean lenient) {
    this.lenient = lenient;
  }

  public ApiClusterTemplateInstantiator enableKerberos(ApiConfigureForKerberosArguments enableKerberos) {
    this.enableKerberos = enableKerberos;
    return this;
  }

  /**
   * Enable kerberos authentication
   * @return enableKerberos
  **/
  @ApiModelProperty(value = "Enable kerberos authentication")

  @Valid

  public ApiConfigureForKerberosArguments getEnableKerberos() {
    return enableKerberos;
  }

  public void setEnableKerberos(ApiConfigureForKerberosArguments enableKerberos) {
    this.enableKerberos = enableKerberos;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterTemplateInstantiator apiClusterTemplateInstantiator = (ApiClusterTemplateInstantiator) o;
    return Objects.equals(this.clusterName, apiClusterTemplateInstantiator.clusterName) &&
        Objects.equals(this.hosts, apiClusterTemplateInstantiator.hosts) &&
        Objects.equals(this.variables, apiClusterTemplateInstantiator.variables) &&
        Objects.equals(this.roleConfigGroups, apiClusterTemplateInstantiator.roleConfigGroups) &&
        Objects.equals(this.clusterSpec, apiClusterTemplateInstantiator.clusterSpec) &&
        Objects.equals(this.keepHostTemplates, apiClusterTemplateInstantiator.keepHostTemplates) &&
        Objects.equals(this.lenient, apiClusterTemplateInstantiator.lenient) &&
        Objects.equals(this.enableKerberos, apiClusterTemplateInstantiator.enableKerberos);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterName, hosts, variables, roleConfigGroups, clusterSpec, keepHostTemplates, lenient, enableKerberos);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplateInstantiator {\n");
    
    sb.append("    clusterName: ").append(toIndentedString(clusterName)).append("\n");
    sb.append("    hosts: ").append(toIndentedString(hosts)).append("\n");
    sb.append("    variables: ").append(toIndentedString(variables)).append("\n");
    sb.append("    roleConfigGroups: ").append(toIndentedString(roleConfigGroups)).append("\n");
    sb.append("    clusterSpec: ").append(toIndentedString(clusterSpec)).append("\n");
    sb.append("    keepHostTemplates: ").append(toIndentedString(keepHostTemplates)).append("\n");
    sb.append("    lenient: ").append(toIndentedString(lenient)).append("\n");
    sb.append("    enableKerberos: ").append(toIndentedString(enableKerberos)).append("\n");
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

