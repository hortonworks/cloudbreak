package com.sequenceiq.cloudbreak.cloud.arm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.arm.context.ArmInteractiveLoginStatusCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

/**
 * Created by perdos on 9/22/16.
 */
@Service
@Scope(value = "singleton")
public class ArmInteractiveLogin {

    public static final String XPLAT_CLI_CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";

    public static final String MANAGEMENT_CORE_WINDOWS = "https://management.core.windows.net/";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmInteractiveLogin.class);

    private Executor executor;

    private ArmInteractiveLoginStatusCheckerContext armInteractiveLoginStatusCheckerContext;

    @Inject
    private ArmPollTaskFactory armPollTaskFactory;

    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @PostConstruct
    public void init() {
        executor = Executors.newSingleThreadExecutor();
    }

    public Map<String, String> login(AuthenticatedContext authenticatedContext, ExtendedCloudCredential extendedCloudCredential) {
        Response deviceCodeResponse = getDeviceCode();

        if (deviceCodeResponse.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            LOGGER.info("Successful device code response: " + deviceCodeResponse.getStatus());
            String jsonString = deviceCodeResponse.readEntity(String.class);
            LOGGER.info("Device code json response: " + jsonString);
            try {
                JSONObject jsonObject = new JSONObject(jsonString);

                int pollInterval = jsonObject.getInt("interval");
                int expiresIn = jsonObject.getInt("expires_in");
                String deviceCode = jsonObject.getString("device_code");

                createCheckerContextAndCancelPrevious(extendedCloudCredential, deviceCode);
                startAsyncPolling(authenticatedContext, pollInterval, expiresIn);

                return extractParameters(jsonObject);
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        } else {
            LOGGER.error("interactive login error, status: " + deviceCodeResponse.getStatus());
            throw new IllegalStateException("interactive login error");
        }
    }

    private void startAsyncPolling(AuthenticatedContext authenticatedContext, int pollInterval, int expiresIn) {
        PollTask<Boolean> interactiveLoginStatusCheckerTask = armPollTaskFactory.interactiveLoginStatusCheckerTask(
                authenticatedContext, armInteractiveLoginStatusCheckerContext);
        executor.execute(() -> {
            try {
                syncPollingScheduler.schedule(interactiveLoginStatusCheckerTask, pollInterval, expiresIn / pollInterval, 1);
            } catch (Exception e) {
                LOGGER.error("Interactive login schedule failed", e);
            }
        });
    }

    private void createCheckerContextAndCancelPrevious(ExtendedCloudCredential extendedCloudCredential, String deviceCode) {
        if (armInteractiveLoginStatusCheckerContext != null) {
            armInteractiveLoginStatusCheckerContext.cancel();
        }
        armInteractiveLoginStatusCheckerContext = new ArmInteractiveLoginStatusCheckerContext(deviceCode, extendedCloudCredential);
    }

    private Map<String, String> extractParameters(JSONObject jsonObject) throws JSONException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("user_code", jsonObject.getString("user_code"));
        parameters.put("verification_url", jsonObject.getString("verification_url"));
        return parameters;
    }

    private Response getDeviceCode() {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target("https://login.microsoftonline.com/common/oauth2");

        Form form = new Form();
        form.param("client_id", XPLAT_CLI_CLIENT_ID);
        form.param("resource", MANAGEMENT_CORE_WINDOWS);
        form.param("mkt", "en-us");

        Invocation.Builder request = resource.path("devicecode").queryParam("api-version", "1.0").request();
        request.accept(MediaType.APPLICATION_JSON);
        return request.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }
}
