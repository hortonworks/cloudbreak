package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ManagementPackDetailsEntity extends AbstractCloudbreakEntity<ManagementPackDetailsV4Request, ManagementPackDetailsV4Response, ManagementPackDetailsEntity> {

    public ManagementPackDetailsEntity(TestContext testContext) {
        super(new ManagementPackDetailsV4Request(), testContext);
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
