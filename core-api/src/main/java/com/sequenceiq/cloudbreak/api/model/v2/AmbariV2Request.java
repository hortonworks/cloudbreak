package com.sequenceiq.cloudbreak.api.model.v2;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AmbariV2Request implements JsonEntity {

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    private GatewayJson gateway;

    @ApiModelProperty(StackModelDescription.ENABLE_SECURITY)
    private Boolean enableSecurity = Boolean.FALSE;

    @Size(max = 15, min = 5, message = "The length of the username has to be in range of 5 to 15")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The username can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = StackModelDescription.USERNAME, required = true)
    private String userName;

    @NotNull
    @Size(max = 100, min = 5, message = "The length of the password has to be in range of 5 to 100")
    @ApiModelProperty(value = StackModelDescription.PASSWORD, required = true)
    private String password;

    @Valid
    private String kerberosConfigName;

    @ApiModelProperty(ClusterModelDescription.VALIDATE_BLUEPRINT)
    private Boolean validateBlueprint = Boolean.TRUE;

    @ApiModelProperty(ClusterModelDescription.VALIDATE_REPOSITORIES)
    private Boolean validateRepositories = Boolean.FALSE;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_STACK_DETAILS)
    private AmbariStackDetailsJson ambariStackDetails;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_REPO_DETAILS)
    private AmbariRepoDetailsJson ambariRepoDetailsJson;

    /**
     * @deprecated RdsConfig is replacing AmbariDatabaseDetailsJson
     */
    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_DATABASE_DETAILS)
    @Deprecated
    private AmbariDatabaseDetailsJson ambariDatabaseDetails;

    @ApiModelProperty(ClusterModelDescription.CONFIG_STRATEGY)
    private ConfigStrategy configStrategy = ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES;

    @ApiModelProperty(ClusterModelDescription.CONNECTED_CLUSTER)
    private ConnectedClusterRequest connectedCluster;

    @ApiModelProperty(ClusterModelDescription.AMBARI_SECURITY_MASTER_KEY)
    @Size(max = 100, min = 5, message = "The length of the password has to be in range of 5 to 100")
    private String ambariSecurityMasterKey;

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public GatewayJson getGateway() {
        return gateway;
    }

    public void setGateway(GatewayJson gateway) {
        this.gateway = gateway;
    }

    public Boolean getEnableSecurity() {
        return enableSecurity;
    }

    public void setEnableSecurity(Boolean enableSecurity) {
        this.enableSecurity = enableSecurity;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getKerberosConfigName() {
        return kerberosConfigName;
    }

    public void setKerberosConfigName(String kerberosConfigName) {
        this.kerberosConfigName = kerberosConfigName;
    }

    public Boolean getValidateBlueprint() {
        return validateBlueprint;
    }

    public void setValidateBlueprint(Boolean validateBlueprint) {
        this.validateBlueprint = validateBlueprint;
    }

    public Boolean getValidateRepositories() {
        return validateRepositories;
    }

    public void setValidateRepositories(Boolean validateRepositories) {
        this.validateRepositories = validateRepositories;
    }

    public AmbariStackDetailsJson getAmbariStackDetails() {
        return ambariStackDetails;
    }

    public void setAmbariStackDetails(AmbariStackDetailsJson ambariStackDetails) {
        this.ambariStackDetails = ambariStackDetails;
    }

    public AmbariRepoDetailsJson getAmbariRepoDetailsJson() {
        return ambariRepoDetailsJson;
    }

    public void setAmbariRepoDetailsJson(AmbariRepoDetailsJson ambariRepoDetailsJson) {
        this.ambariRepoDetailsJson = ambariRepoDetailsJson;
    }

    public AmbariDatabaseDetailsJson getAmbariDatabaseDetails() {
        return ambariDatabaseDetails;
    }

    public void setAmbariDatabaseDetails(AmbariDatabaseDetailsJson ambariDatabaseDetails) {
        this.ambariDatabaseDetails = ambariDatabaseDetails;
    }

    public ConfigStrategy getConfigStrategy() {
        return configStrategy;
    }

    public void setConfigStrategy(ConfigStrategy configStrategy) {
        this.configStrategy = configStrategy;
    }

    public ConnectedClusterRequest getConnectedCluster() {
        return connectedCluster;
    }

    public void setConnectedCluster(ConnectedClusterRequest connectedCluster) {
        this.connectedCluster = connectedCluster;
    }

    public String getAmbariSecurityMasterKey() {
        return ambariSecurityMasterKey;
    }

    public void setAmbariSecurityMasterKey(String ambariSecurityMasterKey) {
        this.ambariSecurityMasterKey = ambariSecurityMasterKey;
    }
}
