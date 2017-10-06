package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterRequest implements JsonEntity {

    @Size(max = 40, min = 5, message = "The length of the cluster's name has to be in range of 5 to 40")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The name of the cluster can only contain lowercase alphanumeric characters and hyphens and has to start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT)
    private BlueprintRequest blueprint;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @Valid
    @ApiModelProperty(ClusterModelDescription.HOSTGROUPS)
    private Set<HostGroupRequest> hostGroups;

    @ApiModelProperty(ClusterModelDescription.EMAIL_NEEDED)
    private Boolean emailNeeded = Boolean.FALSE;

    @ApiModelProperty(ClusterModelDescription.EMAIL_TO)
    private String emailTo;

    private GatewayJson gateway;

    @ApiModelProperty(StackModelDescription.ENABLE_SECURITY)
    private Boolean enableSecurity = Boolean.FALSE;

    @Size(max = 15, min = 5, message = "The length of the username has to be in range of 5 to 15")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The username can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = StackModelDescription.USERNAME, required = true)
    private String userName;

    @NotNull
    @Size(max = 100, min = 5, message = "The length of the password has to be in range of 5 to 100")
    @ApiModelProperty(value = StackModelDescription.PASSWORD, required = true)
    private String password;

    private KerberosRequest kerberos;

    @ApiModelProperty(ClusterModelDescription.LDAP_CONFIG_ID)
    private Long ldapConfigId;

    @ApiModelProperty(ClusterModelDescription.LDAP_CONFIG)
    private LdapConfigRequest ldapConfig;

    @ApiModelProperty(ClusterModelDescription.VALIDATE_BLUEPRINT)
    private Boolean validateBlueprint = Boolean.TRUE;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_STACK_DETAILS)
    private AmbariStackDetailsJson ambariStackDetails;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_REPO_DETAILS)
    private AmbariRepoDetailsJson ambariRepoDetailsJson;

    @ApiModelProperty(ClusterModelDescription.RDSCONFIG_IDS)
    private Set<Long> rdsConfigIds = new HashSet<>();

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_DATABASE_DETAILS)
    private AmbariDatabaseDetailsJson ambariDatabaseDetails;

    @Valid
    @ApiModelProperty(ClusterModelDescription.RDS_CONFIGS)
    private Set<RDSConfigRequest> rdsConfigJsons = new HashSet<>();

    @Valid
    @ApiModelProperty(StackModelDescription.FILE_SYSTEM)
    private FileSystemRequest fileSystem;

    @ApiModelProperty(ClusterModelDescription.CONFIG_STRATEGY)
    private ConfigStrategy configStrategy = ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_INPUTS)
    private Set<BlueprintInputJson> blueprintInputs = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_CUSTOM_PROPERTIES)
    private String blueprintCustomProperties;

    @ApiModelProperty(ClusterModelDescription.CUSTOM_CONTAINERS)
    private CustomContainerRequest customContainer;

    @ApiModelProperty(ClusterModelDescription.CUSTOM_QUEUE)
    private String customQueue;

    @ApiModelProperty(ClusterModelDescription.EXECUTOR_TYPE)
    private ExecutorType executorType = ExecutorType.DEFAULT;

    @ApiModelProperty(ClusterModelDescription.CONNECTED_CLUSTER)
    private ConnectedClusterRequest connectedCluster;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public Set<HostGroupRequest> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupRequest> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public Boolean getEmailNeeded() {
        return emailNeeded;
    }

    public void setEmailNeeded(Boolean emailNeeded) {
        this.emailNeeded = emailNeeded;
    }

    public Boolean getEnableSecurity() {
        return enableSecurity;
    }

    public void setEnableSecurity(Boolean enableSecurity) {
        this.enableSecurity = enableSecurity;
    }

    public GatewayJson getGateway() {
        return gateway;
    }

    public void setGateway(GatewayJson gateway) {
        this.gateway = gateway;
    }

    public boolean getValidateBlueprint() {
        return validateBlueprint == null ? false : validateBlueprint;
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

    public Set<Long> getRdsConfigIds() {
        return rdsConfigIds;
    }

    public void setRdsConfigIds(Set<Long> rdsConfigIds) {
        this.rdsConfigIds = rdsConfigIds;
    }

    public Set<RDSConfigRequest> getRdsConfigJsons() {
        return rdsConfigJsons;
    }

    public void setRdsConfigJsons(Set<RDSConfigRequest> rdsConfigJsons) {
        this.rdsConfigJsons = rdsConfigJsons;
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

    public FileSystemRequest getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystemRequest fileSystem) {
        this.fileSystem = fileSystem;
    }

    public Long getLdapConfigId() {
        return ldapConfigId;
    }

    public void setLdapConfigId(Long ldapConfigId) {
        this.ldapConfigId = ldapConfigId;
    }

    public ConfigStrategy getConfigStrategy() {
        return configStrategy;
    }

    public void setConfigStrategy(ConfigStrategy configStrategy) {
        this.configStrategy = configStrategy;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
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

    public void setBlueprintCustomProperties(JsonNode blueprintCustomProperties) {
        this.blueprintCustomProperties = blueprintCustomProperties.toString();
    }

    public KerberosRequest getKerberos() {
        return kerberos;
    }

    public void setKerberos(KerberosRequest kerberos) {
        this.kerberos = kerberos;
    }

    public LdapConfigRequest getLdapConfig() {
        return ldapConfig;
    }

    public void setLdapConfig(LdapConfigRequest ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    public BlueprintRequest getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintRequest blueprint) {
        this.blueprint = blueprint;
    }

    public CustomContainerRequest getCustomContainer() {
        return customContainer;
    }

    public String getCustomQueue() {
        return customQueue;
    }

    public void setCustomQueue(String customQueue) {
        this.customQueue = customQueue;
    }

    public void setCustomContainer(CustomContainerRequest customContainer) {
        this.customContainer = customContainer;
    }

    public ConnectedClusterRequest getConnectedCluster() {
        return connectedCluster;
    }

    public void setConnectedCluster(ConnectedClusterRequest connectedCluster) {
        this.connectedCluster = connectedCluster;
    }

    public ExecutorType getExecutorType() {
        return executorType;
    }

    public void setExecutorType(ExecutorType executorType) {
        this.executorType = executorType;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }
}
