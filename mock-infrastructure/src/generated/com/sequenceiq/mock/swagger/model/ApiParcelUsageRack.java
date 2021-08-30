package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiParcelUsageHost;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This object is used to represent a rack within an ApiParcelUsage.
 */
@ApiModel(description = "This object is used to represent a rack within an ApiParcelUsage.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiParcelUsageRack   {
  @JsonProperty("hosts")
  @Valid
  private List<ApiParcelUsageHost> hosts = null;

  @JsonProperty("rackId")
  private String rackId = null;

  public ApiParcelUsageRack hosts(List<ApiParcelUsageHost> hosts) {
    this.hosts = hosts;
    return this;
  }

  public ApiParcelUsageRack addHostsItem(ApiParcelUsageHost hostsItem) {
    if (this.hosts == null) {
      this.hosts = new ArrayList<>();
    }
    this.hosts.add(hostsItem);
    return this;
  }

  /**
   * A collection of the hosts in the rack.
   * @return hosts
  **/
  @ApiModelProperty(value = "A collection of the hosts in the rack.")

  @Valid

  public List<ApiParcelUsageHost> getHosts() {
    return hosts;
  }

  public void setHosts(List<ApiParcelUsageHost> hosts) {
    this.hosts = hosts;
  }

  public ApiParcelUsageRack rackId(String rackId) {
    this.rackId = rackId;
    return this;
  }

  /**
   * The rack ID for the rack.
   * @return rackId
  **/
  @ApiModelProperty(value = "The rack ID for the rack.")


  public String getRackId() {
    return rackId;
  }

  public void setRackId(String rackId) {
    this.rackId = rackId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiParcelUsageRack apiParcelUsageRack = (ApiParcelUsageRack) o;
    return Objects.equals(this.hosts, apiParcelUsageRack.hosts) &&
        Objects.equals(this.rackId, apiParcelUsageRack.rackId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hosts, rackId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiParcelUsageRack {\n");
    
    sb.append("    hosts: ").append(toIndentedString(hosts)).append("\n");
    sb.append("    rackId: ").append(toIndentedString(rackId)).append("\n");
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

