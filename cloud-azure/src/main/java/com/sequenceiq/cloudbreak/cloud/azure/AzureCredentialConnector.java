package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionExtractor;
import com.sequenceiq.cloudbreak.cloud.model.credential.CredentialVerificationContext;
import com.sequenceiq.cloudbreak.cloud.response.AzureCredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AuthenticationContextProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.CBRefreshTokenClientProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.CbDelegatedTokenCredentials;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.common.model.CredentialType;

@Service
public class AzureCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialConnector.class);

    @Inject
    private AzureInteractiveLogin azureInteractiveLogin;

    @Inject
    private AzureCredentialAppCreationCommand appCreationCommand;

    @Inject
    private AuthenticationContextProvider authenticationContextProvider;

    @Inject
    private CBRefreshTokenClientProvider cbRefreshTokenClientProvider;

    @Inject
    private AzurePlatformParameters azurePlatformParameters;

    @Inject
    private AzureExceptionExtractor exceptionExtractor;

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext, CredentialVerificationContext credentialVerificationContext) {
        CloudCredential cloudCredential = authenticatedContext.getCloudCredential();
        try {
            AzureClient client = authenticatedContext.getParameter(AzureClient.class);
            if (client.getCurrentSubscription() == null) {
                return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.FAILED, null,
                        "Your subscription ID is not valid");
            } else {
                client.getRefreshToken()
                        .ifPresent(refreshToken -> {
                            Map<String, String> codeGrantFlowBased = (Map<String, String>) cloudCredential
                                    .getParameter("azure", Map.class)
                                    .get(AzureCredentialView.CODE_GRANT_FLOW_BASED);
                            codeGrantFlowBased.put("refreshToken", refreshToken);
                        });
            }
        } catch (RuntimeException e) {
            String exceptionMessage = e.getMessage();
            LOGGER.warn(exceptionMessage, e);
            String errorMessage = Objects.requireNonNullElse(exceptionExtractor.extractErrorMessage(e),
                    String.format("Could not verify the credential on Azure. Original message: %s", exceptionMessage));
            return new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, e, errorMessage);
        }
        return new CloudCredentialStatus(cloudCredential, CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public Map<String, String> interactiveLogin(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential,
            CredentialNotifier credentialNotifier) {
        return azureInteractiveLogin.login(cloudContext, extendedCloudCredential, credentialNotifier);
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.DELETED);
    }

    @Override
    public CredentialPrerequisitesResponse getPrerequisites(CloudContext cloudContext, String externalId, String auditExternalId,
        String deploymentAddress, CredentialType type) {
        String credentialCreationCommand = appCreationCommand.generateEnvironmentCredentialCommand(deploymentAddress);
        String auditCredentialCreationCommand = appCreationCommand.generateAuditCredentialCommand(deploymentAddress);
        String encodedCommand;
        String roleDefJson;
        switch (type) {
            case ENVIRONMENT:
                roleDefJson = azurePlatformParameters.getRoleDefJson();
                encodedCommand = Base64.encodeBase64String(credentialCreationCommand.getBytes());
                break;
            case AUDIT:
                roleDefJson = azurePlatformParameters.getAuditRoleDefJson();
                encodedCommand = Base64.encodeBase64String(auditCredentialCreationCommand.getBytes());
                break;
            default:
                encodedCommand = null;
                roleDefJson = null;
                break;
        }
        AzureCredentialPrerequisites azurePrerequisites = new AzureCredentialPrerequisites(encodedCommand, roleDefJson);
        return new CredentialPrerequisitesResponse(cloudContext.getPlatform().value(), azurePrerequisites);
    }

    @Override
    public Map<String, String> initCodeGrantFlow(CloudContext cloudContext, CloudCredential cloudCredential) {
        Map<String, String> parameters = new HashMap<>();
        AzureCredentialView azureCredential = new AzureCredentialView(cloudCredential);

        ApplicationTokenCredentials applicationCredentials = new ApplicationTokenCredentials(
                azureCredential.getAccessKey(),
                azureCredential.getTenantId(),
                azureCredential.getSecretKey(),
                AzureEnvironment.AZURE);

        String replyUrl = appCreationCommand.getRedirectURL(String.valueOf(cloudContext.getAccountId()), azureCredential.getDeploymentAddress());
        CbDelegatedTokenCredentials creds = new CbDelegatedTokenCredentials(applicationCredentials, replyUrl, authenticationContextProvider,
                cbRefreshTokenClientProvider);

        String state = UUID.randomUUID().toString();
        parameters.put("appLoginUrl", creds.generateAuthenticationUrl(state));
        parameters.put("appReplyUrl", replyUrl);
        parameters.put("codeGrantFlowState", state);
        return parameters;
    }
}

