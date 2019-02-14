package com.sequenceiq.it.cloudbreak.newway;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

class ClusterDefinitionAction {

    private ClusterDefinitionAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterDefinitionEntity clusterDefinitionEntity = (ClusterDefinitionEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" post "
                .concat(clusterDefinitionEntity.getName())
                .concat(" private blueprint. "));
        clusterDefinitionEntity.setResponse(
                client.getCloudbreakClient()
                        .blueprintEndpoint()
                        .postPrivate(clusterDefinitionEntity.getRequest()));

        integrationTestContext.putCleanUpParam(clusterDefinitionEntity.getName(), clusterDefinitionEntity.getResponse().getId());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        ClusterDefinitionEntity clusterDefinitionEntity = (ClusterDefinitionEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get "
                .concat(clusterDefinitionEntity.getName())
                .concat(" private blueprint by Name. "));
        clusterDefinitionEntity.setResponse(
                client.getCloudbreakClient()
                        .blueprintEndpoint()
                        .getPrivate(clusterDefinitionEntity.getName()));
        Log.logJSON(" get "
                .concat(clusterDefinitionEntity.getName())
                .concat(" blueprint response: "),
                clusterDefinitionEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterDefinitionEntity clusterDefinitionEntity = (ClusterDefinitionEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get all private blueprints. ");
        clusterDefinitionEntity.setResponses(
                client.getCloudbreakClient()
                        .blueprintEndpoint()
                        .getPrivates());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterDefinitionEntity clusterDefinitionEntity = (ClusterDefinitionEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" delete "
                .concat(clusterDefinitionEntity.getName())
                .concat(" private blueprint with Name. "));

        Long id = integrationTestContext.getCleanUpParameter(clusterDefinitionEntity.getName(), Long.class);
        client.getCloudbreakClient().blueprintEndpoint().delete(id);
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
