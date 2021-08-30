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
 * This contains information about the host or host range on which provided host template will be applied.
 */
@ApiModel(description = "This contains information about the host or host range on which provided host template will be applied.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiClusterTemplateHostInfo   {
  @JsonProperty("hostName")
  private String hostName = null;

  @JsonProperty("hostNameRange")
  private String hostNameRange = null;

  @JsonProperty("rackId")
  private String rackId = null;

  @JsonProperty("hostTemplateRefName")
  private String hostTemplateRefName = null;

  @JsonProperty("roleRefNames")
  @Valid
  private List<String> roleRefNames = null;

  public ApiClusterTemplateHostInfo hostName(String hostName) {
    this.hostName = hostName;
    return this;
  }

  /**
   * Host name
   * @return hostName
  **/
  @ApiModelProperty(value = "Host name")


  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public ApiClusterTemplateHostInfo hostNameRange(String hostNameRange) {
    this.hostNameRange = hostNameRange;
    return this;
  }

  /**
   * Host range. Either this this or host name must be provided.
   * @return hostNameRange
  **/
  @ApiModelProperty(value = "Host range. Either this this or host name must be provided.")


  public String getHostNameRange() {
    return hostNameRange;
  }

  public void setHostNameRange(String hostNameRange) {
    this.hostNameRange = hostNameRange;
  }

  public ApiClusterTemplateHostInfo rackId(String rackId) {
    this.rackId = rackId;
    return this;
  }

  /**
   * Rack Id
   * @return rackId
  **/
  @ApiModelProperty(value = "Rack Id")


  public String getRackId() {
    return rackId;
  }

  public void setRackId(String rackId) {
    this.rackId = rackId;
  }

  public ApiClusterTemplateHostInfo hostTemplateRefName(String hostTemplateRefName) {
    this.hostTemplateRefName = hostTemplateRefName;
    return this;
  }

  /**
   * Pointing to the host template reference in the cluster template.
   * @return hostTemplateRefName
  **/
  @ApiModelProperty(value = "Pointing to the host template reference in the cluster template.")


  public String getHostTemplateRefName() {
    return hostTemplateRefName;
  }

  public void setHostTemplateRefName(String hostTemplateRefName) {
    this.hostTemplateRefName = hostTemplateRefName;
  }

  public ApiClusterTemplateHostInfo roleRefNames(List<String> roleRefNames) {
    this.roleRefNames = roleRefNames;
    return this;
  }

  public ApiClusterTemplateHostInfo addRoleRefNamesItem(String roleRefNamesItem) {
    if (this.roleRefNames == null) {
      this.roleRefNames = new ArrayList<>();
    }
    this.roleRefNames.add(roleRefNamesItem);
    return this;
  }

  /**
   * This will used to resolve the roles defined in the cluster template. This roleRefName will be used to connect this host with that a role reference defined in cluster template.
   * @return roleRefNames
  **/
  @ApiModelProperty(value = "This will used to resolve the roles defined in the cluster template. This roleRefName will be used to connect this host with that a role reference defined in cluster template.")


  public List<String> getRoleRefNames() {
    return roleRefNames;
  }

  public void setRoleRefNames(List<String> roleRefNames) {
    this.roleRefNames = roleRefNames;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiClusterTemplateHostInfo apiClusterTemplateHostInfo = (ApiClusterTemplateHostInfo) o;
    return Objects.equals(this.hostName, apiClusterTemplateHostInfo.hostName) &&
        Objects.equals(this.hostNameRange, apiClusterTemplateHostInfo.hostNameRange) &&
        Objects.equals(this.rackId, apiClusterTemplateHostInfo.rackId) &&
        Objects.equals(this.hostTemplateRefName, apiClusterTemplateHostInfo.hostTemplateRefName) &&
        Objects.equals(this.roleRefNames, apiClusterTemplateHostInfo.roleRefNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostName, hostNameRange, rackId, hostTemplateRefName, roleRefNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplateHostInfo {\n");
    
    sb.append("    hostName: ").append(toIndentedString(hostName)).append("\n");
    sb.append("    hostNameRange: ").append(toIndentedString(hostNameRange)).append("\n");
    sb.append("    rackId: ").append(toIndentedString(rackId)).append("\n");
    sb.append("    hostTemplateRefName: ").append(toIndentedString(hostTemplateRefName)).append("\n");
    sb.append("    roleRefNames: ").append(toIndentedString(roleRefNames)).append("\n");
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

