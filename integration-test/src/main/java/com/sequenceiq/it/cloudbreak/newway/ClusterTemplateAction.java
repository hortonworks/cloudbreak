package com.sequenceiq.it.cloudbreak.newway;

import java.io.IOException;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

class ClusterTemplateAction {

    private ClusterTemplateAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterTemplateEntity clusterTemplateEntity = (ClusterTemplateEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(String.format(" post %s cluster template. ", clusterTemplateEntity.getName()));
        clusterTemplateEntity.setResponse(
                client.getCloudbreakClient()
                        .clusterTemplateV3EndPoint()
                        .createInWorkspace(workspaceId, clusterTemplateEntity.getRequest()));

        integrationTestContext.putCleanUpParam(clusterTemplateEntity.getName(), clusterTemplateEntity.getResponse().getId());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        ClusterTemplateEntity clusterTemplateEntity = (ClusterTemplateEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(String.format(" get %s cluster template by Name. ", clusterTemplateEntity.getName()));
        clusterTemplateEntity.setResponse(
                client.getCloudbreakClient()
                        .clusterTemplateV3EndPoint()
                        .getByNameInWorkspace(workspaceId, clusterTemplateEntity.getName()));
        Log.logJSON(String.format(" get %s cluster template response: ", clusterTemplateEntity.getName()), new Object[]{clusterTemplateEntity.getResponse()});
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterTemplateEntity clusterTemplateEntity = (ClusterTemplateEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get all cluster templates. ");
        Set<ClusterTemplateResponse> clusterTemplateResponses = ClusterTemplateUtil.getResponseFromViews(
                client.getCloudbreakClient()
                    .clusterTemplateV3EndPoint()
                    .listByWorkspace(workspaceId));
        clusterTemplateEntity.setResponses(clusterTemplateResponses);
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterTemplateEntity clusterTemplateEntity = (ClusterTemplateEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(String.format(" delete %s cluster template with Name. ", clusterTemplateEntity.getName()));

        client.getCloudbreakClient().clusterTemplateV3EndPoint().deleteInWorkspace(workspaceId, clusterTemplateEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
