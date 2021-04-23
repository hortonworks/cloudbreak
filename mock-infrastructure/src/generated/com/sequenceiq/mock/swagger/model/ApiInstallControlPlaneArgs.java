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
 * Arguments used to install CDP a Private Cloud Control Plane
 */
@ApiModel(description = "Arguments used to install CDP a Private Cloud Control Plane")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiInstallControlPlaneArgs   {
  @JsonProperty("kubernetesType")
  private String kubernetesType = null;

  @JsonProperty("remoteRepoUrl")
  private String remoteRepoUrl = null;

  @JsonProperty("valuesYaml")
  private String valuesYaml = null;

  @JsonProperty("kubeConfig")
  private String kubeConfig = null;

  @JsonProperty("namespace")
  private String namespace = null;

  @JsonProperty("dockerRegistry")
  private String dockerRegistry = null;

  @JsonProperty("isOverrideAllowed")
  private Boolean isOverrideAllowed = null;

  public ApiInstallControlPlaneArgs kubernetesType(String kubernetesType) {
    this.kubernetesType = kubernetesType;
    return this;
  }

  /**
   * The kubernetes type (e.g. \"openshift\") that the control plane will run on
   * @return kubernetesType
  **/
  @ApiModelProperty(value = "The kubernetes type (e.g. \"openshift\") that the control plane will run on")


  public String getKubernetesType() {
    return kubernetesType;
  }

  public void setKubernetesType(String kubernetesType) {
    this.kubernetesType = kubernetesType;
  }

  public ApiInstallControlPlaneArgs remoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
    return this;
  }

  /**
   * The url of the remote repository where the private cloud artifacts to install are hosted
   * @return remoteRepoUrl
  **/
  @ApiModelProperty(value = "The url of the remote repository where the private cloud artifacts to install are hosted")


  public String getRemoteRepoUrl() {
    return remoteRepoUrl;
  }

  public void setRemoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
  }

  public ApiInstallControlPlaneArgs valuesYaml(String valuesYaml) {
    this.valuesYaml = valuesYaml;
    return this;
  }

  /**
   * A yaml file containing configuration parameters for the installation. To see sample values.yaml files, read the documentation <a target=\"_blank\"  href=http://tiny.cloudera.com/cdp-pvc.install-values-yaml>here</a>.
   * @return valuesYaml
  **/
  @ApiModelProperty(value = "A yaml file containing configuration parameters for the installation. To see sample values.yaml files, read the documentation <a target=\"_blank\"  href=http://tiny.cloudera.com/cdp-pvc.install-values-yaml>here</a>.")


  public String getValuesYaml() {
    return valuesYaml;
  }

  public void setValuesYaml(String valuesYaml) {
    this.valuesYaml = valuesYaml;
  }

  public ApiInstallControlPlaneArgs kubeConfig(String kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }

  /**
   * The content of the kubeconfig file of the kubernetes environment on which the install will be performed Simplified example:<br> <br> apiVersion: v1<br> clusters:<br> - cluster:<br> &emsp;&emsp;certificate-authority-data: abc123<br> &emsp;&emsp;server: https://example-server.domain.com:6443<br> &emsp;name: example-cluster.domain.com:6443<br> contexts:<br> - context:<br> &emsp;&emsp;cluster: ocp-cluster1<br> &emsp;&emsp;user: admin<br> &emsp;name: admin<br> current-context: admin<br> kind: Config<br> preferences: {}<br> users:<br> - name: admin<br> &emsp;user:<br> &emsp;&emsp;client-certificate-data: abc123<br> &emsp;&emsp;client-key-data: xyz987<br> <br> For more information on the kubeconfig file, read the documentation <a target=\"_blank\" href=http://tiny.cloudera.com/cdp-pvc.kubernetes>here</a>.
   * @return kubeConfig
  **/
  @ApiModelProperty(value = "The content of the kubeconfig file of the kubernetes environment on which the install will be performed Simplified example:<br> <br> apiVersion: v1<br> clusters:<br> - cluster:<br> &emsp;&emsp;certificate-authority-data: abc123<br> &emsp;&emsp;server: https://example-server.domain.com:6443<br> &emsp;name: example-cluster.domain.com:6443<br> contexts:<br> - context:<br> &emsp;&emsp;cluster: ocp-cluster1<br> &emsp;&emsp;user: admin<br> &emsp;name: admin<br> current-context: admin<br> kind: Config<br> preferences: {}<br> users:<br> - name: admin<br> &emsp;user:<br> &emsp;&emsp;client-certificate-data: abc123<br> &emsp;&emsp;client-key-data: xyz987<br> <br> For more information on the kubeconfig file, read the documentation <a target=\"_blank\" href=http://tiny.cloudera.com/cdp-pvc.kubernetes>here</a>.")


  public String getKubeConfig() {
    return kubeConfig;
  }

  public void setKubeConfig(String kubeConfig) {
    this.kubeConfig = kubeConfig;
  }

  public ApiInstallControlPlaneArgs namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * A unique namespace where the control plane will be installed
   * @return namespace
  **/
  @ApiModelProperty(value = "A unique namespace where the control plane will be installed")


  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public ApiInstallControlPlaneArgs dockerRegistry(String dockerRegistry) {
    this.dockerRegistry = dockerRegistry;
    return this;
  }

  /**
   * The url of the Docker Registry where images required for install are hosted. This fields is deprecated. The docker registry should be provided within the values.yaml configuration file.
   * @return dockerRegistry
  **/
  @ApiModelProperty(value = "The url of the Docker Registry where images required for install are hosted. This fields is deprecated. The docker registry should be provided within the values.yaml configuration file.")


  public String getDockerRegistry() {
    return dockerRegistry;
  }

  public void setDockerRegistry(String dockerRegistry) {
    this.dockerRegistry = dockerRegistry;
  }

  public ApiInstallControlPlaneArgs isOverrideAllowed(Boolean isOverrideAllowed) {
    this.isOverrideAllowed = isOverrideAllowed;
    return this;
  }

  /**
   * 
   * @return isOverrideAllowed
  **/
  @ApiModelProperty(value = "")


  public Boolean isIsOverrideAllowed() {
    return isOverrideAllowed;
  }

  public void setIsOverrideAllowed(Boolean isOverrideAllowed) {
    this.isOverrideAllowed = isOverrideAllowed;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiInstallControlPlaneArgs apiInstallControlPlaneArgs = (ApiInstallControlPlaneArgs) o;
    return Objects.equals(this.kubernetesType, apiInstallControlPlaneArgs.kubernetesType) &&
        Objects.equals(this.remoteRepoUrl, apiInstallControlPlaneArgs.remoteRepoUrl) &&
        Objects.equals(this.valuesYaml, apiInstallControlPlaneArgs.valuesYaml) &&
        Objects.equals(this.kubeConfig, apiInstallControlPlaneArgs.kubeConfig) &&
        Objects.equals(this.namespace, apiInstallControlPlaneArgs.namespace) &&
        Objects.equals(this.dockerRegistry, apiInstallControlPlaneArgs.dockerRegistry) &&
        Objects.equals(this.isOverrideAllowed, apiInstallControlPlaneArgs.isOverrideAllowed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kubernetesType, remoteRepoUrl, valuesYaml, kubeConfig, namespace, dockerRegistry, isOverrideAllowed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiInstallControlPlaneArgs {\n");
    
    sb.append("    kubernetesType: ").append(toIndentedString(kubernetesType)).append("\n");
    sb.append("    remoteRepoUrl: ").append(toIndentedString(remoteRepoUrl)).append("\n");
    sb.append("    valuesYaml: ").append(toIndentedString(valuesYaml)).append("\n");
    sb.append("    kubeConfig: ").append(toIndentedString(kubeConfig)).append("\n");
    sb.append("    namespace: ").append(toIndentedString(namespace)).append("\n");
    sb.append("    dockerRegistry: ").append(toIndentedString(dockerRegistry)).append("\n");
    sb.append("    isOverrideAllowed: ").append(toIndentedString(isOverrideAllowed)).append("\n");
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

