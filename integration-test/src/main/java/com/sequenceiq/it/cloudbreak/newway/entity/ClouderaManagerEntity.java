package com.sequenceiq.it.cloudbreak.newway.entity;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ClouderaManagerEntity extends AbstractCloudbreakEntity<ClouderaManagerV4Request, Response, ClouderaManagerEntity> {

    public ClouderaManagerEntity(TestContext testContex) {
        super(new ClouderaManagerV4Request(), testContex);
    }

    public ClouderaManagerEntity() {
        super(ClouderaManagerEntity.class.getSimpleName().toUpperCase());
    }

    public ClouderaManagerEntity valid() {
        return this;
    }

    public ClouderaManagerEntity withClouderaManagerRepository(String key) {
        ClouderaManagerRepositoryEntity repositoryEntity = getTestContext().get(key);
        return withStackRepository(repositoryEntity);
    }

    public ClouderaManagerEntity withStackRepository(ClouderaManagerRepositoryEntity clouderaManagerRepositoryEntity) {
        getRequest().setRepository(clouderaManagerRepositoryEntity.getRequest());
        return this;
    }
}
