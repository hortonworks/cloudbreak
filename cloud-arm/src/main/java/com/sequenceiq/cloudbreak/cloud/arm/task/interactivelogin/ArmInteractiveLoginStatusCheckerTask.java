package com.sequenceiq.cloudbreak.cloud.arm.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmInteractiveLogin.XPLAT_CLI_CLIENT_ID;

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

import com.sequenceiq.cloudbreak.cloud.arm.ArmInteractiveLogin;
import com.sequenceiq.cloudbreak.cloud.arm.context.ArmInteractiveLoginStatusCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

import reactor.bus.Event;
import reactor.bus.EventBus;

/**
 * Created by perdos on 9/22/16.
 */
@Component(ArmInteractiveLoginStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class ArmInteractiveLoginStatusCheckerTask extends PollBooleanStateTask {

    public static final String NAME = "armInteractiveLoginStatusCheckerTask";

    public static final String GRAPH_WINDOWS = "https://graph.windows.net/";
    public static final String GRAPH_API_VERSION = "1.42-previewInternal";
    private static final String LOGIN_MICROSOFTONLINE_OAUTH2 = "https://login.microsoftonline.com/common/oauth2";
    private static final Logger LOGGER = LoggerFactory.getLogger(ArmInteractiveLoginStatusCheckerTask.class);
    private static final String PASSWORD = "cloudbreak";
    private final ArmInteractiveLoginStatusCheckerContext armInteractiveLoginStatusCheckerContext;
    private Client client;

    @Inject
    private ApplicationCreator applicationCreator;

    @Inject
    private AzureRoleManager azureRoleManager;

    @Inject
    private PrincipalCreator principalCreator;

    @Inject
    private EventBus eventBus;

    public ArmInteractiveLoginStatusCheckerTask(AuthenticatedContext authenticatedContext,
            ArmInteractiveLoginStatusCheckerContext armInteractiveLoginStatusCheckerContext) {
        super(authenticatedContext, false);
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

                CloudCredential cloudCredential = getAuthenticatedContext().getCloudCredential();
                ArmCredentialView armCredentialView = new ArmCredentialView(cloudCredential);

                String appId = applicationCreator.createApplication(accessToken, armCredentialView.getTenantId());
                String principalObjectId = principalCreator.createServicePrincipal(accessToken, appId, armCredentialView.getTenantId());
                azureRoleManager.assignRole(accessToken, armCredentialView.getSubscriptionId(), principalObjectId);

                ExtendedCloudCredential extendedCloudCredential = armInteractiveLoginStatusCheckerContext.getExtendedCloudCredential();
                extendedCloudCredential.putParameter("accessKey", appId);
                extendedCloudCredential.putParameter("secretKey", PASSWORD);

                InteractiveCredentialCreationRequest credentialCreationRequest =
                        new InteractiveCredentialCreationRequest(getAuthenticatedContext().getCloudContext(), cloudCredential, extendedCloudCredential);
                LOGGER.info("Triggering event: {}", credentialCreationRequest);
                eventBus.notify(credentialCreationRequest.selector(), Event.wrap(credentialCreationRequest));
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
            return true;
        } else {
            return false;
        }
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
        pollingForm.param("resource", ArmInteractiveLogin.MANAGEMENT_CORE_WINDOWS);
        pollingForm.param("code", armInteractiveLoginStatusCheckerContext.getDeviceCode());
        return pollingForm;
    }






}
