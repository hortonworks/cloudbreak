package com.sequenceiq.cloudbreak.cloud.azure;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureInteractiveLoginStatusCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

/**
 * Created by perdos on 9/22/16.
 */
@Service
@Scope("singleton")
public class AzureInteractiveLogin {

    public static final String XPLAT_CLI_CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";

    public static final String MANAGEMENT_CORE_WINDOWS = "https://management.core.windows.net/";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInteractiveLogin.class);

    private Executor executor;

    private AzureInteractiveLoginStatusCheckerContext azureInteractiveLoginStatusCheckerContext;

    @Inject
    private AzurePollTaskFactory azurePollTaskFactory;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @PostConstruct
    public void init() {
        executor = Executors.newSingleThreadExecutor();
    }

    public Map<String, String> login(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential,
            CredentialNotifier credentialNotifier) {
        Response deviceCodeResponse = getDeviceCode();

        if (deviceCodeResponse.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
            LOGGER.info("Successful device code response: " + deviceCodeResponse.getStatus());
            String jsonString = deviceCodeResponse.readEntity(String.class);
            LOGGER.info("Device code json response: " + jsonString);
            try {
                JsonNode deviceCodeJsonNode = new ObjectMapper().readTree(jsonString);

                int pollInterval = deviceCodeJsonNode.get("interval").asInt();
                int expiresIn = deviceCodeJsonNode.get("expires_in").asInt();
                String deviceCode = deviceCodeJsonNode.get("device_code").asText();

                createCheckerContextAndCancelPrevious(extendedCloudCredential, deviceCode, credentialNotifier);
                startAsyncPolling(cloudContext, pollInterval, expiresIn);

                return extractParameters(deviceCodeJsonNode);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            LOGGER.error("interactive login error, status: " + deviceCodeResponse.getStatus());
            throw new IllegalStateException("interactive login error");
        }
    }

    private void startAsyncPolling(CloudContext cloudContext, int pollInterval, int expiresIn) {
        PollTask<Boolean> interactiveLoginStatusCheckerTask = azurePollTaskFactory.interactiveLoginStatusCheckerTask(
                cloudContext, azureInteractiveLoginStatusCheckerContext);
        executor.execute(() -> {
            try {
                syncPollingScheduler.schedule(interactiveLoginStatusCheckerTask, pollInterval, expiresIn / pollInterval, 1);
            } catch (Exception e) {
                LOGGER.error("Interactive login schedule failed", e);
            }
        });
    }

    private void createCheckerContextAndCancelPrevious(ExtendedCloudCredential extendedCloudCredential, String deviceCode,
            CredentialNotifier credentialNotifier) {
        if (azureInteractiveLoginStatusCheckerContext != null) {
            azureInteractiveLoginStatusCheckerContext.cancel();
        }
        azureInteractiveLoginStatusCheckerContext = new AzureInteractiveLoginStatusCheckerContext(deviceCode, extendedCloudCredential, credentialNotifier);
    }

    private Map<String, String> extractParameters(JsonNode deviceCodeJsonNode) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("user_code", deviceCodeJsonNode.get("user_code").asText());
        parameters.put("verification_url", deviceCodeJsonNode.get("verification_url").asText());
        return parameters;
    }

    private Response getDeviceCode() {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target("https://login.microsoftonline.com/common/oauth2");

        Form form = new Form();
        form.param("client_id", XPLAT_CLI_CLIENT_ID);
        form.param("resource", MANAGEMENT_CORE_WINDOWS);
        form.param("mkt", "en-us");

        Builder request = resource.path("devicecode").queryParam("api-version", "1.0").request();
        request.accept(MediaType.APPLICATION_JSON);
        return request.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }
}
