package com.sequenceiq.mock.swagger.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Arguments passed to the remove hosts from cluster api, every host passed in has the roles in it deleted and the host is removed from the cluster, but is still managed by CM.  If deleteHosts is set to true hosts are also deleted from CM.
 */
@ApiModel(description = "Arguments passed to the remove hosts from cluster api, every host passed in has the roles in it deleted and the host is removed from the cluster, but is still managed by CM.  If deleteHosts is set to true hosts are also deleted from CM.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiHostsToRemoveArgs   {
  @JsonProperty("hostsToRemove")
  @Valid
  private List<String> hostsToRemove = null;

  @JsonProperty("deleteHosts")
  private Boolean deleteHosts = null;

  public ApiHostsToRemoveArgs hostsToRemove(List<String> hostsToRemove) {
    this.hostsToRemove = hostsToRemove;
    return this;
  }

  public ApiHostsToRemoveArgs addHostsToRemoveItem(String hostsToRemoveItem) {
    if (this.hostsToRemove == null) {
      this.hostsToRemove = new ArrayList<>();
    }
    this.hostsToRemove.add(hostsToRemoveItem);
    return this;
  }

  /**
   *
   * @return hostsToRemove
  **/
  @ApiModelProperty(value = "")


  public List<String> getHostsToRemove() {
    return hostsToRemove;
  }

  public void setHostsToRemove(List<String> hostsToRemove) {
    this.hostsToRemove = hostsToRemove;
  }

  public ApiHostsToRemoveArgs deleteHosts(Boolean deleteHosts) {
    this.deleteHosts = deleteHosts;
    return this;
  }

  /**
   *
   * @return deleteHosts
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean isDeleteHosts() {
    return deleteHosts;
  }

  public void setDeleteHosts(Boolean deleteHosts) {
    this.deleteHosts = deleteHosts;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiHostsToRemoveArgs apiHostsToRemoveArgs = (ApiHostsToRemoveArgs) o;
    return Objects.equals(this.hostsToRemove, apiHostsToRemoveArgs.hostsToRemove) &&
        Objects.equals(this.deleteHosts, apiHostsToRemoveArgs.deleteHosts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostsToRemove, deleteHosts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiHostsToRemoveArgs {\n");

    sb.append("    hostsToRemove: ").append(toIndentedString(hostsToRemove)).append("\n");
    sb.append("    deleteHosts: ").append(toIndentedString(deleteHosts)).append("\n");
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

