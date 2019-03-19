package com.sequenceiq.it.cloudbreak.newway.dto;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ClouderaManagerTestDto extends AbstractCloudbreakTestDto<ClouderaManagerV4Request, Response, ClouderaManagerTestDto> {

    public ClouderaManagerTestDto(TestContext testContex) {
        super(new ClouderaManagerV4Request(), testContex);
    }

    public ClouderaManagerTestDto() {
        super(ClouderaManagerTestDto.class.getSimpleName().toUpperCase());
    }

    public ClouderaManagerTestDto valid() {
        return this;
    }

    public ClouderaManagerTestDto withClouderaManagerRepository(String key) {
        ClouderaManagerRepositoryTestDto repositoryEntity = getTestContext().get(key);
        return withStackRepository(repositoryEntity);
    }

    public ClouderaManagerTestDto withStackRepository(ClouderaManagerRepositoryTestDto clouderaManagerRepositoryEntity) {
        getRequest().setRepository(clouderaManagerRepositoryEntity.getRequest());
        return this;
    }
}
