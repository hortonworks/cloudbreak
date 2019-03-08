package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ClouderaManagerRepositoryEntity
        extends AbstractCloudbreakEntity<ClouderaManagerRepositoryV4Request, ClouderaManagerRepositoryV4Response, ClouderaManagerRepositoryEntity> {

    protected ClouderaManagerRepositoryEntity(TestContext testContext) {
        super(new ClouderaManagerRepositoryV4Request(), testContext);
    }

    @Override
    public CloudbreakEntity valid() {
        return withVersion("6.1.0");
    }

    public ClouderaManagerRepositoryEntity withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }
}
