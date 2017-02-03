package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureInteractiveLogin.XPLAT_CLI_CLIENT_ID;

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
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureInteractiveLogin;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureInteractiveLoginStatusCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

import reactor.bus.EventBus;

/**
 * Created by perdos on 9/22/16.
 */
@Component(AzureInteractiveLoginStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class AzureInteractiveLoginStatusCheckerTask extends PollBooleanStateTask {

    public static final String NAME = "armInteractiveLoginStatusCheckerTask";

    public static final String GRAPH_WINDOWS = "https://graph.windows.net/";

    public static final String GRAPH_API_VERSION = "1.42-previewInternal";

    private static final String LOGIN_MICROSOFTONLINE_OAUTH2 = "https://login.microsoftonline.com/common/oauth2";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInteractiveLoginStatusCheckerTask.class);

    private static final String PASSWORD = "cloudbreak";

    private final AzureInteractiveLoginStatusCheckerContext armInteractiveLoginStatusCheckerContext;

    private Client client;

    @Inject
    private ApplicationCreator applicationCreator;

    @Inject
    private AzureRoleManager azureRoleManager;

    @Inject
    private PrincipalCreator principalCreator;

    @Inject
    private EventBus eventBus;

    public AzureInteractiveLoginStatusCheckerTask(CloudContext cloudContext,
            AzureInteractiveLoginStatusCheckerContext armInteractiveLoginStatusCheckerContext) {
        super(new AuthenticatedContext(cloudContext, armInteractiveLoginStatusCheckerContext.getExtendedCloudCredential()), false);
        this.armInteractiveLoginStatusCheckerContext = armInteractiveLoginStatusCheckerContext;
    }

    @Override
    public boolean cancelled() {
        return armInteractiveLoginStatusCheckerContext.isCancelled();
    }

    @PostConstruct
    public void init() {
        client = ClientBuilder.newClient();
    }

    @Override
    public Boolean call() {
        Response response = createPollingRequest();
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            String tokenResponseString = response.readEntity(String.class);
            try {
                String accessToken = new JSONObject(tokenResponseString).getString("access_token");
                LOGGER.info("Access token received");

                ExtendedCloudCredential extendedCloudCredential = armInteractiveLoginStatusCheckerContext.getExtendedCloudCredential();
                AzureCredentialView armCredentialView = new AzureCredentialView(extendedCloudCredential);

                try {
                    String appId = applicationCreator.createApplication(accessToken, armCredentialView.getTenantId());
                    sendStatusMessage(extendedCloudCredential, "Cloudbreak application created");
                    String principalObjectId = principalCreator.createServicePrincipal(accessToken, appId, armCredentialView.getTenantId());
                    sendStatusMessage(extendedCloudCredential, "Principal created for application");
                    azureRoleManager.assignRole(accessToken, armCredentialView.getSubscriptionId(), principalObjectId);
                    sendStatusMessage(extendedCloudCredential, "Role assigned for principal");

                    extendedCloudCredential.putParameter("accessKey", appId);
                    extendedCloudCredential.putParameter("secretKey", PASSWORD);

                    armInteractiveLoginStatusCheckerContext.getCredentialNotifier().createCredential(getAuthenticatedContext().getCloudContext(),
                            extendedCloudCredential);
                } catch (InteractiveLoginException e) {
                    LOGGER.error("Interactive login failed: ", e.getMessage());
                    sendErrorStatusMessage(extendedCloudCredential, e.getMessage());
                }
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
            return true;
        } else {
            LOGGER.info("Polling request failed this time, status code {}, response: {}", response.getStatus(), response.readEntity(String.class));
            return false;
        }
    }

    private void sendStatusMessage(ExtendedCloudCredential extendedCloudCredential, String message) {
        armInteractiveLoginStatusCheckerContext.getCredentialNotifier().sendStatusMessage(getAuthenticatedContext().getCloudContext(),
                extendedCloudCredential, false, message);
    }

    private void sendErrorStatusMessage(ExtendedCloudCredential extendedCloudCredential, String message) {
        armInteractiveLoginStatusCheckerContext.getCredentialNotifier().sendStatusMessage(getAuthenticatedContext().getCloudContext(),
                extendedCloudCredential, true, message);
    }

    private Response createPollingRequest() {
        Form pollingForm = createPollingForm();
        WebTarget resource = client.target(LOGIN_MICROSOFTONLINE_OAUTH2);
        Invocation.Builder request = resource.path("token").queryParam("api-version", "1.0").request();
        request.accept(MediaType.APPLICATION_JSON);
        return request.post(Entity.entity(pollingForm, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    private Form createPollingForm() {
        Form pollingForm = new Form();
        pollingForm.param("grant_type", "device_code");
        pollingForm.param("client_id", XPLAT_CLI_CLIENT_ID);
        pollingForm.param("resource", AzureInteractiveLogin.MANAGEMENT_CORE_WINDOWS);
        pollingForm.param("code", armInteractiveLoginStatusCheckerContext.getDeviceCode());
        return pollingForm;
    }


}
