package com.sequenceiq.it.cloudbreak.newway.v3;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.DiskTypesEntity;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class DiskTypesV3Action {

    private DiskTypesV3Action() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        DiskTypesEntity recommendationEntity = (DiskTypesEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long orgId = integrationTestContext.getContextParam(CloudbreakTest.ORGANIZATION_ID, Long.class);
        Log.log(" get Disk Types to");
        recommendationEntity.setResponse(
                client.getCloudbreakClient()
                        .connectorV3Endpoint().getDisktypes(orgId));
        Log.logJSON(" get Disk Types response: ", recommendationEntity.getResponse());
    }

    public static void getByType(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        DiskTypesEntity recommendationEntity = (DiskTypesEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get Disk Types by Type to");
        // Using V1 endpoint, because the method "getDisktypeByType()" is not on V3
        recommendationEntity.setByFilterResponses(
                client.getCloudbreakClient()
                        .connectorV1Endpoint().getDisktypeByType(recommendationEntity.getType()));
        Log.logJSON(" get Disk Types by Type response: ", recommendationEntity.getByFilterResponses());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        get(integrationTestContext, entity);
    }
}
