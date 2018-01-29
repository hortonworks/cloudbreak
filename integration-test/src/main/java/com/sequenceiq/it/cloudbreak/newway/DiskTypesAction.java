package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class DiskTypesAction {

    private DiskTypesAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        DiskTypesEntity recommendationEntity = (DiskTypesEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        recommendationEntity.setResponse(
                client.getCloudbreakClient()
                        .connectorV1Endpoint().getDisktypes());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        get(integrationTestContext, entity);
    }
}
