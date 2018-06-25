package com.sequenceiq.it.cloudbreak.newway;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

class BlueprintAction {

    private BlueprintAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        BlueprintEntity blueprintEntity = (BlueprintEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" post "
                .concat(blueprintEntity.getName())
                .concat(" private blueprint. "));
        blueprintEntity.setResponse(
                client.getCloudbreakClient()
                        .blueprintEndpoint()
                        .postPrivate(blueprintEntity.getRequest()));

        integrationTestContext.putCleanUpParam(blueprintEntity.getName(), blueprintEntity.getResponse().getId());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        BlueprintEntity blueprintEntity = (BlueprintEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get "
                .concat(blueprintEntity.getName())
                .concat(" private blueprint by Name. "));
        blueprintEntity.setResponse(
                client.getCloudbreakClient()
                        .blueprintEndpoint()
                        .getPrivate(blueprintEntity.getName()));
        Log.logJSON(" get "
                .concat(blueprintEntity.getName())
                .concat(" blueprint response: "),
                blueprintEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        BlueprintEntity blueprintEntity = (BlueprintEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get all private blueprints. ");
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
        Log.log(" delete "
                .concat(blueprintEntity.getName())
                .concat(" private blueprint with Name. "));

        Long id = integrationTestContext.getCleanUpParameter(blueprintEntity.getName(), Long.class);
        client.getCloudbreakClient().blueprintEndpoint().delete(id);
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
