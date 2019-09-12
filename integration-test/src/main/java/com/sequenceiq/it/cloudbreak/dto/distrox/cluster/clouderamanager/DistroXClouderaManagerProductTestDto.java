package com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.product.ClouderaManagerProductV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;

@Prototype
public class DistroXClouderaManagerProductTestDto
        extends AbstractCloudbreakTestDto<ClouderaManagerProductV1Request, ClouderaManagerProductV4Response, DistroXClouderaManagerProductTestDto> {

    public DistroXClouderaManagerProductTestDto(TestContext testContext) {
        super(new ClouderaManagerProductV1Request(), testContext);
    }

    public CloudbreakTestDto valid() {
        return this;
    }

    public DistroXClouderaManagerProductTestDto withName(String name) {
        getRequest().setName(name);
        return this;
    }

    public DistroXClouderaManagerProductTestDto withVersion(String version) {
        getRequest().setVersion(version);
        return this;
    }

    public DistroXClouderaManagerProductTestDto withParcel(String parcel) {
        getRequest().setParcel(parcel);
        return this;
    }

    public DistroXClouderaManagerProductTestDto withCsd(List<String> csd) {
        getRequest().setCsd(csd);
        return this;
    }
}
