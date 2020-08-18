package com.sequenceiq.environment.api.v1.environment.model.request;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.openstack.OpenstackFreeIpaParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AttachedFreeIpaRequest")
public class AttachedFreeIpaRequest {

    @NotNull
    @ApiModelProperty(value = EnvironmentModelDescription.CREATE_FREEIPA, required = true)
    private Boolean create;

    @ApiModelProperty(value = EnvironmentModelDescription.FREEIPA_INSTANCE_COUNT_BY_GROUP)
    private Integer instanceCountByGroup;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_AWS_PARAMETERS)
    private AwsFreeIpaParameters aws;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_AZURE_PARAMETERS)
    private AzureFreeIpaParameters azure;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_GCP_PARAMETERS)
    private GcpFreeIpaParameters gcp;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.FREEIPA_OPENSTACK_PARAMETERS)
    private OpenstackFreeIpaParameters openstack;

    public Boolean getCreate() {
        return create;
    }

    public void setCreate(Boolean create) {
        this.create = create;
    }

    public Integer getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public void setInstanceCountByGroup(Integer instanceCountByGroup) {
        this.instanceCountByGroup = instanceCountByGroup;
    }

    public AwsFreeIpaParameters getAws() {
        return aws;
    }

    public void setAws(AwsFreeIpaParameters aws) {
        this.aws = aws;
    }

    @Override
    public String toString() {
        return "AttachedFreeIpaRequest{" +
                "create=" + create +
                ", instanceCountByGroup=" + instanceCountByGroup +
                ", aws=" + aws +
                '}';
    }

    public AzureFreeIpaParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureFreeIpaParameters azure) {
        this.azure = azure;
    }

    public GcpFreeIpaParameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpFreeIpaParameters gcp) {
        this.gcp = gcp;
    }

    public OpenstackFreeIpaParameters getOpenstack() {
        return openstack;
    }

    public void setOpenstack(OpenstackFreeIpaParameters openstack) {
        this.openstack = openstack;
    }
}
