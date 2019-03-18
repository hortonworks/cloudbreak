package com.sequenceiq.it.cloudbreak.newway.entity;

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
public class NetworkV2Entity extends AbstractCloudbreakEntity<NetworkV4Request, NetworkV4Response, NetworkV2Entity> {
    public static final String NETWORK = "NETWORK";

    public NetworkV2Entity(NetworkV4Request request, TestContext testContext) {
        super(request, testContext);
    }

    public NetworkV2Entity(TestContext testContext) {
        super(new NetworkV4Request(), testContext);
    }

    public NetworkV2Entity() {
        super(NetworkV2Entity.class.getSimpleName().toUpperCase());
    }

    public NetworkV2Entity valid() {
        return getCloudProvider().network(this);
    }

    public NetworkV2Entity withAzure(AzureNetworkV4Parameters azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public NetworkV2Entity withAws(AwsNetworkV4Parameters aws) {
        getRequest().setAws(aws);
        return this;
    }

    public NetworkV2Entity withGcp(GcpNetworkV4Parameters gcp) {
        getRequest().setGcp(gcp);
        return this;
    }

    public NetworkV2Entity withOpenStack(OpenStackNetworkV4Parameters openStack) {
        getRequest().setOpenstack(openStack);
        return this;
    }

    public NetworkV2Entity withMock(MockNetworkV4Parameters param) {
        getRequest().setMock(param);
        return this;
    }

    public NetworkV2Entity withYarn(YarnNetworkV4Parameters yarn) {
        getRequest().setYarn(yarn);
        return this;
    }

    public NetworkV2Entity withSubnetCIDR(String subnetCIDR) {
        getRequest().setSubnetCIDR(subnetCIDR);
        return this;
    }
}
