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
 * Arguments used for the command to generate the external vault setup template
 */
@ApiModel(description = "Arguments used for the command to generate the external vault setup template")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-04-23T12:05:48.864+02:00")




public class ApiGenerateExternalVaultSetupArgs   {
  @JsonProperty("remoteRepoUrl")
  private String remoteRepoUrl = null;

  @JsonProperty("namespace")
  private String namespace = null;

  @JsonProperty("vaultAddr")
  private String vaultAddr = null;

  public ApiGenerateExternalVaultSetupArgs remoteRepoUrl(String remoteRepoUrl) {
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

  public ApiGenerateExternalVaultSetupArgs namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * The namespace of the CDP Private control plane
   * @return namespace
  **/
  @ApiModelProperty(value = "The namespace of the CDP Private control plane")


  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public ApiGenerateExternalVaultSetupArgs vaultAddr(String vaultAddr) {
    this.vaultAddr = vaultAddr;
    return this;
  }

  /**
   * Optional. The address the of external vault
   * @return vaultAddr
  **/
  @ApiModelProperty(value = "Optional. The address the of external vault")


  public String getVaultAddr() {
    return vaultAddr;
  }

  public void setVaultAddr(String vaultAddr) {
    this.vaultAddr = vaultAddr;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiGenerateExternalVaultSetupArgs apiGenerateExternalVaultSetupArgs = (ApiGenerateExternalVaultSetupArgs) o;
    return Objects.equals(this.remoteRepoUrl, apiGenerateExternalVaultSetupArgs.remoteRepoUrl) &&
        Objects.equals(this.namespace, apiGenerateExternalVaultSetupArgs.namespace) &&
        Objects.equals(this.vaultAddr, apiGenerateExternalVaultSetupArgs.vaultAddr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(remoteRepoUrl, namespace, vaultAddr);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiGenerateExternalVaultSetupArgs {\n");
    
    sb.append("    remoteRepoUrl: ").append(toIndentedString(remoteRepoUrl)).append("\n");
    sb.append("    namespace: ").append(toIndentedString(namespace)).append("\n");
    sb.append("    vaultAddr: ").append(toIndentedString(vaultAddr)).append("\n");
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

