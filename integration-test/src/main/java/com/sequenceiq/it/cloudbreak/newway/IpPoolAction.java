package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.it.IntegrationTestContext;

class IpPoolAction {
    private IpPoolAction() {

    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        IpPool ipPool = (IpPool) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        ipPool.setResponse(client.getCloudbreakClient().connectorV1Endpoint().getIpPoolsCredentialId(ipPool.getRequest()));
        logJSON("V1 Connectors ipPools get request: ", ipPool.getRequest());

    }
}
