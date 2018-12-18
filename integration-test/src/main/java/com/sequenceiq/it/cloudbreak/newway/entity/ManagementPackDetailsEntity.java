package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ManagementPackDetailsEntity extends AbstractCloudbreakEntity<ManagementPackDetails, ManagementPackDetails, ManagementPackDetailsEntity> {

    public ManagementPackDetailsEntity(TestContext testContext) {
        super(new ManagementPackDetails(), testContext);
    }

    public ManagementPackDetailsEntity valid() {
        return this;
    }

    public ManagementPackDetailsEntity withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public ManagementPackDetailsEntity withPreInstalled(Boolean preInstalled) {
        getRequest().setPreInstalled(preInstalled);
        return this;
    }
}
