package com.sequenceiq.it.cloudbreak.newway.v3;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.Entity;

public class IpPoolV4Action {
    private IpPoolV4Action() {

    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        throw new NotImplementedException("Should figure out how to obtain the following values: region, availabilityZone, credentialName, platformVariant");
        /*IpPool ipPool = (IpPool) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        ipPool.setResponse(client.getCloudbreakClient().connectorV4Endpoint().getIpPoolsCredentialId(workspaceId, ipPool.getRequest()));
        logJSON("V3 Connectors ipPools get request: ", ipPool.getRequest());*/
    }
}
