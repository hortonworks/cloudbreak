package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.StackRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AmbariV4Response implements JsonEntity {

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT)
    private BlueprintV4Response blueprint;

    @ApiModelProperty(StackModelDescription.AMBARI_IP)
    private String serverIp;

    @ApiModelProperty(StackModelDescription.AMBARI_URL)
    private String serverUrl;

    @NotNull
    @ApiModelProperty(value = StackModelDescription.USERNAME, required = true)
    private SecretV4Response userName;

    @NotNull
    @ApiModelProperty(value = StackModelDescription.PASSWORD, required = true)
    private SecretV4Response password;

    @ApiModelProperty(StackModelDescription.DP_AMBARI_USERNAME)
    private SecretV4Response dpUser;

    @ApiModelProperty(StackModelDescription.DP_AMBARI_PASSWORD)
    private SecretV4Response dpPassword;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_STACK_DETAILS)
    private StackRepositoryV4Response stackRepository;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_REPO_DETAILS)
    private AmbariRepositoryV4Response repository;

    @ApiModelProperty(ClusterModelDescription.CONFIG_STRATEGY)
    private ConfigStrategy configStrategy = ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES;

    @ApiModelProperty(ClusterModelDescription.AMBARI_SECURITY_MASTER_KEY)
    private SecretV4Response securityMasterKey;

    @ApiModelProperty(BlueprintModelDescription.AMBARI_BLUEPRINT)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String extendedBlueprintText;

    public BlueprintV4Response getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintV4Response blueprint) {
        this.blueprint = blueprint;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public SecretV4Response getUserName() {
        return userName;
    }

    public void setUserName(SecretV4Response userName) {
        this.userName = userName;
    }

    public SecretV4Response getPassword() {
        return password;
    }

    public void setPassword(SecretV4Response password) {
        this.password = password;
    }

    public SecretV4Response getDpUser() {
        return dpUser;
    }

    public void setDpUser(SecretV4Response dpUser) {
        this.dpUser = dpUser;
    }

    public SecretV4Response getDpPassword() {
        return dpPassword;
    }

    public void setDpPassword(SecretV4Response dpPassword) {
        this.dpPassword = dpPassword;
    }

    public StackRepositoryV4Response getStackRepository() {
        return stackRepository;
    }

    public void setStackRepository(StackRepositoryV4Response stackRepository) {
        this.stackRepository = stackRepository;
    }

    public AmbariRepositoryV4Response getRepository() {
        return repository;
    }

    public void setRepository(AmbariRepositoryV4Response repository) {
        this.repository = repository;
    }

    public ConfigStrategy getConfigStrategy() {
        return configStrategy;
    }

    public void setConfigStrategy(ConfigStrategy configStrategy) {
        this.configStrategy = configStrategy;
    }

    public SecretV4Response getSecurityMasterKey() {
        return securityMasterKey;
    }

    public void setSecurityMasterKey(SecretV4Response securityMasterKey) {
        this.securityMasterKey = securityMasterKey;
    }

    public String getExtendedBlueprintText() {
        return extendedBlueprintText;
    }

    public void setExtendedBlueprintText(String extendedBlueprintText) {
        this.extendedBlueprintText = extendedBlueprintText;
    }
}
