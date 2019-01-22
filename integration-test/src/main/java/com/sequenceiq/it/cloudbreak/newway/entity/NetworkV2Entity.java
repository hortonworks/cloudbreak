package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
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
        return this;
    }

    public NetworkV2Entity withAzure(AzureNetworkParametersV4 azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public NetworkV2Entity withSubnetCIDR(String subnetCIDR) {
        getRequest().setSubnetCIDR(subnetCIDR);
        return this;
    }
}
