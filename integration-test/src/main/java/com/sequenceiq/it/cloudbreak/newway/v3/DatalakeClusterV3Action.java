package com.sequenceiq.it.cloudbreak.newway.v3;

import java.util.Collections;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.DatalakeCluster;
import com.sequenceiq.it.cloudbreak.newway.Entity;

public class DatalakeClusterV3Action {

    private DatalakeClusterV3Action() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity, String name) {
        DatalakeCluster datalake = (DatalakeCluster) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        datalake.setResponse(client.getCloudbreakClient().stackV4Endpoint().get(workspaceId, name, Collections.emptySet()));
    }

}
