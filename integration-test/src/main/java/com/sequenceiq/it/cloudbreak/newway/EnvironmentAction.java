package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentDetachV4Request;
import com.sequenceiq.it.IntegrationTestContext;

public class EnvironmentAction {
    private EnvironmentAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        EnvironmentEntity environmentEntity = (EnvironmentEntity) entity;

        CloudbreakClient client;

        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        environmentEntity.setResponse(
                client.getCloudbreakClient()
                        .environmentV4Endpoint()
                        .post(client.getWorkspaceId(), environmentEntity.getRequest()));

        logJSON("Environment post request: ", environmentEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        EnvironmentEntity environmentEntity = (EnvironmentEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        environmentEntity.setResponse(
                client.getCloudbreakClient()
                        .environmentV4Endpoint()
                        .get(client.getWorkspaceId(), environmentEntity.getName()));
        logJSON("Environment get response: ", environmentEntity.getResponse());
    }

    public static void list(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        EnvironmentEntity environmentEntity = (EnvironmentEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        environmentEntity.setResponseSimpleEnvSet(
                client.getCloudbreakClient()
                        .environmentV4Endpoint()
                        .list(client.getWorkspaceId()).getResponses());
        logJSON("Environment list response: ", environmentEntity.getResponse());
    }

    public static void putAttachResources(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        EnvironmentEntity environmentEntity = (EnvironmentEntity) entity;
        CloudbreakClient client;
        EnvironmentAttachV4Request environmentAttachV4Request = new EnvironmentAttachV4Request();
        environmentAttachV4Request.setLdaps(environmentEntity.getRequest().getLdaps());
        environmentAttachV4Request.setProxies(environmentEntity.getRequest().getProxies());
        environmentAttachV4Request.setDatabases(environmentEntity.getRequest().getDatabases());

        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        environmentEntity.setResponse(
                client.getCloudbreakClient()
                        .environmentV4Endpoint()
                        .attach(client.getWorkspaceId(), environmentEntity.getName(), environmentAttachV4Request));
        logJSON("Environment put attach response: ", environmentEntity.getResponse());
    }

    public static void putDetachResources(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        EnvironmentEntity environmentEntity = (EnvironmentEntity) entity;
        CloudbreakClient client;
        EnvironmentDetachV4Request environmentDetachV4Request = new EnvironmentDetachV4Request();
        environmentDetachV4Request.setLdaps(environmentEntity.getRequest().getLdaps());
        environmentDetachV4Request.setProxies(environmentEntity.getRequest().getProxies());
        environmentDetachV4Request.setDatabases(environmentEntity.getRequest().getDatabases());

        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        environmentEntity.setResponse(
                client.getCloudbreakClient()
                        .environmentV4Endpoint()
                        .detach(client.getWorkspaceId(), environmentEntity.getName(), environmentDetachV4Request));
        logJSON("Environment put detach response: ", environmentEntity.getResponse());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
    }
}