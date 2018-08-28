package com.sequenceiq.it.cloudbreak.newway.v3;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.IpPool;

public class IpPoolV3Action {
    private IpPoolV3Action() {

    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        IpPool ipPool = (IpPool) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        ipPool.setResponse(client.getCloudbreakClient().connectorV3Endpoint().getIpPoolsCredentialId(orgId, ipPool.getRequest()));
        logJSON("V3 Connectors ipPools get request: ", ipPool.getRequest());
    }
}
