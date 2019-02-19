package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AmbariV4Request implements JsonEntity {

    @ApiModelProperty(ClusterModelDescription.CLUSTER_DEFINITION_NAME)
    private String clusterDefinitionName;

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

    @ApiModelProperty(ClusterModelDescription.VALIDATE_CLUSTER_DEFINITION)
    private Boolean validateClusterDefinition = Boolean.TRUE;

    @ApiModelProperty(ClusterModelDescription.VALIDATE_REPOSITORIES)
    private Boolean validateRepositories = Boolean.FALSE;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_STACK_DETAILS)
    private StackRepositoryV4Request stackRepository;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_REPO_DETAILS)
    private AmbariRepositoryV4Request repository;

    @ApiModelProperty(value = ClusterModelDescription.CONFIG_STRATEGY,
            allowableValues = "NEVER_APPLY,ONLY_STACK_DEFAULTS_APPLY,ALWAYS_APPLY,ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES")
    private ConfigStrategy configStrategy = ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES;

    @ApiModelProperty(ClusterModelDescription.AMBARI_SECURITY_MASTER_KEY)
    @Size(max = 100, min = 5, message = "The length of the password has to be in range of 5 to 100")
    private String securityMasterKey;

    public String getClusterDefinitionName() {
        return clusterDefinitionName;
    }

    public void setClusterDefinitionName(String clusterDefinitionName) {
        this.clusterDefinitionName = clusterDefinitionName;
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

    public Boolean getValidateClusterDefinition() {
        return validateClusterDefinition;
    }

    public void setValidateClusterDefinition(Boolean validateClusterDefinition) {
        this.validateClusterDefinition = validateClusterDefinition;
    }

    public Boolean getValidateRepositories() {
        return validateRepositories;
    }

    public void setValidateRepositories(Boolean validateRepositories) {
        this.validateRepositories = validateRepositories;
    }

    public StackRepositoryV4Request getStackRepository() {
        return stackRepository;
    }

    public void setStackRepository(StackRepositoryV4Request stackRepository) {
        this.stackRepository = stackRepository;
    }

    public AmbariRepositoryV4Request getRepository() {
        return repository;
    }

    public void setRepository(AmbariRepositoryV4Request repository) {
        this.repository = repository;
    }

    public ConfigStrategy getConfigStrategy() {
        return configStrategy;
    }

    public void setConfigStrategy(ConfigStrategy configStrategy) {
        this.configStrategy = configStrategy;
    }

    public String getSecurityMasterKey() {
        return securityMasterKey;
    }

    public void setSecurityMasterKey(String securityMasterKey) {
        this.securityMasterKey = securityMasterKey;
    }
}
