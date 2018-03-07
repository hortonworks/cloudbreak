package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

import java.io.IOException;

public class DiskTypesAction {

    private DiskTypesAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        DiskTypesEntity recommendationEntity = (DiskTypesEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        Log.log(" get Disk Types to");
        recommendationEntity.setResponse(
                client.getCloudbreakClient()
                        .connectorV1Endpoint().getDisktypes());
        Log.logJSON(" get Disk Types response: ", recommendationEntity.getResponse());
    }

    public static void getByType(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        DiskTypesEntity recommendationEntity = (DiskTypesEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        Log.log(" get Disk Types by Type to");
        recommendationEntity.setByFilterResponses(
                client.getCloudbreakClient()
                        .connectorV1Endpoint().getDisktypeByType(recommendationEntity.getType()));
        Log.logJSON(" get Disk Types by Type response: ", recommendationEntity.getByFilterResponses());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        get(integrationTestContext, entity);
    }
}
