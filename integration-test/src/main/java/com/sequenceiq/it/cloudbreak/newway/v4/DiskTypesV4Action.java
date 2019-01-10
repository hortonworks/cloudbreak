package com.sequenceiq.it.cloudbreak.newway.v4;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.DiskTypesEntity;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class DiskTypesV4Action {

    private DiskTypesV4Action() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        DiskTypesEntity recommendationEntity = (DiskTypesEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get Disk Types to");
        recommendationEntity.setResponse(
                client.getCloudbreakClient()
                        .connectorV4Endpoint().getDisktypes(workspaceId));
        Log.logJSON(" get Disk Types response: ", recommendationEntity.getResponse());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        get(integrationTestContext, entity);
    }
}
