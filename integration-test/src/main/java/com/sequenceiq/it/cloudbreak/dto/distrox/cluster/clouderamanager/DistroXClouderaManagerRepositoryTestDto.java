package com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.repository.ClouderaManagerRepositoryV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;

@Prototype
public class DistroXClouderaManagerRepositoryTestDto
        extends AbstractCloudbreakTestDto<ClouderaManagerRepositoryV1Request, ClouderaManagerRepositoryV4Response, DistroXClouderaManagerRepositoryTestDto> {

    public DistroXClouderaManagerRepositoryTestDto(TestContext testContext) {
        super(new ClouderaManagerRepositoryV1Request(), testContext);
    }

    public CloudbreakTestDto valid() {
        return this;
    }

    public DistroXClouderaManagerRepositoryTestDto withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }

    public DistroXClouderaManagerRepositoryTestDto withBaseUrl(String baseUrl) {
        getRequest().setBaseUrl(baseUrl);
        return this;
    }

    public DistroXClouderaManagerRepositoryTestDto withGpgKeyUrl(String gpgKeyUrl) {
        getRequest().setGpgKeyUrl(gpgKeyUrl);
        return this;
    }
}
