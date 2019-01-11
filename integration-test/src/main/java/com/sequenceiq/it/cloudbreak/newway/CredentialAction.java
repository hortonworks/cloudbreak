package com.sequenceiq.it.cloudbreak.newway;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

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
                        .credentialV4Endpoint()
                        .post(1L, credentialEntity.getRequest()));
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
                        .credentialV4Endpoint()
                        .get(1L, credentialEntity.getName()));
        Log.logJSON(" get credential response: ", credentialEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get all private credential. ");
        credentialEntity.setResponses(
                client.getCloudbreakClient().credentialV4Endpoint().list(1L).getResponses());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" delete "
                .concat(credentialEntity.getName())
                .concat(" private credential. "));
        client.getCloudbreakClient().credentialV4Endpoint()
                .delete(1L, credentialEntity.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        if (getWithoutException(integrationTestContext, entity)) {
            return;
        }
        if (postWithoutException(integrationTestContext, entity)) {
            return;
        }
        try {
            get(integrationTestContext, entity);
        } catch (IOException iox) {
            throw new RuntimeException(iox);
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

    private static boolean getWithoutException(IntegrationTestContext integrationTestContext, Entity entity) {
        try {
            get(integrationTestContext, entity);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean postWithoutException(IntegrationTestContext integrationTestContext, Entity entity) {
        try {
            post(integrationTestContext, entity);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
