package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiConfigStalenessStatus;
import com.sequenceiq.mock.swagger.model.ApiHealthSummary;
import com.sequenceiq.mock.swagger.model.ApiMapEntry;
import com.sequenceiq.mock.swagger.model.ApiService;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ApiDataContext is the persistent storage/services context exported by the base cluster to be consumed by the compute cluster.
 */
@ApiModel(description = "ApiDataContext is the persistent storage/services context exported by the base cluster to be consumed by the compute cluster.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiDataContext   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("nameservice")
  private String nameservice = null;

  @JsonProperty("createdTime")
  private String createdTime = null;

  @JsonProperty("lastModifiedTime")
  private String lastModifiedTime = null;

  @JsonProperty("services")
  @Valid
  private List<ApiServiceRef> services = null;

  @JsonProperty("servicesDetails")
  @Valid
  private List<ApiService> servicesDetails = null;

  @JsonProperty("supportedServiceTypes")
  @Valid
  private List<String> supportedServiceTypes = null;

  @JsonProperty("allowedClusterVersions")
  @Valid
  private List<ApiMapEntry> allowedClusterVersions = null;

  @JsonProperty("configStalenessStatus")
  private ApiConfigStalenessStatus configStalenessStatus = null;

  @JsonProperty("clientConfigStalenessStatus")
  private ApiConfigStalenessStatus clientConfigStalenessStatus = null;

  @JsonProperty("healthSummary")
  private ApiHealthSummary healthSummary = null;

  public ApiDataContext name(String name) {
    this.name = name;
    return this;
  }

  /**
   * 
   * @return name
  **/
  @ApiModelProperty(value = "")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiDataContext displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * 
   * @return displayName
  **/
  @ApiModelProperty(value = "")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiDataContext nameservice(String nameservice) {
    this.nameservice = nameservice;
    return this;
  }

  /**
   * 
   * @return nameservice
  **/
  @ApiModelProperty(value = "")


  public String getNameservice() {
    return nameservice;
  }

  public void setNameservice(String nameservice) {
    this.nameservice = nameservice;
  }

  public ApiDataContext createdTime(String createdTime) {
    this.createdTime = createdTime;
    return this;
  }

  /**
   * 
   * @return createdTime
  **/
  @ApiModelProperty(value = "")


  public String getCreatedTime() {
    return createdTime;
  }

  public void setCreatedTime(String createdTime) {
    this.createdTime = createdTime;
  }

  public ApiDataContext lastModifiedTime(String lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

  /**
   * 
   * @return lastModifiedTime
  **/
  @ApiModelProperty(value = "")


  public String getLastModifiedTime() {
    return lastModifiedTime;
  }

  public void setLastModifiedTime(String lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  public ApiDataContext services(List<ApiServiceRef> services) {
    this.services = services;
    return this;
  }

  public ApiDataContext addServicesItem(ApiServiceRef servicesItem) {
    if (this.services == null) {
      this.services = new ArrayList<>();
    }
    this.services.add(servicesItem);
    return this;
  }

  /**
   * 
   * @return services
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiServiceRef> getServices() {
    return services;
  }

  public void setServices(List<ApiServiceRef> services) {
    this.services = services;
  }

  public ApiDataContext servicesDetails(List<ApiService> servicesDetails) {
    this.servicesDetails = servicesDetails;
    return this;
  }

  public ApiDataContext addServicesDetailsItem(ApiService servicesDetailsItem) {
    if (this.servicesDetails == null) {
      this.servicesDetails = new ArrayList<>();
    }
    this.servicesDetails.add(servicesDetailsItem);
    return this;
  }

  /**
   * 
   * @return servicesDetails
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiService> getServicesDetails() {
    return servicesDetails;
  }

  public void setServicesDetails(List<ApiService> servicesDetails) {
    this.servicesDetails = servicesDetails;
  }

  public ApiDataContext supportedServiceTypes(List<String> supportedServiceTypes) {
    this.supportedServiceTypes = supportedServiceTypes;
    return this;
  }

  public ApiDataContext addSupportedServiceTypesItem(String supportedServiceTypesItem) {
    if (this.supportedServiceTypes == null) {
      this.supportedServiceTypes = new ArrayList<>();
    }
    this.supportedServiceTypes.add(supportedServiceTypesItem);
    return this;
  }

  /**
   * 
   * @return supportedServiceTypes
  **/
  @ApiModelProperty(value = "")


  public List<String> getSupportedServiceTypes() {
    return supportedServiceTypes;
  }

  public void setSupportedServiceTypes(List<String> supportedServiceTypes) {
    this.supportedServiceTypes = supportedServiceTypes;
  }

  public ApiDataContext allowedClusterVersions(List<ApiMapEntry> allowedClusterVersions) {
    this.allowedClusterVersions = allowedClusterVersions;
    return this;
  }

  public ApiDataContext addAllowedClusterVersionsItem(ApiMapEntry allowedClusterVersionsItem) {
    if (this.allowedClusterVersions == null) {
      this.allowedClusterVersions = new ArrayList<>();
    }
    this.allowedClusterVersions.add(allowedClusterVersionsItem);
    return this;
  }

  /**
   * 
   * @return allowedClusterVersions
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ApiMapEntry> getAllowedClusterVersions() {
    return allowedClusterVersions;
  }

  public void setAllowedClusterVersions(List<ApiMapEntry> allowedClusterVersions) {
    this.allowedClusterVersions = allowedClusterVersions;
  }

  public ApiDataContext configStalenessStatus(ApiConfigStalenessStatus configStalenessStatus) {
    this.configStalenessStatus = configStalenessStatus;
    return this;
  }

  /**
   * 
   * @return configStalenessStatus
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiConfigStalenessStatus getConfigStalenessStatus() {
    return configStalenessStatus;
  }

  public void setConfigStalenessStatus(ApiConfigStalenessStatus configStalenessStatus) {
    this.configStalenessStatus = configStalenessStatus;
  }

  public ApiDataContext clientConfigStalenessStatus(ApiConfigStalenessStatus clientConfigStalenessStatus) {
    this.clientConfigStalenessStatus = clientConfigStalenessStatus;
    return this;
  }

  /**
   * 
   * @return clientConfigStalenessStatus
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiConfigStalenessStatus getClientConfigStalenessStatus() {
    return clientConfigStalenessStatus;
  }

  public void setClientConfigStalenessStatus(ApiConfigStalenessStatus clientConfigStalenessStatus) {
    this.clientConfigStalenessStatus = clientConfigStalenessStatus;
  }

  public ApiDataContext healthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
    return this;
  }

  /**
   * 
   * @return healthSummary
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ApiHealthSummary getHealthSummary() {
    return healthSummary;
  }

  public void setHealthSummary(ApiHealthSummary healthSummary) {
    this.healthSummary = healthSummary;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiDataContext apiDataContext = (ApiDataContext) o;
    return Objects.equals(this.name, apiDataContext.name) &&
        Objects.equals(this.displayName, apiDataContext.displayName) &&
        Objects.equals(this.nameservice, apiDataContext.nameservice) &&
        Objects.equals(this.createdTime, apiDataContext.createdTime) &&
        Objects.equals(this.lastModifiedTime, apiDataContext.lastModifiedTime) &&
        Objects.equals(this.services, apiDataContext.services) &&
        Objects.equals(this.servicesDetails, apiDataContext.servicesDetails) &&
        Objects.equals(this.supportedServiceTypes, apiDataContext.supportedServiceTypes) &&
        Objects.equals(this.allowedClusterVersions, apiDataContext.allowedClusterVersions) &&
        Objects.equals(this.configStalenessStatus, apiDataContext.configStalenessStatus) &&
        Objects.equals(this.clientConfigStalenessStatus, apiDataContext.clientConfigStalenessStatus) &&
        Objects.equals(this.healthSummary, apiDataContext.healthSummary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, displayName, nameservice, createdTime, lastModifiedTime, services, servicesDetails, supportedServiceTypes, allowedClusterVersions, configStalenessStatus, clientConfigStalenessStatus, healthSummary);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiDataContext {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    nameservice: ").append(toIndentedString(nameservice)).append("\n");
    sb.append("    createdTime: ").append(toIndentedString(createdTime)).append("\n");
    sb.append("    lastModifiedTime: ").append(toIndentedString(lastModifiedTime)).append("\n");
    sb.append("    services: ").append(toIndentedString(services)).append("\n");
    sb.append("    servicesDetails: ").append(toIndentedString(servicesDetails)).append("\n");
    sb.append("    supportedServiceTypes: ").append(toIndentedString(supportedServiceTypes)).append("\n");
    sb.append("    allowedClusterVersions: ").append(toIndentedString(allowedClusterVersions)).append("\n");
    sb.append("    configStalenessStatus: ").append(toIndentedString(configStalenessStatus)).append("\n");
    sb.append("    clientConfigStalenessStatus: ").append(toIndentedString(clientConfigStalenessStatus)).append("\n");
    sb.append("    healthSummary: ").append(toIndentedString(healthSummary)).append("\n");
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

