package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class GatewayAction {

    private GatewayAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        Gateway gateway = (Gateway) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        gateway.setResponse(client.getCloudbreakClient().connectorV1Endpoint().getGatewaysCredentialId(gateway.getRequest()));
    }
}
