package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.OpenStackInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class InstanceTemplateV4Entity extends AbstractCloudbreakEntity<InstanceTemplateV4Request, InstanceTemplateV4Response, InstanceTemplateV4Entity> {

    public InstanceTemplateV4Entity(InstanceTemplateV4Request request, TestContext testContext) {
        super(request, testContext);
    }

    public InstanceTemplateV4Entity(TestContext testContext) {
        super(new InstanceTemplateV4Request(), testContext);
    }

    public InstanceTemplateV4Entity() {
        super(InstanceTemplateV4Entity.class.getSimpleName().toUpperCase());
    }

    public InstanceTemplateV4Entity valid() {
        return withInstanceType("large")
                .witAttachedVolume(getTestContext().get(VolumeV4Entity.class));
    }

    public InstanceTemplateV4Entity withAwsParameters(AwsInstanceTemplateV4Parameters awsParameters) {
        getRequest().setAws(awsParameters);
        return this;
    }

    public InstanceTemplateV4Entity withGcpParameters(GcpInstanceTemplateV4Parameters gcpParameters) {
        getRequest().setGcp(gcpParameters);
        return this;
    }

    public InstanceTemplateV4Entity withAzureParameters(AzureInstanceTemplateV4Parameters azureParameters) {
        getRequest().setAzure(azureParameters);
        return this;
    }

    public InstanceTemplateV4Entity withOpenStackParameters(OpenStackInstanceTemplateV4Parameters openStackParameters) {
        getRequest().setOpenstack(openStackParameters);
        return this;
    }

    public InstanceTemplateV4Entity withRootVolumeKey(String key) {
        getRequest().setRootVolume(getTestContext().get(key));
        return this;
    }

    public InstanceTemplateV4Entity witAttachedVolume(VolumeV4Entity... volumes) {
        getRequest().setAttachedVolumes(Stream.of(volumes).map(AbstractCloudbreakEntity::getRequest).collect(Collectors.toSet()));
        return this;
    }

    public InstanceTemplateV4Entity witAttachedVolumeKeys(String... keys) {
        getRequest().setAttachedVolumes(Stream.of(keys).map(key -> {
            VolumeV4Entity value = getTestContext().get(key);
            return value.getRequest();
        }).collect(Collectors.toSet()));
        return this;
    }

    public InstanceTemplateV4Entity withAzure(AzureInstanceTemplateV4Parameters azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public InstanceTemplateV4Entity withGcp(GcpInstanceTemplateV4Parameters gcp) {
        getRequest().setGcp(gcp);
        return this;
    }

    public InstanceTemplateV4Entity withAws(AwsInstanceTemplateV4Parameters aws) {
        getRequest().setAws(aws);
        return this;
    }

    public InstanceTemplateV4Entity withOpenstack(OpenStackInstanceTemplateV4Parameters openstack) {
        getRequest().setOpenstack(openstack);
        return this;
    }

    public InstanceTemplateV4Entity withInstanceType(String instanceType) {
        getRequest().setInstanceType(instanceType);
        return this;
    }
}
