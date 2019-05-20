package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class ClouderaManagerRepositoryTestDto
        extends AbstractCloudbreakTestDto<ClouderaManagerRepositoryV4Request, ClouderaManagerRepositoryV4Response, ClouderaManagerRepositoryTestDto> {

    public ClouderaManagerRepositoryTestDto(TestContext testContext) {
        super(new ClouderaManagerRepositoryV4Request(), testContext);
    }

    public CloudbreakTestDto valid() {
        return this;
    }

    public ClouderaManagerRepositoryTestDto withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }

    public ClouderaManagerRepositoryTestDto withBaseUrl(String baseUrl) {
        getRequest().setBaseUrl(baseUrl);
        return this;
    }

    public ClouderaManagerRepositoryTestDto withGpgKeyUrl(String gpgKeyUrl) {
        getRequest().setGpgKeyUrl(gpgKeyUrl);
        return this;
    }
}
