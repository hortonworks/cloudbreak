package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AwsInstanceGroupParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AzureInstanceGroupParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.GcpInstanceGroupParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.OpenStackInstanceGroupParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup.SecurityGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceGroupV4Response extends ProviderParametersBase implements JsonEntity {

    @ApiModelProperty(InstanceGroupModelDescription.AZURE_PARAMETERS)
    private AzureInstanceGroupParametersV4 azure;

    @ApiModelProperty(InstanceGroupModelDescription.GCP_PARAMETERS)
    private GcpInstanceGroupParametersV4 gcp;

    @ApiModelProperty(InstanceGroupModelDescription.AWS_PARAMETERS)
    private AwsInstanceGroupParametersV4 aws;

    @ApiModelProperty(InstanceGroupModelDescription.OPENSTACK_PARAMETERS)
    private OpenStackInstanceGroupParametersV4 openstack;

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(InstanceGroupModelDescription.METADATA)
    private Set<InstanceMetaDataV4Response> metadata = new HashSet<>();

    @ApiModelProperty(InstanceGroupModelDescription.TEMPLATE)
    private InstanceTemplateV4Response template;

    @ApiModelProperty(InstanceGroupModelDescription.SECURITYGROUP)
    private SecurityGroupV4Response securityGroup;

    @Override
    public AzureInstanceGroupParametersV4 getAzure() {
        return azure;
    }

    public void setAzure(AzureInstanceGroupParametersV4 azure) {
        this.azure = azure;
    }

    @Override
    public GcpInstanceGroupParametersV4 getGcp() {
        return gcp;
    }

    public void setGcp(GcpInstanceGroupParametersV4 gcp) {
        this.gcp = gcp;
    }

    @Override
    public AwsInstanceGroupParametersV4 getAws() {
        return aws;
    }

    public void setAws(AwsInstanceGroupParametersV4 aws) {
        this.aws = aws;
    }

    @Override
    public OpenStackInstanceGroupParametersV4 getOpenstack() {
        return openstack;
    }

    public void setOpenstack(OpenStackInstanceGroupParametersV4 openstack) {
        this.openstack = openstack;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<InstanceMetaDataV4Response> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<InstanceMetaDataV4Response> metadata) {
        this.metadata = metadata;
    }

    public InstanceTemplateV4Response getTemplate() {
        return template;
    }

    public void setTemplate(InstanceTemplateV4Response template) {
        this.template = template;
    }

    public SecurityGroupV4Response getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupV4Response securityGroup) {
        this.securityGroup = securityGroup;
    }
}
