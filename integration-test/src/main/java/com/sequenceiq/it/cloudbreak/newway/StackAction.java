package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

import java.io.IOException;

public class StackAction {

    private StackAction() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get stack " + stackEntity.getName());
        stackEntity.setResponse(
                client.getCloudbreakClient().stackV2Endpoint()
                        .getPrivate(stackEntity.getName(), null));
        Log.logJSON(" stack get response: ", stackEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get all stack");
        stackEntity.setResponses(
                client.getCloudbreakClient()
                        .stackV2Endpoint()
                        .getPrivates());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" delete: " + stackEntity.getName());
        client.getCloudbreakClient().stackV2Endpoint()
                .deletePrivate(stackEntity.getName(), false, false);
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        try {
            get(integrationTestContext, entity);
        } catch (Exception e) {
            new StackPostStrategy().doAction(integrationTestContext, entity);
        }
    }
}
