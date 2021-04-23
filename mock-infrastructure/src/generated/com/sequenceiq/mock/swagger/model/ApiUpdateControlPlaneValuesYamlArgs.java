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
 * Arguments used to get an existing CDP Private Cloud Control Plane&#39;s info
 */
@ApiModel(description = "Arguments used to get an existing CDP Private Cloud Control Plane's info")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiUpdateControlPlaneValuesYamlArgs   {
  @JsonProperty("kubeConfig")
  private String kubeConfig = null;

  @JsonProperty("remoteRepoUrl")
  private String remoteRepoUrl = null;

  public ApiUpdateControlPlaneValuesYamlArgs kubeConfig(String kubeConfig) {
    this.kubeConfig = kubeConfig;
    return this;
  }

  /**
   * The content of the kubeconfig file of the kubernetes environment on which the control plane is running Simplified example:<br> <br> apiVersion: v1<br> clusters:<br> - cluster:<br> &emsp;&emsp;certificate-authority-data: abc123<br> &emsp;&emsp;server: https://example-server.domain.com:6443<br> &emsp;name: example-cluster.domain.com:6443<br> contexts:<br> - context:<br> &emsp;&emsp;cluster: ocp-cluster1<br> &emsp;&emsp;user: admin<br> &emsp;name: admin<br> current-context: admin<br> kind: Config<br> preferences: {}<br> users:<br> - name: admin<br> &emsp;user:<br> &emsp;&emsp;client-certificate-data: abc123<br> &emsp;&emsp;client-key-data: xyz987<br> <br> For more information on the kubeconfig file, read the documentation <a target=\"_blank\" href=http://tiny.cloudera.com/cdp-pvc.kubernetes>here</a>.
   * @return kubeConfig
  **/
  @ApiModelProperty(value = "The content of the kubeconfig file of the kubernetes environment on which the control plane is running Simplified example:<br> <br> apiVersion: v1<br> clusters:<br> - cluster:<br> &emsp;&emsp;certificate-authority-data: abc123<br> &emsp;&emsp;server: https://example-server.domain.com:6443<br> &emsp;name: example-cluster.domain.com:6443<br> contexts:<br> - context:<br> &emsp;&emsp;cluster: ocp-cluster1<br> &emsp;&emsp;user: admin<br> &emsp;name: admin<br> current-context: admin<br> kind: Config<br> preferences: {}<br> users:<br> - name: admin<br> &emsp;user:<br> &emsp;&emsp;client-certificate-data: abc123<br> &emsp;&emsp;client-key-data: xyz987<br> <br> For more information on the kubeconfig file, read the documentation <a target=\"_blank\" href=http://tiny.cloudera.com/cdp-pvc.kubernetes>here</a>.")


  public String getKubeConfig() {
    return kubeConfig;
  }

  public void setKubeConfig(String kubeConfig) {
    this.kubeConfig = kubeConfig;
  }

  public ApiUpdateControlPlaneValuesYamlArgs remoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
    return this;
  }

  /**
   * The url of the remote repository where the private cloud artifacts are hosted.
   * @return remoteRepoUrl
  **/
  @ApiModelProperty(value = "The url of the remote repository where the private cloud artifacts are hosted.")


  public String getRemoteRepoUrl() {
    return remoteRepoUrl;
  }

  public void setRemoteRepoUrl(String remoteRepoUrl) {
    this.remoteRepoUrl = remoteRepoUrl;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiUpdateControlPlaneValuesYamlArgs apiUpdateControlPlaneValuesYamlArgs = (ApiUpdateControlPlaneValuesYamlArgs) o;
    return Objects.equals(this.kubeConfig, apiUpdateControlPlaneValuesYamlArgs.kubeConfig) &&
        Objects.equals(this.remoteRepoUrl, apiUpdateControlPlaneValuesYamlArgs.remoteRepoUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kubeConfig, remoteRepoUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiUpdateControlPlaneValuesYamlArgs {\n");
    
    sb.append("    kubeConfig: ").append(toIndentedString(kubeConfig)).append("\n");
    sb.append("    remoteRepoUrl: ").append(toIndentedString(remoteRepoUrl)).append("\n");
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

