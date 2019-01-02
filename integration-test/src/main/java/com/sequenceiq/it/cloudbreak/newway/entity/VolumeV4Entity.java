package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class VolumeV4Entity extends AbstractCloudbreakEntity<VolumeV4Request, VolumeV4Response, VolumeV4Entity> {

    protected VolumeV4Entity(TestContext testContext) {
        super(new VolumeV4Request(), testContext);
    }

    public VolumeV4Entity withSize(int size) {
        getRequest().setSize(size);
        return this;
    }

    public VolumeV4Entity withType(String type) {
        getRequest().setType(type);
        return this;
    }

    public VolumeV4Entity withCount(int count) {
        getRequest().setCount(count);
        return this;
    }
}
