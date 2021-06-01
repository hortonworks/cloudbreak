package com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AzureInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.YarnInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.VolumeV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class DistroXInstanceTemplateTestDto
        extends AbstractCloudbreakTestDto<InstanceTemplateV1Request, InstanceTemplateV4Response, DistroXInstanceTemplateTestDto> {

    public DistroXInstanceTemplateTestDto(TestContext testContext) {
        super(new InstanceTemplateV1Request(), testContext);
    }

    public DistroXInstanceTemplateTestDto valid() {
        return getCloudProvider().template(
                withRootVolume(getTestContext().given(DistroXRootVolumeTestDto.class))
                        .withAttachedVolume(getTestContext().init(DistroXVolumeTestDto.class)));
    }

    public DistroXInstanceTemplateTestDto withAws(AwsInstanceTemplateV1Parameters awsParameters) {
        getRequest().setAws(awsParameters);
        return this;
    }

    public DistroXInstanceTemplateTestDto withAzure(AzureInstanceTemplateV1Parameters azureParameters) {
        getRequest().setAzure(azureParameters);
        return this;
    }

    public DistroXInstanceTemplateTestDto withYarn(YarnInstanceTemplateV1Parameters yarnParameters) {
        getRequest().setYarn(yarnParameters);
        return this;
    }

    public DistroXInstanceTemplateTestDto withRootVolume(DistroXRootVolumeTestDto rootVolume) {
        getRequest().setRootVolume(rootVolume.getRequest());
        return this;
    }

    public DistroXInstanceTemplateTestDto withRootVolumeKey(String key) {
        getRequest().setRootVolume(getTestContext().get(key));
        return this;
    }

    public DistroXInstanceTemplateTestDto withAttachedVolume(DistroXVolumeTestDto... volumes) {
        getRequest().setAttachedVolumes(Stream.of(volumes).map(AbstractCloudbreakTestDto::getRequest).collect(Collectors.toSet()));
        return this;
    }

    public DistroXInstanceTemplateTestDto withAttachedVolumeKeys(String... keys) {
        getRequest().setAttachedVolumes(Stream.of(keys).map(key -> {
            DistroXVolumeTestDto value = getTestContext().get(key);
            return value.getRequest();
        }).collect(Collectors.toSet()));
        return this;
    }

    public DistroXInstanceTemplateTestDto withAttachedVolumeCount(int count) {
        Set<VolumeV1Request> volumes = new HashSet<>();
        for (int i = 0; i < count; i++) {
            volumes.add(getTestContext().given("attachedVolume" + i, DistroXVolumeTestDto.class).getRequest());
        }
        getRequest().setAttachedVolumes(volumes);
        return this;
    }

    public DistroXInstanceTemplateTestDto withAttachedVolumes(int count) {
        Optional<VolumeV1Request> first = getRequest().getAttachedVolumes().stream().findFirst();
        if (first.isEmpty()) {
            withAttachedVolumeCount(1);
        }
        first.get().setCount(count);
        return this;
    }

    public DistroXInstanceTemplateTestDto withInstanceType(String instanceType) {
        getRequest().setInstanceType(instanceType);
        return this;
    }
}
