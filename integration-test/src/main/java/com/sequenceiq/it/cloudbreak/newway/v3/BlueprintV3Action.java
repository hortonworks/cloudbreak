package com.sequenceiq.it.cloudbreak.newway.v3;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.BlueprintViewResponse;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class BlueprintV3Action {

    private BlueprintV3Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        BlueprintEntity blueprintEntity = (BlueprintEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" post "
                .concat(blueprintEntity.getName())
                .concat(" private blueprint. "));
        blueprintEntity.setResponse(
                client.getCloudbreakClient()
                        .blueprintV3Endpoint()
                        .createInWorkspace(workspaceId, blueprintEntity.getRequest()));

        integrationTestContext.putCleanUpParam(blueprintEntity.getName(), blueprintEntity.getResponse().getId());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        BlueprintEntity blueprintEntity = (BlueprintEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get "
                .concat(blueprintEntity.getName())
                .concat(" private blueprint by Name. "));
        blueprintEntity.setResponse(
                client.getCloudbreakClient()
                        .blueprintV3Endpoint()
                        .getByNameInWorkspace(workspaceId, blueprintEntity.getName()));
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
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get all private blueprints. ");
        Set<BlueprintViewResponse> blueprints = client.getCloudbreakClient().blueprintV3Endpoint().listByWorkspace(workspaceId);
        Set<BlueprintResponse> detailedBlueprints = blueprints.stream().map(bp -> client.getCloudbreakClient().blueprintV3Endpoint()
                .getByNameInWorkspace(workspaceId, bp.getName())).collect(Collectors.toSet());
        blueprintEntity.setResponses(detailedBlueprints);
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        BlueprintEntity blueprintEntity = (BlueprintEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" delete "
                .concat(blueprintEntity.getName())
                .concat(" private blueprint with Name. "));
        client.getCloudbreakClient().blueprintV3Endpoint().deleteInWorkspace(workspaceId, blueprintEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
