package com.sequenceiq.it.cloudbreak.newway.v4;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.ClusterTemplateUtil;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class ClusterTemplateV4Action {

    private ClusterTemplateV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterTemplateV4TestDto clusterTemplateV4Entity = (ClusterTemplateV4TestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(String.format(" post %s cluster template. ", clusterTemplateV4Entity.getName()));
        clusterTemplateV4Entity.setResponse(
                client.getCloudbreakClient()
                        .clusterTemplateV4EndPoint()
                        .post(workspaceId, clusterTemplateV4Entity.getRequest()));

        integrationTestContext.putCleanUpParam(clusterTemplateV4Entity.getName(), clusterTemplateV4Entity.getResponse().getId());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        ClusterTemplateV4TestDto clusterTemplateV4Entity = (ClusterTemplateV4TestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(String.format(" get %s cluster template by Name. ", clusterTemplateV4Entity.getName()));
        clusterTemplateV4Entity.setResponse(
                client.getCloudbreakClient()
                        .clusterTemplateV4EndPoint()
                        .get(workspaceId, clusterTemplateV4Entity.getName()));
        Log.logJSON(String.format(" get %s cluster template response: ", clusterTemplateV4Entity.getName()),
                new Object[]{clusterTemplateV4Entity.getResponse()});
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterTemplateV4TestDto clusterTemplateV4Entity = (ClusterTemplateV4TestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get all cluster templates. ");
        clusterTemplateV4Entity.setResponses(
                ClusterTemplateUtil.getResponseFromViews(client.getCloudbreakClient()
                        .clusterTemplateV4EndPoint()
                        .list(workspaceId).getResponses()));
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        ClusterTemplateV4TestDto clusterTemplateV4Entity = (ClusterTemplateV4TestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(String.format(" delete %s cluster template with Name. ", clusterTemplateV4Entity.getName()));

        client.getCloudbreakClient().clusterTemplateV4EndPoint().delete(workspaceId, clusterTemplateV4Entity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        post(integrationTestContext, entity);
    }
}
