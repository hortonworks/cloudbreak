package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReinstallV4Request implements JsonEntity {

    @ApiModelProperty(StackModelDescription.INSTANCE_GROUPS)
    private Set<InstanceGroupV4Request> instanceGroups;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_STACK_DETAILS)
    private StackRepositoryV4Request stackRepository;

    @NotNull
    @ApiModelProperty(ClusterModelDescription.CLUSTER_DEFINITION_NAME)
    private String clusterDefinition;

    @ApiModelProperty(StackModelDescription.KERBEROS_PASSWORD)
    @Size(max = 50, min = 5, message = "The length of the Kerberos password has to be in range of 5 to 50")
    private String kerberosPassword;

    @ApiModelProperty(StackModelDescription.KERBEROS_PRINCIPAL)
    private String kerberosPrincipal;

    private Long stackId;

    public Set<InstanceGroupV4Request> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroupV4Request> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public StackRepositoryV4Request getStackRepository() {
        return stackRepository;
    }

    public void setStackRepository(StackRepositoryV4Request stackRepository) {
        this.stackRepository = stackRepository;
    }

    public String getClusterDefinition() {
        return clusterDefinition;
    }

    public void setClusterDefinition(String clusterDefinition) {
        this.clusterDefinition = clusterDefinition;
    }

    public String getKerberosPassword() {
        return kerberosPassword;
    }

    public void setKerberosPassword(String kerberosPassword) {
        this.kerberosPassword = kerberosPassword;
    }

    public String getKerberosPrincipal() {
        return kerberosPrincipal;
    }

    public void setKerberosPrincipal(String kerberosPrincipal) {
        this.kerberosPrincipal = kerberosPrincipal;
    }

    @JsonIgnore
    public Long getStackId() {
        return stackId;
    }

    @JsonIgnore
    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }
}
