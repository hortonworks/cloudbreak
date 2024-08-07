package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.RootVolumeV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class RootVolumeV4TestDto extends AbstractCloudbreakTestDto<RootVolumeV4Request, RootVolumeV4Response, RootVolumeV4TestDto> {

    protected RootVolumeV4TestDto(TestContext testContext) {
        super(new RootVolumeV4Request(), testContext);
    }

    @Override
    public RootVolumeV4TestDto valid() {
        return getCloudProvider().rootVolume(this);
    }

    public RootVolumeV4TestDto withSize(int size) {
        getRequest().setSize(size);
        return this;
    }
}
