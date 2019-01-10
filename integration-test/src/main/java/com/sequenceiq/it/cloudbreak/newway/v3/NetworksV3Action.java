package com.sequenceiq.it.cloudbreak.newway.v3;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.Networks;

public class NetworksV3Action {
    private NetworksV3Action() {
    }

    public static void getNetworks(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        Networks networkEntity = (Networks) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        networkEntity.setResponse(
                client.getCloudbreakClient()
                        .connectorV4Endpoint()
                        .getCloudNetworks(workspaceId, networkEntity.getRequest()));
        logJSON("V3 Connectors networks post request: ", networkEntity.getRequest());
    }
}
