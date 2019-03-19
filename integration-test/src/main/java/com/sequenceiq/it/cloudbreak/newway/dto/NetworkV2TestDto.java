package com.sequenceiq.it.cloudbreak.newway.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.OpenStackNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.YarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class NetworkV2TestDto extends AbstractCloudbreakTestDto<NetworkV4Request, NetworkV4Response, NetworkV2TestDto> {
    public static final String NETWORK = "NETWORK";

    public NetworkV2TestDto(NetworkV4Request request, TestContext testContext) {
        super(request, testContext);
    }

    public NetworkV2TestDto(TestContext testContext) {
        super(new NetworkV4Request(), testContext);
    }

    public NetworkV2TestDto() {
        super(NetworkV2TestDto.class.getSimpleName().toUpperCase());
    }

    public NetworkV2TestDto valid() {
        return getCloudProvider().network(this);
    }

    public NetworkV2TestDto withAzure(AzureNetworkV4Parameters azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public NetworkV2TestDto withAws(AwsNetworkV4Parameters aws) {
        getRequest().setAws(aws);
        return this;
    }

    public NetworkV2TestDto withGcp(GcpNetworkV4Parameters gcp) {
        getRequest().setGcp(gcp);
        return this;
    }

    public NetworkV2TestDto withOpenStack(OpenStackNetworkV4Parameters openStack) {
        getRequest().setOpenstack(openStack);
        return this;
    }

    public NetworkV2TestDto withMock(MockNetworkV4Parameters param) {
        getRequest().setMock(param);
        return this;
    }

    public NetworkV2TestDto withYarn(YarnNetworkV4Parameters yarn) {
        getRequest().setYarn(yarn);
        return this;
    }

    public NetworkV2TestDto withSubnetCIDR(String subnetCIDR) {
        getRequest().setSubnetCIDR(subnetCIDR);
        return this;
    }
}
