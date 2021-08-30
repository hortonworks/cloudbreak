package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateClusterSpec;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateHostTemplate;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateInstantiator;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.mock.swagger.model.ApiEntityTag;
import com.sequenceiq.mock.swagger.model.ApiProductVersion;
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




public class ApiClusterTemplate   {
  @JsonProperty("cdhVersion")
  private String cdhVersion = null;

  @JsonProperty("experienceVersion")
  private String experienceVersion = null;

  @JsonProperty("products")
  @Valid
  private List<ApiProductVersion> products = null;

  @JsonProperty("services")
  @Valid
  private List<ApiClusterTemplateService> services = null;

  @JsonProperty("hostTemplates")
  @Valid
  private List<ApiClusterTemplateHostTemplate> hostTemplates = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("cmVersion")
  private String cmVersion = null;

  @JsonProperty("instantiator")
  private ApiClusterTemplateInstantiator instantiator = null;

  @JsonProperty("repositories")
  @Valid
  private List<String> repositories = null;

  @JsonProperty("clusterSpec")
  private ApiClusterTemplateClusterSpec clusterSpec = null;

  @JsonProperty("tags")
  @Valid
  private List<ApiEntityTag> tags = null;

  public ApiClusterTemplate cdhVersion(String cdhVersion) {
    this.cdhVersion = cdhVersion;
    return this;
  }

  /**
   * CDH version
   * @return cdhVersion
  **/
  @ApiModelProperty(value = "CDH version")


  public String getCdhVersion() {
    return cdhVersion;
  }

  public void setCdhVersion(String cdhVersion) {
    this.cdhVersion = cdhVersion;
  }

  public ApiClusterTemplate experienceVersion(String experienceVersion) {
    this.experienceVersion = experienceVersion;
    return this;
  }

  /**
   * Experience version
   * @return experienceVersion
  **/
  @ApiModelProperty(value = "Experience version")


  public String getExperienceVersion() {
    return experienceVersion;
  }

  public void setExperienceVersion(String experienceVersion) {
    this.experienceVersion = experienceVersion;
  }

  public ApiClusterTemplate products(List<ApiProductVersion> products) {
    this.products = products;
    return this;
  }

  public ApiClusterTemplate addProductsItem(ApiProductVersion productsItem) {
    if (this.products == null) {
      this.products = new ArrayList<>();
    }
    this.products.add(productsItem);
    return this;
  }

  /**
   * All the parcels that needs to be deployed and activated
   * @return products
  **/
  @ApiModelProperty(value = "All the parcels that needs to be deployed and activated")

  @Valid

  public List<ApiProductVersion> getProducts() {
    return products;
  }

  public void setProducts(List<ApiProductVersion> products) {
    this.products = products;
  }

  public ApiClusterTemplate services(List<ApiClusterTemplateService> services) {
    this.services = services;
    return this;
  }

  public ApiClusterTemplate addServicesItem(ApiClusterTemplateService servicesItem) {
    if (this.services == null) {
      this.services = new ArrayList<>();
    }
    this.services.add(servicesItem);
    return this;
  }

  /**
   * All the services that needs to be deployed
   * @return services
  **/
  @ApiModelProperty(value = "All the services that needs to be deployed")

  @Valid

  public List<ApiClusterTemplateService> getServices() {
    return services;
  }

  public void setServices(List<ApiClusterTemplateService> services) {
    this.services = services;
  }

  public ApiClusterTemplate hostTemplates(List<ApiClusterTemplateHostTemplate> hostTemplates) {
    this.hostTemplates = hostTemplates;
    return this;
  }

  public ApiClusterTemplate addHostTemplatesItem(ApiClusterTemplateHostTemplate hostTemplatesItem) {
    if (this.hostTemplates == null) {
      this.hostTemplates = new ArrayList<>();
    }
    this.hostTemplates.add(hostTemplatesItem);
    return this;
  }

  /**
   * All host templates
   * @return hostTemplates
  **/
  @ApiModelProperty(value = "All host templates")

  @Valid

  public List<ApiClusterTemplateHostTemplate> getHostTemplates() {
    return hostTemplates;
  }

