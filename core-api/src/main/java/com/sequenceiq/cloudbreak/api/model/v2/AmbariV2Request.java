package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbariV2Request implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    private GatewayJson gateway;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.ENABLE_SECURITY)
    private Boolean enableSecurity = Boolean.FALSE;

    @Size(max = 15, min = 5, message = "The length of the username has to be in range of 5 to 15")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The username can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.USERNAME, required = true)
    private String userName;

    @NotNull
    @Size(max = 100, min = 5, message = "The length of the password has to be in range of 5 to 100")
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.PASSWORD, required = true)
    private String password;

    private KerberosRequest kerberos;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.VALIDATE_BLUEPRINT)
    private Boolean validateBlueprint = Boolean.TRUE;

    @Valid
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.AMBARI_STACK_DETAILS)
    private AmbariStackDetailsJson ambariStackDetails;

    @Valid
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.AMBARI_REPO_DETAILS)
    private AmbariRepoDetailsJson ambariRepoDetailsJson;

    @Valid
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.AMBARI_DATABASE_DETAILS)
    private AmbariDatabaseDetailsJson ambariDatabaseDetails;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.CONFIG_STRATEGY)
    private ConfigStrategy configStrategy = ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.BLUEPRINT_INPUTS)
    private Set<BlueprintInputJson> blueprintInputs = new HashSet<>();

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.BLUEPRINT_CUSTOM_PROPERTIES)
    private String blueprintCustomProperties;

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.CONNECTED_CLUSTER)
    private ConnectedClusterRequest connectedCluster;

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

    public KerberosRequest getKerberos() {
        return kerberos;
    }

    public void setKerberos(KerberosRequest kerberos) {
        this.kerberos = kerberos;
    }

    public Boolean getValidateBlueprint() {
        return validateBlueprint;
    }

    public void setValidateBlueprint(Boolean validateBlueprint) {
        this.validateBlueprint = validateBlueprint;
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

    public Set<BlueprintInputJson> getBlueprintInputs() {
        return blueprintInputs;
    }

    public void setBlueprintInputs(Set<BlueprintInputJson> blueprintInputs) {
        this.blueprintInputs = blueprintInputs;
    }

    @JsonRawValue
    public String getBlueprintCustomProperties() {
        return blueprintCustomProperties;
    }

    public void setBlueprintCustomProperties(String blueprintCustomProperties) {
        this.blueprintCustomProperties = blueprintCustomProperties;
    }

    public ConnectedClusterRequest getConnectedCluster() {
        return connectedCluster;
    }

    public void setConnectedCluster(ConnectedClusterRequest connectedCluster) {
        this.connectedCluster = connectedCluster;
    }
}
