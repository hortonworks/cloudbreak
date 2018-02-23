package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class IpPoolAction {

    private IpPoolAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        IpPool ipPool = (IpPool) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        ipPool.setResponse(client.getCloudbreakClient().connectorV1Endpoint().getIpPoolsCredentialId(ipPool.getRequest()));
    }
}
