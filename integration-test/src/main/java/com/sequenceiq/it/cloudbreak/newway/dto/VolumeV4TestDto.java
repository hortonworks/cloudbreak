package com.sequenceiq.it.cloudbreak.newway.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class VolumeV4TestDto extends AbstractCloudbreakTestDto<VolumeV4Request, VolumeV4Response, VolumeV4TestDto> {

    protected VolumeV4TestDto(TestContext testContext) {
        super(new VolumeV4Request(), testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return getCloudProvider().attachedVolume(this);
    }

    public VolumeV4TestDto withSize(int size) {
        getRequest().setSize(size);
        return this;
    }

    public VolumeV4TestDto withType(String type) {
        getRequest().setType(type);
        return this;
    }

    public VolumeV4TestDto withCount(int count) {
        getRequest().setCount(count);
        return this;
    }
}
