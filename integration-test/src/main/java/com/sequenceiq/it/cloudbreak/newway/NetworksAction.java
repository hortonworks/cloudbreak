package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.it.IntegrationTestContext;

class NetworksAction {

    private NetworksAction() {
    }

    static void getNetworks(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        Networks networkEntity = (Networks) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        networkEntity.setResponse(
                client.getCloudbreakClient()
                        .connectorV1Endpoint()
                        .getCloudNetworks(networkEntity.getRequest()));
        logJSON("V1 Connectors networks post request: ", networkEntity.getRequest());
    }
}