  public void setHostTemplates(List<ApiClusterTemplateHostTemplate> hostTemplates) {
    this.hostTemplates = hostTemplates;
  }

  public ApiClusterTemplate displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Cluster display name
   * @return displayName
  **/
  @ApiModelProperty(value = "Cluster display name")


  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ApiClusterTemplate cmVersion(String cmVersion) {
    this.cmVersion = cmVersion;
    return this;
  }

  /**
   * CM version for which the template
   * @return cmVersion
  **/
  @ApiModelProperty(value = "CM version for which the template")


  public String getCmVersion() {
    return cmVersion;
  }

  public void setCmVersion(String cmVersion) {
    this.cmVersion = cmVersion;
  }

  public ApiClusterTemplate instantiator(ApiClusterTemplateInstantiator instantiator) {
    this.instantiator = instantiator;
    return this;
  }

  /**
   * A constructor listing all the variables and references that needs to be resolved for this template
   * @return instantiator
  **/
  @ApiModelProperty(value = "A constructor listing all the variables and references that needs to be resolved for this template")

  @Valid

  public ApiClusterTemplateInstantiator getInstantiator() {
    return instantiator;
  }

  public void setInstantiator(ApiClusterTemplateInstantiator instantiator) {
    this.instantiator = instantiator;
  }

  public ApiClusterTemplate repositories(List<String> repositories) {
    this.repositories = repositories;
    return this;
  }

  public ApiClusterTemplate addRepositoriesItem(String repositoriesItem) {
    if (this.repositories == null) {
      this.repositories = new ArrayList<>();
    }
    this.repositories.add(repositoriesItem);
    return this;
  }

  /**
   * List of all repositories registered with CM
   * @return repositories
  **/
  @ApiModelProperty(value = "List of all repositories registered with CM")


  public List<String> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<String> repositories) {
    this.repositories = repositories;
  }

  public ApiClusterTemplate clusterSpec(ApiClusterTemplateClusterSpec clusterSpec) {
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

  public ApiClusterTemplate tags(List<ApiEntityTag> tags) {
    this.tags = tags;
    return this;
  }

  public ApiClusterTemplate addTagsItem(ApiEntityTag tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Tags associated with the cluster
   * @return tags
  **/
  @ApiModelProperty(value = "Tags associated with the cluster")

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
    ApiClusterTemplate apiClusterTemplate = (ApiClusterTemplate) o;
    return Objects.equals(this.cdhVersion, apiClusterTemplate.cdhVersion) &&
        Objects.equals(this.experienceVersion, apiClusterTemplate.experienceVersion) &&
        Objects.equals(this.products, apiClusterTemplate.products) &&
        Objects.equals(this.services, apiClusterTemplate.services) &&
        Objects.equals(this.hostTemplates, apiClusterTemplate.hostTemplates) &&
        Objects.equals(this.displayName, apiClusterTemplate.displayName) &&
        Objects.equals(this.cmVersion, apiClusterTemplate.cmVersion) &&
        Objects.equals(this.instantiator, apiClusterTemplate.instantiator) &&
        Objects.equals(this.repositories, apiClusterTemplate.repositories) &&
        Objects.equals(this.clusterSpec, apiClusterTemplate.clusterSpec) &&
        Objects.equals(this.tags, apiClusterTemplate.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cdhVersion, experienceVersion, products, services, hostTemplates, displayName, cmVersion, instantiator, repositories, clusterSpec, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiClusterTemplate {\n");
    
    sb.append("    cdhVersion: ").append(toIndentedString(cdhVersion)).append("\n");
    sb.append("    experienceVersion: ").append(toIndentedString(experienceVersion)).append("\n");
    sb.append("    products: ").append(toIndentedString(products)).append("\n");
    sb.append("    services: ").append(toIndentedString(services)).append("\n");
    sb.append("    hostTemplates: ").append(toIndentedString(hostTemplates)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    cmVersion: ").append(toIndentedString(cmVersion)).append("\n");
    sb.append("    instantiator: ").append(toIndentedString(instantiator)).append("\n");
    sb.append("    repositories: ").append(toIndentedString(repositories)).append("\n");
    sb.append("    clusterSpec: ").append(toIndentedString(clusterSpec)).append("\n");
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

