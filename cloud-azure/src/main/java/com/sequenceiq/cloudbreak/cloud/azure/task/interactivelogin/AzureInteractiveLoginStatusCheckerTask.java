package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureInteractiveLogin.MANAGEMENT_CORE_WINDOWS;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureInteractiveLogin.XPLAT_CLI_CLIENT_ID;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
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
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.management.graphrbac.implementation.ServicePrincipalInner;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTenant;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureInteractiveLoginStatusCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

/**
 * Created by perdos on 9/22/16.
 */
@Component(AzureInteractiveLoginStatusCheckerTask.NAME)
@Scope("prototype")
public class AzureInteractiveLoginStatusCheckerTask extends PollBooleanStateTask {

    public static final String NAME = "armInteractiveLoginStatusCheckerTask";

    public static final String GRAPH_WINDOWS = "https://graph.windows.net/";

    public static final String GRAPH_API_VERSION = "1.6";

    public static final String AZURE_MANAGEMENT = "https://management.azure.com/";

    public static final String LOGIN_MICROSOFTONLINE = "https://login.microsoftonline.com/";

    private static final String LOGIN_MICROSOFTONLINE_OAUTH2 = LOGIN_MICROSOFTONLINE + "common/oauth2";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInteractiveLoginStatusCheckerTask.class);

    private final AzureInteractiveLoginStatusCheckerContext armInteractiveLoginStatusCheckerContext;

    @Inject
    private SubscriptionChecker subscriptionChecker;

    @Inject
    private TenantChecker tenantChecker;

    @Inject
    private ApplicationCreator applicationCreator;

    @Inject
    private PrincipalCreator principalCreator;

    public AzureInteractiveLoginStatusCheckerTask(CloudContext cloudContext,
            AzureInteractiveLoginStatusCheckerContext armInteractiveLoginStatusCheckerContext) {
        super(new AuthenticatedContext(cloudContext, armInteractiveLoginStatusCheckerContext.getExtendedCloudCredential()), false);
        this.armInteractiveLoginStatusCheckerContext = armInteractiveLoginStatusCheckerContext;
    }

    @Override
    public boolean cancelled() {
        return armInteractiveLoginStatusCheckerContext.isCancelled();
    }

    @Override
    protected Boolean doCall() {
        Response response = createPollingRequest();
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
            String tokenResponseString = response.readEntity(String.class);
            try {
                String refreshToken = new ObjectMapper().readTree(tokenResponseString).get("refresh_token").asText();
                LOGGER.debug("Access token received");

                ExtendedCloudCredential extendedCloudCredential = armInteractiveLoginStatusCheckerContext.getExtendedCloudCredential();
                AzureCredentialView armCredentialView = new AzureCredentialView(extendedCloudCredential);

                try {
                    String tenantId = armCredentialView.getTenantId();
                    String graphApiAccessToken = createResourceToken(refreshToken, tenantId, GRAPH_WINDOWS);
                    String managementApiToken = createResourceToken(refreshToken, tenantId, MANAGEMENT_CORE_WINDOWS);
                    subscriptionChecker.checkSubscription(armCredentialView.getSubscriptionId(), managementApiToken);
                    List<AzureTenant> tenants = tenantChecker.getTenants(managementApiToken);
                    tenantChecker.checkTenant(tenantId, tenants);
                    String deploymentAddress = armCredentialView.getDeploymentAddress();
                    AzureApplication application = applicationCreator.createApplication(graphApiAccessToken, tenantId, deploymentAddress);
                    sendStatusMessage(extendedCloudCredential, "Cloudbreak application created");
                    applicationCreator.waitApplicationCreated(graphApiAccessToken, tenantId, application.getObjectId());
                    ServicePrincipalInner sp = principalCreator.createServicePrincipal(graphApiAccessToken, application.getAppId(), tenantId);
                    principalCreator.waitPrincipalCreated(graphApiAccessToken, sp.objectId(), tenantId, application);
                    String notification = new StringBuilder("Principal created for application!")
                            .append(" Name: ")
                            .append(sp.displayName())
                            .append(", AppId: ")
                            .append(sp.appId())
                            .toString();
                    sendStatusMessage(extendedCloudCredential, notification);

                    extendedCloudCredential.putParameter("accessKey", application.getAppId());
                    extendedCloudCredential.putParameter("secretKey", application.getAzureApplicationCreationView().getAppSecret());
                    extendedCloudCredential.putParameter("spDisplayName", sp.displayName());
                    extendedCloudCredential.putParameter("appObjectId", application.getObjectId());

                    armInteractiveLoginStatusCheckerContext.getCredentialNotifier().createCredential(getAuthenticatedContext().getCloudContext(),
                            extendedCloudCredential);
                } catch (InteractiveLoginException e) {
                    LOGGER.info("Interactive login failed", e);
                    sendErrorStatusMessage(extendedCloudCredential, e.getMessage());
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return true;
        } else {
            LOGGER.debug("Polling request failed this time, status code {}, response: {}", response.getStatus(), response.readEntity(String.class));
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
        WebTarget resource = ClientBuilder.newClient().target(LOGIN_MICROSOFTONLINE_OAUTH2);
        Builder request = resource.path("token").queryParam("api-version", "1.0").request();
        request.accept(MediaType.APPLICATION_JSON);
        return request.post(Entity.entity(pollingForm, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    private String createResourceToken(String refreshToken, String tenantId, String resource) throws InteractiveLoginException {
        Form resourceTokenForm = createResourceTokenForm(refreshToken, resource);
        WebTarget webTarget = ClientBuilder.newClient().target(LOGIN_MICROSOFTONLINE);
        Builder request = webTarget.path(tenantId + "/oauth2/token").queryParam("api-version", "1.0").request();
        request.accept(MediaType.APPLICATION_JSON);
        try (Response response = request.post(Entity.entity(resourceTokenForm, MediaType.APPLICATION_FORM_URLENCODED_TYPE))) {
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new InteractiveLoginException("Obtain access token for " + resource + " failed "
                        + "with tenant ID: " + tenantId + ", status code " + response.getStatus()
                        + ", error message: " + response.readEntity(String.class));
            }
            String responseString = response.readEntity(String.class);
            try {
                return new ObjectMapper().readTree(responseString).get("access_token").asText();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private Form createPollingForm() {
        Form pollingForm = new Form();
        pollingForm.param("grant_type", "device_code");
        pollingForm.param("client_id", XPLAT_CLI_CLIENT_ID);
        pollingForm.param("resource", MANAGEMENT_CORE_WINDOWS);
        pollingForm.param("code", armInteractiveLoginStatusCheckerContext.getDeviceCode());
        return pollingForm;
    }

    private Form createResourceTokenForm(String refreshToken, String resource) {
        Form graphApiTokenForm = new Form();
        graphApiTokenForm.param("grant_type", "refresh_token");
        graphApiTokenForm.param("client_id", XPLAT_CLI_CLIENT_ID);
        graphApiTokenForm.param("resource", resource);
        graphApiTokenForm.param("refresh_token", refreshToken);
        return graphApiTokenForm;
    }

}
