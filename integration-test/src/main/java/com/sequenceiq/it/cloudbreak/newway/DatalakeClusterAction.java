package com.sequenceiq.it.cloudbreak.newway;

import java.util.Collections;

import com.sequenceiq.it.IntegrationTestContext;

public class DatalakeClusterAction {

    private DatalakeClusterAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity, String name) {
        DatalakeCluster datalake = (DatalakeCluster) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        datalake.setResponse(client.getCloudbreakClient().stackV4Endpoint().get(client.getWorkspaceId(), name, Collections.emptySet()));
    }

}
