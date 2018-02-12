package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

class CredentialAction {

    private CredentialAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        credentialEntity.setResponse(
                client.getCloudbreakClient()
                        .credentialEndpoint()
                        .postPrivate(credentialEntity.getRequest()));
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        credentialEntity.setResponse(
                client.getCloudbreakClient()
                        .credentialEndpoint()
                        .getPrivate(credentialEntity.getName()));
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        credentialEntity.setResponses(
                client.getCloudbreakClient().credentialEndpoint().getPrivates());

    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        client.getCloudbreakClient().credentialEndpoint()
                .deletePrivate(credentialEntity.getName());
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
