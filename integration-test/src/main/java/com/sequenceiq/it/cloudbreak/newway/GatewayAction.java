package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.it.IntegrationTestContext;

public class GatewayAction {

    private GatewayAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        Gateway gateway = (Gateway) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        gateway.setResponse(client.getCloudbreakClient().connectorV1Endpoint().getGatewaysCredentialId(gateway.getRequest()));
        logJSON("V1 Connectors Gateways post request: ", gateway.getRequest());
    }
}
