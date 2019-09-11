package com.sequenceiq.it.cloudbreak.dto;

import java.util.List;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

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
        return withClouderaManagerRepository(repositoryEntity);
    }

    public ClouderaManagerTestDto withClouderaManagerRepository(ClouderaManagerRepositoryTestDto clouderaManagerRepositoryEntity) {
        getRequest().setRepository(clouderaManagerRepositoryEntity.getRequest());
        return this;
    }

    public ClouderaManagerTestDto withClouderaManagerProduct(String key) {
        ClouderaManagerProductTestDto repositoryEntity = getTestContext().get(key);
        return withClouderaManagerProduct(repositoryEntity);
    }

    public ClouderaManagerTestDto withClouderaManagerProduct(ClouderaManagerProductTestDto clouderaManagerProductEntity) {
        getRequest().setProducts(List.of(clouderaManagerProductEntity.getRequest()));
        return this;
    }
}
