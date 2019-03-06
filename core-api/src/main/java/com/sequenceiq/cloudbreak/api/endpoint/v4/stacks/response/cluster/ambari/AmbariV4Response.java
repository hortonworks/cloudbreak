package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.StackRepositoryV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AmbariV4Response implements JsonEntity {

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
}
