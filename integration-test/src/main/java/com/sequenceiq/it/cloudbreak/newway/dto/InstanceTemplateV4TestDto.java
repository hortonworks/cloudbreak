package com.sequenceiq.it.cloudbreak.newway.dto;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.OpenStackInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class InstanceTemplateV4TestDto extends AbstractCloudbreakTestDto<InstanceTemplateV4Request, InstanceTemplateV4Response, InstanceTemplateV4TestDto> {

    public InstanceTemplateV4TestDto(InstanceTemplateV4Request request, TestContext testContext) {
        super(request, testContext);
    }

    public InstanceTemplateV4TestDto(TestContext testContext) {
        super(new InstanceTemplateV4Request(), testContext);
    }

    public InstanceTemplateV4TestDto() {
        super(InstanceTemplateV4TestDto.class.getSimpleName().toUpperCase());
    }

    public InstanceTemplateV4TestDto valid() {
        return getCloudProvider().template(withRootVolume(getTestContext().given(RootVolumeV4TestDto.class))
                .withAttachedVolume(getTestContext().init(VolumeV4TestDto.class)));
    }

    public InstanceTemplateV4TestDto withAttachedVolumes(Set<VolumeV4Request> volumes) {
        getRequest().setAttachedVolumes(volumes);
        return this;
    }

    public InstanceTemplateV4TestDto withAwsParameters(AwsInstanceTemplateV4Parameters awsParameters) {
        getRequest().setAws(awsParameters);
        return this;
    }

    public InstanceTemplateV4TestDto withGcpParameters(GcpInstanceTemplateV4Parameters gcpParameters) {
        getRequest().setGcp(gcpParameters);
        return this;
    }

    public InstanceTemplateV4TestDto withAzureParameters(AzureInstanceTemplateV4Parameters azureParameters) {
        getRequest().setAzure(azureParameters);
        return this;
    }

    public InstanceTemplateV4TestDto withOpenStackParameters(OpenStackInstanceTemplateV4Parameters openStackParameters) {
        getRequest().setOpenstack(openStackParameters);
        return this;
    }

    public InstanceTemplateV4TestDto withYarnParameters(YarnInstanceTemplateV4Parameters yarnParameters) {
        getRequest().setYarn(yarnParameters);
        return this;
    }

    public InstanceTemplateV4TestDto withRootVolume(RootVolumeV4TestDto rootVolume) {
        getRequest().setRootVolume(rootVolume.getRequest());
        return this;
    }

    public InstanceTemplateV4TestDto withRootVolumeKey(String key) {
        getRequest().setRootVolume(getTestContext().get(key));
        return this;
    }

    public InstanceTemplateV4TestDto withAttachedVolume(VolumeV4TestDto... volumes) {
        getRequest().setAttachedVolumes(Stream.of(volumes).map(AbstractCloudbreakTestDto::getRequest).collect(Collectors.toSet()));
        return this;
    }

    public InstanceTemplateV4TestDto withAttachedVolumeKeys(String... keys) {
        getRequest().setAttachedVolumes(Stream.of(keys).map(key -> {
            VolumeV4TestDto value = getTestContext().get(key);
            return value.getRequest();
        }).collect(Collectors.toSet()));
        return this;
    }

    public InstanceTemplateV4TestDto withAzure(AzureInstanceTemplateV4Parameters azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public InstanceTemplateV4TestDto withGcp(GcpInstanceTemplateV4Parameters gcp) {
        getRequest().setGcp(gcp);
        return this;
    }

    public InstanceTemplateV4TestDto withAws(AwsInstanceTemplateV4Parameters aws) {
        getRequest().setAws(aws);
        return this;
    }

    public InstanceTemplateV4TestDto withOpenstack(OpenStackInstanceTemplateV4Parameters openstack) {
        getRequest().setOpenstack(openstack);
        return this;
    }

    public InstanceTemplateV4TestDto withYarn(YarnInstanceTemplateV4Parameters yarn) {
        getRequest().setYarn(yarn);
        return this;
    }

    public InstanceTemplateV4TestDto withInstanceType(String instanceType) {
        getRequest().setInstanceType(instanceType);
        return this;
    }
}
