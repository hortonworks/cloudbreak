package com.sequenceiq.it.cloudbreak.newway.v4;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4ViewResponse;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class ClusterDefinitionV4Action {

    private ClusterDefinitionV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterDefinitionEntity clusterDefinitionEntity = (ClusterDefinitionEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" post "
                .concat(clusterDefinitionEntity.getName())
                .concat(" private cluster definition. "));
        clusterDefinitionEntity.setResponse(
                client.getCloudbreakClient()
                        .clusterDefinitionV4Endpoint()
                        .post(workspaceId, clusterDefinitionEntity.getRequest()));

        integrationTestContext.putCleanUpParam(clusterDefinitionEntity.getName(), clusterDefinitionEntity.getResponse().getId());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        ClusterDefinitionEntity clusterDefinitionEntity = (ClusterDefinitionEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get "
                .concat(clusterDefinitionEntity.getName())
                .concat(" private cluster definition by Name. "));
        clusterDefinitionEntity.setResponse(
                client.getCloudbreakClient()
                        .clusterDefinitionV4Endpoint().get(workspaceId, clusterDefinitionEntity.getName()));
        Log.logJSON(" get "
                .concat(clusterDefinitionEntity.getName())
                .concat(" cluster definition response: "),
                clusterDefinitionEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterDefinitionEntity clusterDefinitionEntity = (ClusterDefinitionEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get all private cluster definitions. ");
        Collection<ClusterDefinitionV4ViewResponse> clusterDefinitions = client.getCloudbreakClient().clusterDefinitionV4Endpoint()
                .list(workspaceId).getResponses();
        Set<ClusterDefinitionV4Response> detailedClusterDefinitions = clusterDefinitions.stream()
                .map(bp -> client.getCloudbreakClient().clusterDefinitionV4Endpoint()
                .get(workspaceId, bp.getName())).collect(Collectors.toSet());
        clusterDefinitionEntity.setResponses(detailedClusterDefinitions);
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterDefinitionEntity clusterDefinitionEntity = (ClusterDefinitionEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" delete "
                .concat(clusterDefinitionEntity.getName())
                .concat(" private cluster definition with Name. "));
        client.getCloudbreakClient().clusterDefinitionV4Endpoint().delete(workspaceId, clusterDefinitionEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
