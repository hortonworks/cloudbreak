package com.sequenceiq.it.cloudbreak;

import java.util.Collections;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.IntegrationTestContext;

public class DatalakeClusterAction {

    private DatalakeClusterAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity, String name) {
        DatalakeCluster datalake = (DatalakeCluster) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        datalake.setResponse(client.getCloudbreakClient().stackV4Endpoint().get(client.getWorkspaceId(), name, Collections.emptySet(),
                Crn.fromString(datalake.getResponse().getCrn()).getAccountId()));
    }

}
