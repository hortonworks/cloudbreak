package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * This is the model for a component in the host.
 */
@ApiModel(description = "This is the model for a component in the host.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiComponentInfo   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("cdhVersion")
  private String cdhVersion = null;

  @JsonProperty("cdhRelease")
  private String cdhRelease = null;

  @JsonProperty("componentVersion")
  private String componentVersion = null;

  @JsonProperty("componentRelease")
  private String componentRelease = null;

  @JsonProperty("componentInfoSource")
  private String componentInfoSource = null;

  @JsonProperty("isActive")
  private Boolean isActive = null;

  @JsonProperty("componentConfig")
  @Valid
  private Map<String, String> componentConfig = null;

  public ApiComponentInfo name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the component.
   * @return name
  **/
  @ApiModelProperty(value = "The name of the component.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiComponentInfo cdhVersion(String cdhVersion) {
    this.cdhVersion = cdhVersion;
    return this;
  }

  /**
   * The CDH version on the host.
   * @return cdhVersion
  **/
  @ApiModelProperty(value = "The CDH version on the host.")


  public String getCdhVersion() {
    return cdhVersion;
  }

  public void setCdhVersion(String cdhVersion) {
    this.cdhVersion = cdhVersion;
  }

  public ApiComponentInfo cdhRelease(String cdhRelease) {
    this.cdhRelease = cdhRelease;
    return this;
  }

  /**
   * CDH release on the host.
   * @return cdhRelease
  **/
  @ApiModelProperty(value = "CDH release on the host.")


  public String getCdhRelease() {
    return cdhRelease;
  }

  public void setCdhRelease(String cdhRelease) {
    this.cdhRelease = cdhRelease;
  }

  public ApiComponentInfo componentVersion(String componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * Component version on the host.
   * @return componentVersion
  **/
  @ApiModelProperty(value = "Component version on the host.")


  public String getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(String componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ApiComponentInfo componentRelease(String componentRelease) {
    this.componentRelease = componentRelease;
    return this;
  }

  /**
   * Component release on the host.
   * @return componentRelease
  **/
  @ApiModelProperty(value = "Component release on the host.")


  public String getComponentRelease() {
    return componentRelease;
  }

  public void setComponentRelease(String componentRelease) {
    this.componentRelease = componentRelease;
  }

  public ApiComponentInfo componentInfoSource(String componentInfoSource) {
    this.componentInfoSource = componentInfoSource;
    return this;
  }

  /**
   * Source from which component is taken.
   * @return componentInfoSource
  **/
  @ApiModelProperty(value = "Source from which component is taken.")


  public String getComponentInfoSource() {
    return componentInfoSource;
  }

  public void setComponentInfoSource(String componentInfoSource) {
    this.componentInfoSource = componentInfoSource;
  }

  public ApiComponentInfo isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * returns true if component is active.
   * @return isActive
  **/
  @ApiModelProperty(value = "returns true if component is active.")


  public Boolean isIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public ApiComponentInfo componentConfig(Map<String, String> componentConfig) {
    this.componentConfig = componentConfig;
    return this;
  }

  public ApiComponentInfo putComponentConfigItem(String key, String componentConfigItem) {
    if (this.componentConfig == null) {
      this.componentConfig = new HashMap<>();
    }
    this.componentConfig.put(key, componentConfigItem);
    return this;
  }

  /**
   * list of config name and value pair associated with component.
   * @return componentConfig
  **/
  @ApiModelProperty(value = "list of config name and value pair associated with component.")


  public Map<String, String> getComponentConfig() {
    return componentConfig;
  }

  public void setComponentConfig(Map<String, String> componentConfig) {
    this.componentConfig = componentConfig;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiComponentInfo apiComponentInfo = (ApiComponentInfo) o;
    return Objects.equals(this.name, apiComponentInfo.name) &&
        Objects.equals(this.cdhVersion, apiComponentInfo.cdhVersion) &&
        Objects.equals(this.cdhRelease, apiComponentInfo.cdhRelease) &&
        Objects.equals(this.componentVersion, apiComponentInfo.componentVersion) &&
        Objects.equals(this.componentRelease, apiComponentInfo.componentRelease) &&
        Objects.equals(this.componentInfoSource, apiComponentInfo.componentInfoSource) &&
        Objects.equals(this.isActive, apiComponentInfo.isActive) &&
        Objects.equals(this.componentConfig, apiComponentInfo.componentConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, cdhVersion, cdhRelease, componentVersion, componentRelease, componentInfoSource, isActive, componentConfig);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiComponentInfo {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    cdhVersion: ").append(toIndentedString(cdhVersion)).append("\n");
    sb.append("    cdhRelease: ").append(toIndentedString(cdhRelease)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    componentRelease: ").append(toIndentedString(componentRelease)).append("\n");
    sb.append("    componentInfoSource: ").append(toIndentedString(componentInfoSource)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    componentConfig: ").append(toIndentedString(componentConfig)).append("\n");
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

