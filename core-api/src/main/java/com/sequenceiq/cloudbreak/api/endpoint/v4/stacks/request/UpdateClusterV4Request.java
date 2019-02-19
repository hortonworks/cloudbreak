package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.annotations.TransformGetterType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateClusterV4Request implements JsonEntity {

    @ApiModelProperty(ClusterModelDescription.HOSTGROUP_ADJUSTMENT)
    private HostGroupAdjustmentV4Request hostGroupAdjustment;

    @ApiModelProperty(value = ClusterModelDescription.STATUS_REQUEST, allowableValues = "SYNC,FULL_SYNC,REPAIR_FAILED_NODES,STOPPED,STARTED")
    private StatusRequest status;

    @ApiModelProperty(ClusterModelDescription.USERNAME_PASSWORD)
    private UserNamePasswordV4Request userNamePassword;

    @ApiModelProperty(ClusterModelDescription.CLUSTER_DEFINITION_ID)
    private String clusterDefinitionName;

    @TransformGetterType
    @ApiModelProperty(ClusterModelDescription.VALIDATE_CLUSTER_DEFINITION)
    private Boolean validateClusterDefinition = Boolean.TRUE;

    @ApiModelProperty(ClusterModelDescription.HOSTGROUPS)
    private Set<HostGroupV4Request> hostgroups;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_STACK_DETAILS)
    private StackRepositoryV4Request stackRepository;

    @ApiModelProperty(StackModelDescription.KERBEROS_PASSWORD)
    @Size(max = 50, min = 5, message = "The length of the Kerberos password has to be in range of 5 to 50")
    private String kerberosPassword;

    @ApiModelProperty(StackModelDescription.KERBEROS_PRINCIPAL)
    private String kerberosPrincipal;

    public HostGroupAdjustmentV4Request getHostGroupAdjustment() {
        return hostGroupAdjustment;
    }

    public void setHostGroupAdjustment(HostGroupAdjustmentV4Request hostGroupAdjustment) {
        this.hostGroupAdjustment = hostGroupAdjustment;
    }

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public UserNamePasswordV4Request getUserNamePassword() {
        return userNamePassword;
    }

    public void setUserNamePassword(UserNamePasswordV4Request userNamePassword) {
        this.userNamePassword = userNamePassword;
    }

    public String getClusterDefinitionName() {
        return clusterDefinitionName;
    }

    public void setClusterDefinitionName(String clusterDefinitionName) {
        this.clusterDefinitionName = clusterDefinitionName;
    }

    public Boolean getValidateClusterDefinition() {
        return validateClusterDefinition;
    }

    public void setValidateClusterDefinition(Boolean validateClusterDefinition) {
        this.validateClusterDefinition = validateClusterDefinition;
    }

    public Set<HostGroupV4Request> getHostgroups() {
        return hostgroups;
    }

    public void setHostgroups(Set<HostGroupV4Request> hostgroups) {
        this.hostgroups = hostgroups;
    }

    public StackRepositoryV4Request getStackRepository() {
        return stackRepository;
    }

    public void setStackRepository(StackRepositoryV4Request stackRepository) {
        this.stackRepository = stackRepository;
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
}
