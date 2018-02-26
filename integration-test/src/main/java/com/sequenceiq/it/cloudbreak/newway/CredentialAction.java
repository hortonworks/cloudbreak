package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

import java.io.IOException;

class CredentialAction {

    private CredentialAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" post "
                .concat(credentialEntity.getName())
                .concat(" private credential. "));
        credentialEntity.setResponse(
                client.getCloudbreakClient()
                        .credentialEndpoint()
                        .postPrivate(credentialEntity.getRequest()));
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get "
                .concat(credentialEntity.getName())
                .concat(" private credential. "));
        credentialEntity.setResponse(
                client.getCloudbreakClient()
                        .credentialEndpoint()
                        .getPrivate(credentialEntity.getName()));
        Log.logJSON(" get credential response: ", credentialEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get all private credential. ");
        credentialEntity.setResponses(
                client.getCloudbreakClient().credentialEndpoint().getPrivates());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" delete "
                .concat(credentialEntity.getName())
                .concat(" private credential. "));
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
