package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;


public class ProxyConfigAction {
    private ProxyConfigAction() {
    }

    static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        ProxyConfigEntity proxyconfigEntity = (ProxyConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        proxyconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .proxyConfigEndpoint()
                        .postPrivate(proxyconfigEntity.getRequest()));
        logJSON("Proxy config post request: ", proxyconfigEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        ProxyConfigEntity proxyconfigEntity = (ProxyConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        proxyconfigEntity.setResponse(
                client.getCloudbreakClient()
                        .proxyConfigEndpoint()
                        .getPrivate(proxyconfigEntity.getEntityId()));
        logJSON(" get proxy config response: ", proxyconfigEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        ProxyConfigEntity proxyconfigEntity = (ProxyConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        proxyconfigEntity.setResponses(
                client.getCloudbreakClient()
                        .proxyConfigEndpoint()
                        .getPrivates());
        logJSON(" get all proxy config response: ", proxyconfigEntity.getResponse());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        ProxyConfigEntity proxyconfigEntity = (ProxyConfigEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        client.getCloudbreakClient()
                .proxyConfigEndpoint()
                .deletePrivate(proxyconfigEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
    }

    static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
        delete(integrationTestContext, entity);
    }
}