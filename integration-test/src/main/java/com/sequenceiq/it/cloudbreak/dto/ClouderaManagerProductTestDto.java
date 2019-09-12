package com.sequenceiq.it.cloudbreak.dto;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class ClouderaManagerProductTestDto
        extends AbstractCloudbreakTestDto<ClouderaManagerProductV4Request, ClouderaManagerProductV4Response, ClouderaManagerProductTestDto> {

    public ClouderaManagerProductTestDto(TestContext testContext) {
        super(new ClouderaManagerProductV4Request(), testContext);
    }

    public CloudbreakTestDto valid() {
        return this;
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

    public ClouderaManagerProductTestDto withCsd(List<String> csd) {
        getRequest().setCsd(csd);
        return this;
    }
}
