package com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.MockNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class DistroXNetworkTestDto extends AbstractCloudbreakTestDto<NetworkV1Request, NetworkV4Response, DistroXNetworkTestDto> {

    public DistroXNetworkTestDto(TestContext testContext) {
        super(new NetworkV1Request(), testContext);
    }

    public DistroXNetworkTestDto valid() {
        return getCloudProvider().network(this);
    }

    public DistroXNetworkTestDto withAzure(AzureNetworkV1Parameters azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public DistroXNetworkTestDto withAws(AwsNetworkV1Parameters aws) {
        getRequest().setAws(aws);
        return this;
    }

    public DistroXNetworkTestDto withMock(MockNetworkV1Parameters mock) {
        getRequest().setMock(mock);
        return this;
    }

}
