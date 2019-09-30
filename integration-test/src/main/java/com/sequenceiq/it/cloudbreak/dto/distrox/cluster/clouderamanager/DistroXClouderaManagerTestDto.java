package com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager;

import java.util.List;

import javax.ws.rs.core.Response;

import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.ClouderaManagerV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class DistroXClouderaManagerTestDto extends AbstractCloudbreakTestDto<ClouderaManagerV1Request, Response, DistroXClouderaManagerTestDto> {

    public DistroXClouderaManagerTestDto(TestContext testContex) {
        super(new ClouderaManagerV1Request(), testContex);
    }

    public DistroXClouderaManagerTestDto() {
        super(DistroXClouderaManagerTestDto.class.getSimpleName().toUpperCase());
    }

    public DistroXClouderaManagerTestDto valid() {
        withAutoTls(false);
        return this;
    }

    public DistroXClouderaManagerTestDto withClouderaManagerRepository(String key) {
        DistroXClouderaManagerRepositoryTestDto repositoryEntity = getTestContext().get(key);
        return withClouderaManagerRepository(repositoryEntity);
    }

    public DistroXClouderaManagerTestDto withClouderaManagerRepository(DistroXClouderaManagerRepositoryTestDto clouderaManagerRepositoryEntity) {
        getRequest().setRepository(clouderaManagerRepositoryEntity.getRequest());
        return this;
    }

    public DistroXClouderaManagerTestDto withAutoTls(boolean enabled) {
        getRequest().setEnableAutoTls(enabled);
        return this;
    }

    public DistroXClouderaManagerTestDto withClouderaManagerProduct(String key) {
        DistroXClouderaManagerProductTestDto repositoryEntity = getTestContext().get(key);
        return withClouderaManagerProduct(repositoryEntity);
    }

    public DistroXClouderaManagerTestDto withClouderaManagerProduct(DistroXClouderaManagerProductTestDto clouderaManagerProductEntity) {
        getRequest().setProducts(List.of(clouderaManagerProductEntity.getRequest()));
        return this;
    }
}
