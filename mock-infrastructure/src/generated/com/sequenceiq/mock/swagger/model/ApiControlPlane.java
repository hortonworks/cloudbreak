package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A Control Plane in a Cloudera Manager instance
 */
@ApiModel(description = "A Control Plane in a Cloudera Manager instance")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiControlPlane   {
  @JsonProperty("namespace")
  private String namespace = null;

  @JsonProperty("dnsSuffix")
  private String dnsSuffix = null;

  @JsonProperty("uuid")
  private String uuid = null;

  @JsonProperty("remoteRepoUrl")
  private String remoteRepoUrl = null;

  @JsonProperty("version")
  private String version = null;

  @JsonProperty("manifest")
  private String manifest = null;

  @JsonProperty("valuesYaml")
  private String valuesYaml = null;

  @JsonProperty("kubernetesType")
  private String kubernetesType = null;

  public ApiControlPlane namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * The namespace where the control plane is installed. Append the domain to the namespace to get the url of the control plane.
   * @return namespace
  **/
  @ApiModelProperty(value = "The namespace where the control plane is installed. Append the domain to the namespace to get the url of the control plane.")


  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public ApiControlPlane dnsSuffix(String dnsSuffix) {
    this.dnsSuffix = dnsSuffix;
    return this;
  }

  /**
   * The domain where the control plane is installed. Append the domain to the namespace to get the url of the control plane.
   * @return dnsSuffix
  **/
  @ApiModelProperty(value = "The domain where the control plane is installed. Append the domain to the namespace to get the url of the control plane.")


  public String getDnsSuffix() {
    return dnsSuffix;
  }

  public void setDnsSuffix(String dnsSuffix) {
    this.dnsSuffix = dnsSuffix;
  }

  public ApiControlPlane uuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * The universally unique ID of this control plane in Cloudera Manager
   * @return uuid
  **/
  @ApiModelProperty(value = "The universally unique ID of this control plane in Cloudera Manager")


  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public ApiControlPlane remoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
    return this;
  }

  /**
   * The url of the remote repository where the artifacts used to install the control plane are hosted
   * @return remoteRepoUrl
  **/
  @ApiModelProperty(value = "The url of the remote repository where the artifacts used to install the control plane are hosted")


  public String getRemoteRepoUrl() {
    return remoteRepoUrl;
  }

  public void setRemoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
  }

  public ApiControlPlane version(String version) {
    this.version = version;
    return this;
  }

  /**
   * The CDP version of the control plane
   * @return version
  **/
  @ApiModelProperty(value = "The CDP version of the control plane")


  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public ApiControlPlane manifest(String manifest) {
    this.manifest = manifest;
    return this;
  }

  /**
   * The content of the manifest.json of the control plane
   * @return manifest
  **/
  @ApiModelProperty(value = "The content of the manifest.json of the control plane")


  public String getManifest() {
    return manifest;
  }

  public void setManifest(String manifest) {
    this.manifest = manifest;
  }

  public ApiControlPlane valuesYaml(String valuesYaml) {
    this.valuesYaml = valuesYaml;
    return this;
  }

  /**
   * The content of the values.yaml used to configure the control plane
   * @return valuesYaml
  **/
  @ApiModelProperty(value = "The content of the values.yaml used to configure the control plane")


  public String getValuesYaml() {
    return valuesYaml;
  }

  public void setValuesYaml(String valuesYaml) {
    this.valuesYaml = valuesYaml;
  }

  public ApiControlPlane kubernetesType(String kubernetesType) {
    this.kubernetesType = kubernetesType;
    return this;
  }

  /**
   * The kubernetes type on which the control plane is running
   * @return kubernetesType
  **/
  @ApiModelProperty(value = "The kubernetes type on which the control plane is running")


  public String getKubernetesType() {
    return kubernetesType;
  }

  public void setKubernetesType(String kubernetesType) {
    this.kubernetesType = kubernetesType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiControlPlane apiControlPlane = (ApiControlPlane) o;
    return Objects.equals(this.namespace, apiControlPlane.namespace) &&
        Objects.equals(this.dnsSuffix, apiControlPlane.dnsSuffix) &&
        Objects.equals(this.uuid, apiControlPlane.uuid) &&
        Objects.equals(this.remoteRepoUrl, apiControlPlane.remoteRepoUrl) &&
        Objects.equals(this.version, apiControlPlane.version) &&
        Objects.equals(this.manifest, apiControlPlane.manifest) &&
        Objects.equals(this.valuesYaml, apiControlPlane.valuesYaml) &&
        Objects.equals(this.kubernetesType, apiControlPlane.kubernetesType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, dnsSuffix, uuid, remoteRepoUrl, version, manifest, valuesYaml, kubernetesType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiControlPlane {\n");
    
    sb.append("    namespace: ").append(toIndentedString(namespace)).append("\n");
    sb.append("    dnsSuffix: ").append(toIndentedString(dnsSuffix)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    remoteRepoUrl: ").append(toIndentedString(remoteRepoUrl)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    manifest: ").append(toIndentedString(manifest)).append("\n");
    sb.append("    valuesYaml: ").append(toIndentedString(valuesYaml)).append("\n");
    sb.append("    kubernetesType: ").append(toIndentedString(kubernetesType)).append("\n");
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

