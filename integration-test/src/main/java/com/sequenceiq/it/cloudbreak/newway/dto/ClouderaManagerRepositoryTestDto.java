package com.sequenceiq.it.cloudbreak.newway.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ClouderaManagerRepositoryTestDto
        extends AbstractCloudbreakTestDto<ClouderaManagerRepositoryV4Request, ClouderaManagerRepositoryV4Response, ClouderaManagerRepositoryTestDto> {

    protected ClouderaManagerRepositoryTestDto(TestContext testContext) {
        super(new ClouderaManagerRepositoryV4Request(), testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return withVersion("6.1.0");
    }

    public ClouderaManagerRepositoryTestDto withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }
}
