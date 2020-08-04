package com.sequenceiq.it.cloudbreak.v4;

import java.util.Collections;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.DatalakeCluster;
import com.sequenceiq.it.cloudbreak.Entity;

public class DatalakeClusterV4Action {

    private DatalakeClusterV4Action() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity, String name) {
        DatalakeCluster datalake = (DatalakeCluster) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        datalake.setResponse(client.getCloudbreakClient().stackV4Endpoint().get(workspaceId, name, Collections.emptySet(),
                Crn.fromString(datalake.getResponse().getCrn()).getAccountId()));
    }

}
