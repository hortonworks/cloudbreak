package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.YarnNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class NetworkV4TestDto extends AbstractCloudbreakTestDto<NetworkV4Request, NetworkV4Response, NetworkV4TestDto> {
    public static final String NETWORK = "NETWORK";

    public NetworkV4TestDto(NetworkV4Request request, TestContext testContext) {
        super(request, testContext);
    }

    public NetworkV4TestDto(TestContext testContext) {
        super(new NetworkV4Request(), testContext);
    }

    public NetworkV4TestDto() {
        super(NetworkV4TestDto.class.getSimpleName().toUpperCase());
    }

    public NetworkV4TestDto valid() {
        return getCloudProvider().network(this);
    }

    public NetworkV4TestDto withAzure(AzureNetworkV4Parameters azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public NetworkV4TestDto withAws(AwsNetworkV4Parameters aws) {
        getRequest().setAws(aws);
        return this;
    }

    public NetworkV4TestDto withGcp(GcpNetworkV4Parameters gcp) {
        getRequest().setGcp(gcp);
        return this;
    }

    public NetworkV4TestDto withMock(MockNetworkV4Parameters param) {
        getRequest().setMock(param);
        return this;
    }

    public NetworkV4TestDto withYarn(YarnNetworkV4Parameters yarn) {
        getRequest().setYarn(yarn);
        return this;
    }

    public NetworkV4TestDto withSubnetCIDR(String subnetCIDR) {
        getRequest().setSubnetCIDR(subnetCIDR);
        return this;
    }
}
