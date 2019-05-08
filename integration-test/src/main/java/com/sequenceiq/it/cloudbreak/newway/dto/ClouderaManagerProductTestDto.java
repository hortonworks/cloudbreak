package com.sequenceiq.it.cloudbreak.newway.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ClouderaManagerProductTestDto
        extends AbstractCloudbreakTestDto<ClouderaManagerProductV4Request, ClouderaManagerProductV4Response, ClouderaManagerProductTestDto> {

    public ClouderaManagerProductTestDto(TestContext testContext) {
        super(new ClouderaManagerProductV4Request(), testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return withVersion("6.2.0-1.cdh6.2.0.p0.967373");
    }

    public ClouderaManagerProductTestDto withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public ClouderaManagerProductTestDto withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }

    public ClouderaManagerProductTestDto withParcel(String parcel) {
        getRequest().setParcel(parcel);
        return this;
    }
}
