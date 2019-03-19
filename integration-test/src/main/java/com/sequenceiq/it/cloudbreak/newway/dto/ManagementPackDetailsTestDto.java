package com.sequenceiq.it.cloudbreak.newway.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ManagementPackDetailsTestDto extends AbstractCloudbreakTestDto<ManagementPackDetailsV4Request, ManagementPackDetailsV4Response,
        ManagementPackDetailsTestDto> {

    public ManagementPackDetailsTestDto(TestContext testContext) {
        super(new ManagementPackDetailsV4Request(), testContext);
    }

    public ManagementPackDetailsTestDto valid() {
        return this;
    }

    public ManagementPackDetailsTestDto withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public ManagementPackDetailsTestDto withPreInstalled(Boolean preInstalled) {
        getRequest().setPreInstalled(preInstalled);
        return this;
    }
}
