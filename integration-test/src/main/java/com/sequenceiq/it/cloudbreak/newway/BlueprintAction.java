package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

class BlueprintAction {

    private BlueprintAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        BlueprintEntity blueprintEntity = (BlueprintEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        blueprintEntity.setResponse(
                client.getCloudbreakClient()
                        .blueprintEndpoint()
                        .postPrivate(blueprintEntity.getRequest()));
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        BlueprintEntity blueprintEntity = (BlueprintEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        blueprintEntity.setResponse(
                client.getCloudbreakClient()
                        .blueprintEndpoint()
                        .getPrivate(blueprintEntity.getName()));
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        BlueprintEntity blueprintEntity = (BlueprintEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        blueprintEntity.setResponses(
                client.getCloudbreakClient()
                        .blueprintEndpoint()
                        .getPrivates());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        BlueprintEntity blueprintEntity = (BlueprintEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        client.getCloudbreakClient().blueprintEndpoint().deletePrivate(blueprintEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
