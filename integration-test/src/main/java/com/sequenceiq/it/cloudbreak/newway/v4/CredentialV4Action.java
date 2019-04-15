package com.sequenceiq.it.cloudbreak.newway.v4;

import static com.sequenceiq.it.cloudbreak.RetryOnGatewayTimeout.retry;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class CredentialV4Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialV4Action.class);

    private CredentialV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialTestDto credentialTestDto = (CredentialTestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" post "
                .concat(credentialTestDto.getName())
                .concat(" private credential. "));
        credentialTestDto.setResponse(
                client.getCloudbreakClient()
                        .credentialV4Endpoint()
                        .post(workspaceId, credentialTestDto.getRequest()));
    }

    public static void put(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialTestDto credentialTestDto = (CredentialTestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" put "
                .concat(credentialTestDto.getName())
                .concat(" private credential. "));
        credentialTestDto.setResponse(
                client.getCloudbreakClient()
                        .credentialV4Endpoint()
                        .put(workspaceId, credentialTestDto.getRequest()));
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        get(integrationTestContext, entity, 1);
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity, int retryQuantity) throws IOException {
        CredentialTestDto credentialTestDto = (CredentialTestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get "
                .concat(credentialTestDto.getName())
                .concat(" private credential. "));
        credentialTestDto.setResponse(retry(() -> client.getCloudbreakClient()
                        .credentialV4Endpoint()
                        .get(workspaceId, credentialTestDto.getName()), retryQuantity));
        Log.logJSON(" get credential response: ", credentialTestDto.getResponse());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity, CloudbreakClient client) throws IOException {
        CredentialTestDto credentialTestDto = (CredentialTestDto) entity;
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get "
                .concat(credentialTestDto.getName())
                .concat(" private credential. "));
        credentialTestDto.setResponse(
                client.getCloudbreakClient()
                        .credentialV4Endpoint()
                        .get(workspaceId, credentialTestDto.getName()));
        Log.logJSON(" get credential response: ", credentialTestDto.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        getAll(integrationTestContext, entity, 1);
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity, int retryQuantity) {
        CredentialTestDto credentialTestDto = (CredentialTestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get all private credential. ");
        credentialTestDto.setResponses((Set<CredentialV4Response>) retry(() -> client.getCloudbreakClient()
                        .credentialV4Endpoint()
                        .list(workspaceId), retryQuantity).getResponses());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity, CloudbreakClient client) {
        CredentialTestDto credentialTestDto = (CredentialTestDto) entity;
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" delete "
                .concat(credentialTestDto.getName())
                .concat(" private credential. "));
        client.getCloudbreakClient().credentialV4Endpoint()
                .delete(workspaceId, credentialTestDto.getName());
    }

    public static void safeDelete(IntegrationTestContext integrationTestContext, Entity entity, CloudbreakClient client) {
        try {
            get(integrationTestContext, entity, client);
            delete(integrationTestContext, entity, client);
        } catch (Exception e) {
            Log.log("Could not delete credential, probably already deleted.");
        }
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialTestDto credentialTestDto = (CredentialTestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(LOGGER, " delete %s private credential. ", credentialTestDto.getName());
        client.getCloudbreakClient().credentialV4Endpoint()
                .delete(workspaceId, credentialTestDto.getName());
    }

    public static CredentialTestDto deleteV2(TestContext testContext, CredentialTestDto entity, CloudbreakClient cloudbreakClient) {
        Log.log(LOGGER, "Delete %s credential. ", entity.getName());
        cloudbreakClient.getCloudbreakClient().credentialV4Endpoint()
                .delete(cloudbreakClient.getWorkspaceId(), entity.getName());
        return entity;
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        createInGiven(integrationTestContext, entity, 1);
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity, int retryQuantity) {
        if (getWithoutException(integrationTestContext, entity, retryQuantity)) {
            return;
        }
        if (postWithoutException(integrationTestContext, entity)) {
            return;
        }
        try {
            get(integrationTestContext, entity, retryQuantity);
        } catch (IOException iox) {
            throw new RuntimeException(iox);
        }
    }

    public static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity) {
        createDeleteInGiven(integrationTestContext, entity, 1);
    }

    public static void createDeleteInGiven(IntegrationTestContext integrationTestContext, Entity entity, int retryQuantity) {
        try {
            get(integrationTestContext, entity, retryQuantity);
        } catch (Exception e) {
            post(integrationTestContext, entity);
            delete(integrationTestContext, entity);
        }
    }

    private static boolean getWithoutException(IntegrationTestContext integrationTestContext, Entity entity, int retryQuantity) {
        try {
            get(integrationTestContext, entity, retryQuantity);
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
