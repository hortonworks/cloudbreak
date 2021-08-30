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
 * Arguments used to uninstall CDP a Private Cloud Control Plane
 */
@ApiModel(description = "Arguments used to uninstall CDP a Private Cloud Control Plane")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiUninstallControlPlaneArgs   {
  @JsonProperty("kubeConfig")
  private String kubeConfig = null;

  @JsonProperty("valuesYaml")
  private String valuesYaml = null;

  public ApiUninstallControlPlaneArgs kubeConfig(String kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }

  /**
   * The content of the kubeconfig file of the kubernetes environment on which the control plane is running Simplified example:<br> <br> apiVersion: v1<br> clusters:<br> - cluster:<br> &emsp;&emsp;certificate-authority-data: abc123<br> &emsp;&emsp;server: https://example-server.domain.com:6443<br> &emsp;name: example-cluster.domain.com:6443<br> contexts:<br> - context:<br> &emsp;&emsp;cluster: ocp-cluster1<br> &emsp;&emsp;user: admin<br> &emsp;name: admin<br> current-context: admin<br> kind: Config<br> preferences: {}<br> users:<br> - name: admin<br> &emsp;user:<br> &emsp;&emsp;client-certificate-data: abc123<br> &emsp;&emsp;client-key-data: xyz987<br> <br> For more information on the kubeconfig file, read the documentation <a target=\"_blank\" href=https://docs.cloudera.com/r/cdp-pvc-kubernetes>here</a>.
   * @return kubeConfig
  **/
  @ApiModelProperty(value = "The content of the kubeconfig file of the kubernetes environment on which the control plane is running Simplified example:<br> <br> apiVersion: v1<br> clusters:<br> - cluster:<br> &emsp;&emsp;certificate-authority-data: abc123<br> &emsp;&emsp;server: https://example-server.domain.com:6443<br> &emsp;name: example-cluster.domain.com:6443<br> contexts:<br> - context:<br> &emsp;&emsp;cluster: ocp-cluster1<br> &emsp;&emsp;user: admin<br> &emsp;name: admin<br> current-context: admin<br> kind: Config<br> preferences: {}<br> users:<br> - name: admin<br> &emsp;user:<br> &emsp;&emsp;client-certificate-data: abc123<br> &emsp;&emsp;client-key-data: xyz987<br> <br> For more information on the kubeconfig file, read the documentation <a target=\"_blank\" href=https://docs.cloudera.com/r/cdp-pvc-kubernetes>here</a>.")


  public String getKubeConfig() {
    return kubeConfig;
  }

  public void setKubeConfig(String kubeConfig) {
    this.kubeConfig = kubeConfig;
  }

  public ApiUninstallControlPlaneArgs valuesYaml(String valuesYaml) {
    this.valuesYaml = valuesYaml;
    return this;
  }

  /**
   * Currently, we only support the following content in the values.yaml: other: forceCleanup: true|false
   * @return valuesYaml
  **/
  @ApiModelProperty(value = "Currently, we only support the following content in the values.yaml: other: forceCleanup: true|false")


  public String getValuesYaml() {
    return valuesYaml;
  }

  public void setValuesYaml(String valuesYaml) {
    this.valuesYaml = valuesYaml;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiUninstallControlPlaneArgs apiUninstallControlPlaneArgs = (ApiUninstallControlPlaneArgs) o;
    return Objects.equals(this.kubeConfig, apiUninstallControlPlaneArgs.kubeConfig) &&
        Objects.equals(this.valuesYaml, apiUninstallControlPlaneArgs.valuesYaml);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kubeConfig, valuesYaml);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiUninstallControlPlaneArgs {\n");
    
    sb.append("    kubeConfig: ").append(toIndentedString(kubeConfig)).append("\n");
    sb.append("    valuesYaml: ").append(toIndentedString(valuesYaml)).append("\n");
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

