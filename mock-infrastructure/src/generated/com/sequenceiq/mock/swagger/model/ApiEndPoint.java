package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiEndPointHost;
import com.sequenceiq.mock.swagger.model.ApiMapEntry;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This defines a single logical service in the SDX cluster. A single service can expose one or more URLs. This ApiEndPoint groups the URLs logically for configuration, versioning, or any other service specific reason.
 */
@ApiModel(description = "This defines a single logical service in the SDX cluster. A single service can expose one or more URLs. This ApiEndPoint groups the URLs logically for configuration, versioning, or any other service specific reason.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiEndPoint   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("version")
  private String version = null;

  @JsonProperty("serviceConfigs")
  @Valid
  private List<ApiMapEntry> serviceConfigs = null;

  @JsonProperty("endPointHostList")
  @Valid
  private List<ApiEndPointHost> endPointHostList = null;

  @JsonProperty("serviceType")
  private String serviceType = null;

  public ApiEndPoint name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Name for the endPoint.
   * @return name
  **/
  @ApiModelProperty(value = "Name for the endPoint.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiEndPoint version(String version) {
    this.version = version;
    return this;
  }

  /**
   * Endpoint specific version.
   * @return version
  **/
  @ApiModelProperty(value = "Endpoint specific version.")


  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public ApiEndPoint serviceConfigs(List<ApiMapEntry> serviceConfigs) {
    this.serviceConfigs = serviceConfigs;
    return this;
  }

  public ApiEndPoint addServiceConfigsItem(ApiMapEntry serviceConfigsItem) {
    if (this.serviceConfigs == null) {
      this.serviceConfigs = new ArrayList<>();
    }
    this.serviceConfigs.add(serviceConfigsItem);
    return this;
  }

  /**
   * Additional configs for the endPoint.
   * @return serviceConfigs
  **/
  @ApiModelProperty(value = "Additional configs for the endPoint.")

  @Valid

  public List<ApiMapEntry> getServiceConfigs() {
    return serviceConfigs;
  }

  public void setServiceConfigs(List<ApiMapEntry> serviceConfigs) {
    this.serviceConfigs = serviceConfigs;
  }

  public ApiEndPoint endPointHostList(List<ApiEndPointHost> endPointHostList) {
    this.endPointHostList = endPointHostList;
    return this;
  }

  public ApiEndPoint addEndPointHostListItem(ApiEndPointHost endPointHostListItem) {
    if (this.endPointHostList == null) {
      this.endPointHostList = new ArrayList<>();
    }
    this.endPointHostList.add(endPointHostListItem);
    return this;
  }

  /**
   * List hosts (uris) for this endPoint.
   * @return endPointHostList
  **/
  @ApiModelProperty(value = "List hosts (uris) for this endPoint.")

  @Valid

  public List<ApiEndPointHost> getEndPointHostList() {
    return endPointHostList;
  }

  public void setEndPointHostList(List<ApiEndPointHost> endPointHostList) {
    this.endPointHostList = endPointHostList;
  }

  public ApiEndPoint serviceType(String serviceType) {
    this.serviceType = serviceType;
    return this;
  }

  /**
   * Endpoint service type.
   * @return serviceType
  **/
  @ApiModelProperty(value = "Endpoint service type.")


  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEndPoint apiEndPoint = (ApiEndPoint) o;
    return Objects.equals(this.name, apiEndPoint.name) &&
        Objects.equals(this.version, apiEndPoint.version) &&
        Objects.equals(this.serviceConfigs, apiEndPoint.serviceConfigs) &&
        Objects.equals(this.endPointHostList, apiEndPoint.endPointHostList) &&
        Objects.equals(this.serviceType, apiEndPoint.serviceType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version, serviceConfigs, endPointHostList, serviceType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEndPoint {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    serviceConfigs: ").append(toIndentedString(serviceConfigs)).append("\n");
    sb.append("    endPointHostList: ").append(toIndentedString(endPointHostList)).append("\n");
    sb.append("    serviceType: ").append(toIndentedString(serviceType)).append("\n");
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

