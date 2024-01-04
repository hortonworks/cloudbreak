package com.sequenceiq.it.cloudbreak.dto;

import java.util.List;

import jakarta.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class ClouderaManagerTestDto extends AbstractCloudbreakTestDto<ClouderaManagerV4Request, Response, ClouderaManagerTestDto> {

    public ClouderaManagerTestDto(TestContext testContext) {
        super(new ClouderaManagerV4Request(), testContext);
    }

    public ClouderaManagerTestDto valid() {
        return withAutoTls(Boolean.FALSE);
    }

    public ClouderaManagerTestDto withAutoTls(Boolean autoTls) {
        getRequest().setEnableAutoTls(autoTls);
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
