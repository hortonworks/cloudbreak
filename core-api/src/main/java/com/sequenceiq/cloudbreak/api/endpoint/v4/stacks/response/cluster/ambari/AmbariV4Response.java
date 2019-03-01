package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.StackRepositoryV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterDefinitionModelDescription;
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

    @ApiModelProperty(ClusterModelDescription.CLUSTER_DEFINITION)
    private ClusterDefinitionV4Response clusterDefinition;

    @ApiModelProperty(StackModelDescription.AMBARI_IP)
    private String serverIp;

    @ApiModelProperty(StackModelDescription.AMBARI_URL)
    private String serverUrl;

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

    @ApiModelProperty(ClusterDefinitionModelDescription.CLUSTER_DEFINITION)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String extendedClusterDefinitionText;

    public ClusterDefinitionV4Response getClusterDefinition() {
        return clusterDefinition;
    }

    public void setClusterDefinition(ClusterDefinitionV4Response clusterDefinition) {
        this.clusterDefinition = clusterDefinition;
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

    public String getExtendedClusterDefinitionText() {
        return extendedClusterDefinitionText;
    }

    public void setExtendedClusterDefinitionText(String extendedClusterDefinitionText) {
        this.extendedClusterDefinitionText = extendedClusterDefinitionText;
    }
}
