package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.GcpStackParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.OpenStackStackParametersV4;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class StackV4Base extends ProviderParametersBase implements JsonEntity {

    @Size(max = 40, min = 5, message = "The length of the name has to be in range of 5 to 40")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @ApiModelProperty(StackModelDescription.AWS_PARAMETERS)
    private AwsStackParametersV4 aws;

    @ApiModelProperty(StackModelDescription.GCP_PARAMETERS)
    private GcpStackParametersV4 gcp;

    @ApiModelProperty(StackModelDescription.AZURE_PARAMETERS)
    private AzureStackParametersV4 azure;

    @ApiModelProperty(StackModelDescription.OPENSTACK_PARAMETERS)
    private OpenStackStackParametersV4 openstack;

    @ApiModelProperty
    private Long timeToLive;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AwsStackParametersV4 getAws() {
        return aws;
    }

    public void setAws(AwsStackParametersV4 aws) {
        this.aws = aws;
    }

    public GcpStackParametersV4 getGcp() {
        return gcp;
    }

    public void setGcp(GcpStackParametersV4 gcp) {
        this.gcp = gcp;
    }

    public AzureStackParametersV4 getAzure() {
        return azure;
    }

    public void setAzure(AzureStackParametersV4 azure) {
        this.azure = azure;
    }

    public OpenStackStackParametersV4 getOpenstack() {
        return openstack;
    }

    public void setOpenstack(OpenStackStackParametersV4 openstack) {
        this.openstack = openstack;
    }
}
