package com.sequenceiq.it.cloudbreak.newway.v3;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.Entity;

public class NetworksV3Action {
    private NetworksV3Action() {
    }

    public static void getNetworks(IntegrationTestContext integrationTestContext, Entity entity) {
        throw new NotImplementedException("Should figure out how to obtain the following values: region, availabilityZone, credentialName, platformVariant");
        /*Networks networkEntity = (Networks) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        networkEntity.setResponse(
                client.getCloudbreakClient()
                        .connectorV4Endpoint()
                        .getCloudNetworks(workspaceId, networkEntity.getRequest()));
        logJSON("V3 Connectors networks post request: ", networkEntity.getRequest());*/
    }
}
