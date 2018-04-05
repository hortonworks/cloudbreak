package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

import java.io.IOException;

class NetworkAction {

    private NetworkAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        NetworkEntity networkEntity = (NetworkEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Log.log(" post private network. ");
        networkEntity.setResponse(client.getCloudbreakClient().networkEndpoint().postPrivate(networkEntity.getRequest()));
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        NetworkEntity networkEntity = (NetworkEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Log.log(" get private network. ");
        networkEntity.setResponse(client.getCloudbreakClient().networkEndpoint().getPrivate(networkEntity.getName()));
        Log.logJSON(" get network response: ", networkEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        NetworkEntity networkEntity = (NetworkEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Log.log(" get all private network. ");
        networkEntity.setResponses(client.getCloudbreakClient().networkEndpoint().getPrivates());
        Log.logJSON(" get network response: ", networkEntity.getResponse());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        NetworkEntity networkEntity = (NetworkEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Log.log(" delete private network. ");
        client.getCloudbreakClient().networkEndpoint().deletePrivate(networkEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        try {
            get(integrationTestContext, entity);
        } catch (Exception e) {
            post(integrationTestContext, entity);
        }
    }

    public static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        try {
            get(integrationTestContext, entity);
        } catch (Exception e) {
            post(integrationTestContext, entity);
            delete(integrationTestContext, entity);
        }
    }

}
