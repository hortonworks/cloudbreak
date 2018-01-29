package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public class StackAction {

    private StackAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        stackEntity.setResponse(
                client.getCloudbreakClient()
                        .stackV2Endpoint()
                        .postPrivate(stackEntity.getRequest()));
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        stackEntity.setResponse(
                client.getCloudbreakClient().stackV2Endpoint()
                        .getPrivate(stackEntity.getName(), null));
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
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
        client.getCloudbreakClient().stackV2Endpoint()
                .deletePrivate(stackEntity.getName(), false, true);
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        try {
            get(integrationTestContext, entity);
        } catch (Exception e) {
            post(integrationTestContext, entity);
        }
    }
}
