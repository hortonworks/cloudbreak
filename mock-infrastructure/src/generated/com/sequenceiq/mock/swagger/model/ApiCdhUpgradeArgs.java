package com.sequenceiq.mock.swagger.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.mock.swagger.model.ApiRollingUpgradeClusterArgs;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Arguments used for the CDH Upgrade command.
 */
@ApiModel(description = "Arguments used for the CDH Upgrade command.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2021-12-10T21:24:30.629+01:00")




public class ApiCdhUpgradeArgs   {
  @JsonProperty("cdhParcelVersion")
  private String cdhParcelVersion = null;

  @JsonProperty("cdhPackageVersion")
  private String cdhPackageVersion = null;

  @JsonProperty("rollingRestartArgs")
  private ApiRollingUpgradeClusterArgs rollingRestartArgs = null;

  @JsonProperty("deployClientConfig")
  private Boolean deployClientConfig = null;

  @JsonProperty("startAllServices")
  private Boolean startAllServices = null;

  public ApiCdhUpgradeArgs cdhParcelVersion(String cdhParcelVersion) {
    this.cdhParcelVersion = cdhParcelVersion;
    return this;
  }

  /**
   * If using parcels, the full version of an already distributed parcel for the next major CDH version. Default is null, which indicates this is a package upgrade. Example versions are: '5.0.0-1.cdh5.0.0.p0.11' or '5.0.2-1.cdh5.0.2.p0.32'
   * @return cdhParcelVersion
  **/
  @ApiModelProperty(value = "If using parcels, the full version of an already distributed parcel for the next major CDH version. Default is null, which indicates this is a package upgrade. Example versions are: '5.0.0-1.cdh5.0.0.p0.11' or '5.0.2-1.cdh5.0.2.p0.32'")


  public String getCdhParcelVersion() {
    return cdhParcelVersion;
  }

  public void setCdhParcelVersion(String cdhParcelVersion) {
    this.cdhParcelVersion = cdhParcelVersion;
  }

  public ApiCdhUpgradeArgs cdhPackageVersion(String cdhPackageVersion) {
    this.cdhPackageVersion = cdhPackageVersion;
    return this;
  }

  /**
   * If using packages, the full version of the CDH packages being upgraded to, such as \"5.1.2\". These packages must already be installed on the cluster before running the upgrade command. For backwards compatibility, if \"5.0.0\" is specified here, then the upgrade command will relax validation of installed packages to match v6 behavior, only checking major version. <p> Introduced in v9. Has no effect in older API versions, which assume \"5.0.0\"
   * @return cdhPackageVersion
  **/
  @ApiModelProperty(value = "If using packages, the full version of the CDH packages being upgraded to, such as \"5.1.2\". These packages must already be installed on the cluster before running the upgrade command. For backwards compatibility, if \"5.0.0\" is specified here, then the upgrade command will relax validation of installed packages to match v6 behavior, only checking major version. <p> Introduced in v9. Has no effect in older API versions, which assume \"5.0.0\"")


  public String getCdhPackageVersion() {
    return cdhPackageVersion;
  }

  public void setCdhPackageVersion(String cdhPackageVersion) {
    this.cdhPackageVersion = cdhPackageVersion;
  }

  public ApiCdhUpgradeArgs rollingRestartArgs(ApiRollingUpgradeClusterArgs rollingRestartArgs) {
    this.rollingRestartArgs = rollingRestartArgs;
    return this;
  }

  /**
   * If provided and rolling restart is available, will perform rolling restart with the requested arguments. If provided and rolling restart is not available, errors. If omitted, will do a regular restart. <p> Introduced in v9. Has no effect in older API versions, which must always do a hard restart.
   * @return rollingRestartArgs
  **/
  @ApiModelProperty(value = "If provided and rolling restart is available, will perform rolling restart with the requested arguments. If provided and rolling restart is not available, errors. If omitted, will do a regular restart. <p> Introduced in v9. Has no effect in older API versions, which must always do a hard restart.")

  @Valid

  public ApiRollingUpgradeClusterArgs getRollingRestartArgs() {
    return rollingRestartArgs;
  }

  public void setRollingRestartArgs(ApiRollingUpgradeClusterArgs rollingRestartArgs) {
    this.rollingRestartArgs = rollingRestartArgs;
  }

  public ApiCdhUpgradeArgs deployClientConfig(Boolean deployClientConfig) {
    this.deployClientConfig = deployClientConfig;
    return this;
  }

  /**
   * Not used starting in v9 - Client config is always deployed as part of upgrade. For older versions, determines whether client configuration should be deployed as part of upgrade. Default is true.
   * @return deployClientConfig
  **/
  @ApiModelProperty(value = "Not used starting in v9 - Client config is always deployed as part of upgrade. For older versions, determines whether client configuration should be deployed as part of upgrade. Default is true.")


  public Boolean isDeployClientConfig() {
    return deployClientConfig;
  }

  public void setDeployClientConfig(Boolean deployClientConfig) {
    this.deployClientConfig = deployClientConfig;
  }

  public ApiCdhUpgradeArgs startAllServices(Boolean startAllServices) {
    this.startAllServices = startAllServices;
    return this;
  }

  /**
   * Not used starting in v9 - All servies are always started as part of upgrade. For older versions, determines whether all services should be started should be deployed as part of upgrade. Default is true.
   * @return startAllServices
  **/
  @ApiModelProperty(value = "Not used starting in v9 - All servies are always started as part of upgrade. For older versions, determines whether all services should be started should be deployed as part of upgrade. Default is true.")


  public Boolean isStartAllServices() {
    return startAllServices;
  }

  public void setStartAllServices(Boolean startAllServices) {
    this.startAllServices = startAllServices;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCdhUpgradeArgs apiCdhUpgradeArgs = (ApiCdhUpgradeArgs) o;
    return Objects.equals(this.cdhParcelVersion, apiCdhUpgradeArgs.cdhParcelVersion) &&
        Objects.equals(this.cdhPackageVersion, apiCdhUpgradeArgs.cdhPackageVersion) &&
        Objects.equals(this.rollingRestartArgs, apiCdhUpgradeArgs.rollingRestartArgs) &&
        Objects.equals(this.deployClientConfig, apiCdhUpgradeArgs.deployClientConfig) &&
        Objects.equals(this.startAllServices, apiCdhUpgradeArgs.startAllServices);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cdhParcelVersion, cdhPackageVersion, rollingRestartArgs, deployClientConfig, startAllServices);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCdhUpgradeArgs {\n");
    
    sb.append("    cdhParcelVersion: ").append(toIndentedString(cdhParcelVersion)).append("\n");
    sb.append("    cdhPackageVersion: ").append(toIndentedString(cdhPackageVersion)).append("\n");
    sb.append("    rollingRestartArgs: ").append(toIndentedString(rollingRestartArgs)).append("\n");
    sb.append("    deployClientConfig: ").append(toIndentedString(deployClientConfig)).append("\n");
    sb.append("    startAllServices: ").append(toIndentedString(startAllServices)).append("\n");
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

