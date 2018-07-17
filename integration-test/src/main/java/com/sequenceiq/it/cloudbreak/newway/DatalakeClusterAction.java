package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import java.util.Collections;

public class DatalakeClusterAction {

    private DatalakeClusterAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity, String name) {
        DatalakeCluster datalake = (DatalakeCluster) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        datalake.setResponse(client.getCloudbreakClient().stackV2Endpoint().getPrivate(name, Collections.emptySet()));
    }

}
