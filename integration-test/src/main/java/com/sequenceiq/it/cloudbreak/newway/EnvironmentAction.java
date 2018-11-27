package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
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
                        .environmentV3Endpoint()
                        .create(client.getWorkspaceId(), environmentEntity.getRequest()));

        logJSON("Environment post request: ", environmentEntity.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        EnvironmentEntity environmentEntity = (EnvironmentEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        environmentEntity.setResponse(
                client.getCloudbreakClient()
                        .environmentV3Endpoint()
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
                        .environmentV3Endpoint()
                        .list(client.getWorkspaceId()));
        logJSON("Environment list response: ", environmentEntity.getResponse());
    }

    public static void putAttachResources(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        EnvironmentEntity environmentEntity = (EnvironmentEntity) entity;
        CloudbreakClient client;
        EnvironmentAttachRequest environmentAttachRequest = new EnvironmentAttachRequest();
        environmentAttachRequest.setLdapConfigs(environmentEntity.getRequest().getLdapConfigs());
        environmentAttachRequest.setProxyConfigs(environmentEntity.getRequest().getProxyConfigs());
        environmentAttachRequest.setRdsConfigs(environmentEntity.getRequest().getRdsConfigs());

        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        environmentEntity.setResponse(
                client.getCloudbreakClient()
                        .environmentV3Endpoint()
                        .attachResources(client.getWorkspaceId(), environmentEntity.getName(), environmentAttachRequest));
        logJSON("Environment put attach response: ", environmentEntity.getResponse());
    }

    public static void putDetachResources(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        EnvironmentEntity environmentEntity = (EnvironmentEntity) entity;
        CloudbreakClient client;
        EnvironmentDetachRequest environmentDetachRequest = new EnvironmentDetachRequest();
        environmentDetachRequest.setLdapConfigs(environmentEntity.getRequest().getLdapConfigs());
        environmentDetachRequest.setProxyConfigs(environmentEntity.getRequest().getProxyConfigs());
        environmentDetachRequest.setRdsConfigs(environmentEntity.getRequest().getRdsConfigs());

        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        environmentEntity.setResponse(
                client.getCloudbreakClient()
                        .environmentV3Endpoint()
                        .detachResources(client.getWorkspaceId(), environmentEntity.getName(), environmentDetachRequest));
        logJSON("Environment put detach response: ", environmentEntity.getResponse());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        post(integrationTestContext, entity);
    }
}