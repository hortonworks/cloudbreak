package com.sequenceiq.it.cloudbreak.newway.v4;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.Entity;

public class GatewayV4Action {

    private GatewayV4Action() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        throw new NotImplementedException("Should figure out how to obtain the following values: region, availabilityZone, credentialName, platformVariant");
        /*Gateway gateway = (Gateway) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        gateway.setResponse(client.getCloudbreakClient().connectorV4Endpoint().getGatewaysCredentialId(workspaceId, gateway.getRequest()));
        logJSON("V3 Connectors Gateways post request: ", gateway.getRequest());*/
    }
}
