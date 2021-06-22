package com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.RootVolumeV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.RootVolumeV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class DistroXRootVolumeTestDto extends AbstractCloudbreakTestDto<RootVolumeV1Request, RootVolumeV4Response, DistroXRootVolumeTestDto> {

    protected DistroXRootVolumeTestDto(TestContext testContext) {
        super(new RootVolumeV1Request(), testContext);
    }

    @Override
    public DistroXRootVolumeTestDto valid() {
        return getCloudProvider().distroXRootVolume(this);
    }

    public DistroXRootVolumeTestDto withSize(int size) {
        getRequest().setSize(size);
        return this;
    }
}
