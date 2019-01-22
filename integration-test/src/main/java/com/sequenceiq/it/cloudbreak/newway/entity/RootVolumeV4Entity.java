package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.RootVolumeV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class RootVolumeV4Entity extends AbstractCloudbreakEntity<RootVolumeV4Request, RootVolumeV4Response, RootVolumeV4Entity> {
    protected RootVolumeV4Entity(TestContext testContext) {
        super(new RootVolumeV4Request(), testContext);
    }

    public RootVolumeV4Entity withSize(int size) {
        getRequest().setSize(size);
        return this;
    }
}
