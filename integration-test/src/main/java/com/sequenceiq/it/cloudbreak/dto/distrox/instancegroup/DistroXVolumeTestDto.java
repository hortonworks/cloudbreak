package com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.VolumeV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;

@Prototype
public class DistroXVolumeTestDto extends AbstractCloudbreakTestDto<VolumeV1Request, VolumeV4Response, DistroXVolumeTestDto> {

    protected DistroXVolumeTestDto(TestContext testContext) {
        super(new VolumeV1Request(), testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return getCloudProvider().attachedVolume(this);
    }

    public DistroXVolumeTestDto withSize(int size) {
        getRequest().setSize(size);
        return this;
    }

    public DistroXVolumeTestDto withType(String type) {
        getRequest().setType(type);
        return this;
    }

    public DistroXVolumeTestDto withCount(int count) {
        getRequest().setCount(count);
        return this;
    }
}
