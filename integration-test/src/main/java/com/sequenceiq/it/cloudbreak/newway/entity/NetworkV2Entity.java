package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class NetworkV2Entity extends AbstractCloudbreakEntity<NetworkV2Request, NetworkResponse, NetworkV2Entity> {
    public static final String NETWORK = "NETWORK";

    public NetworkV2Entity(NetworkV2Request request, TestContext testContext) {
        super(request, testContext);
    }

    public NetworkV2Entity(TestContext testContext) {
        super(new NetworkV2Request(), testContext);
    }

    public NetworkV2Entity() {
        super(NetworkV2Entity.class.getSimpleName().toUpperCase());
    }

    public NetworkV2Entity valid() {
        return this;
    }

    public NetworkV2Entity withParameters(Map<String, Object> parameters) {
        getRequest().setParameters(parameters);
        return this;
    }

    public NetworkV2Entity withSubnetCIDR(String subnetCIDR) {
        getRequest().setSubnetCIDR(subnetCIDR);
        return this;
    }
}
