package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.AzureCredentialPrerequisites;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialPrerequisitesV4Response;
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

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext) {
        CloudCredential cloudCredential = authenticatedContext.getCloudCredential();
        try {
            AzureClient client = authenticatedContext.getParameter(AzureClient.class);
            client.getStorageAccounts().list();

            client.getRefreshToken()
                    .ifPresent(refreshToken -> cloudCredential.putParameter("refreshToken", refreshToken));
        } catch (RuntimeException e) {
            LOGGER.warn(e.getMessage(), e);
            return new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, e, e.getMessage());
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
    public CredentialPrerequisitesV4Response getPrerequisites(CloudContext cloudContext, String externalId, String deploymentAddress) {
        String creationCommand = appCreationCommand.generate(deploymentAddress);
        String encodedCommand = Base64.encodeBase64String(creationCommand.getBytes());
        AzureCredentialPrerequisites azurePrerequisites = new AzureCredentialPrerequisites(encodedCommand);
        return new CredentialPrerequisitesV4Response(cloudContext.getPlatform().value(), azurePrerequisites);
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

        String replyUrl = appCreationCommand.getRedirectURL(String.valueOf(cloudContext.getWorkspaceId()), azureCredential.getDeploymentAddress());
        CbDelegatedTokenCredentials creds = new CbDelegatedTokenCredentials(applicationCredentials, replyUrl, authenticationContextProvider,
                cbRefreshTokenClientProvider);

        String state = UUID.randomUUID().toString();
        parameters.put("appLoginUrl", creds.generateAuthenticationUrl(state));
        parameters.put("appReplyUrl", replyUrl);
        parameters.put("codeGrantFlowState", state);
        return parameters;
    }
}

