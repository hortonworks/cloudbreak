package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.response.PolicyComponentIdentifier.ENVIRONMENT;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionExtractor;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.credential.CredentialVerificationContext;
import com.sequenceiq.cloudbreak.cloud.response.AzureCredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.cloud.response.GranularPolicyResponse;
import com.sequenceiq.common.api.credential.AppAuthenticationType;
import com.sequenceiq.common.api.credential.AppCertificateStatus;
import com.sequenceiq.common.model.CredentialType;

@Service
public class AzureCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialConnector.class);

    @Inject
    private AzureCredentialAppCreationCommand appCreationCommand;

    @Inject
    private AzurePlatformParameters azurePlatformParameters;

    @Inject
    private AzureExceptionExtractor exceptionExtractor;

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext, CredentialVerificationContext credentialVerificationContext) {
        CloudCredential cloudCredential = authenticatedContext.getCloudCredential();
        try {
            AzureCredentialView azureCredentialView = new AzureCredentialView(cloudCredential);

            if (!(AppAuthenticationType.CERTIFICATE.name().equals(azureCredentialView.getAuthenticationType())
                    && AppCertificateStatus.KEY_GENERATED.name().equals(azureCredentialView.getStatus()))) {
                AzureClient client = authenticatedContext.getParameter(AzureClient.class);
                if (client.getCurrentSubscription() == null) {
                    return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.FAILED, null,
                            "Your subscription ID is not valid");
                } else {
                    Optional<String> accessToken = client.getAccessToken();
                    if (accessToken.isEmpty()) {
                        LOGGER.error("Couldn't get access token from azure.");
                    }
                }
            } else {
                LOGGER.info("Keys are generated for the Azure credential: {}, but they are not ACTIVE yet, therefore we just skip the validation",
                        cloudCredential.getName());
                return new CloudCredentialStatus(cloudCredential, CredentialStatus.CREATED, null,
                        "Keys are generated for the Azure credential: {}, but they are not ACTIVE yet, therefore we just skip the validation");
            }
        } catch (RuntimeException e) {
            String exceptionMessage = e.getMessage();
            LOGGER.warn(exceptionMessage, e);
            String errorMessage = Objects.requireNonNullElse(exceptionExtractor.extractErrorMessage(e),
                    String.format("Could not verify the credential on Azure. Original message: %s", exceptionMessage));
            return new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, new CloudConnectorException(errorMessage), errorMessage);
        }
        return new CloudCredentialStatus(cloudCredential, CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
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
        Map<String, String> minimalRoleDef = new HashMap<>();
        Set<GranularPolicyResponse> granularPolicies = new HashSet<>();
        switch (type) {
            case ENVIRONMENT:
                roleDefJson = azurePlatformParameters.getRoleDefJson();
                String minimalRoleDefJson = azurePlatformParameters.getMinimalRoleDefJson();
                minimalRoleDef.put("MinimalRoleDefinition", minimalRoleDefJson);
                if (StringUtils.isNotEmpty(minimalRoleDefJson)) {
                    granularPolicies.add(new GranularPolicyResponse(ENVIRONMENT.name(), "MinimalRoleDefinition",
                            azurePlatformParameters.getMinimalRoleDefJson()));
                }
                encodedCommand = Base64.encodeBase64String(credentialCreationCommand.getBytes());
                break;
            case AUDIT:
                roleDefJson = azurePlatformParameters.getAuditRoleDefJson();
                encodedCommand = Base64.encodeBase64String(auditCredentialCreationCommand.getBytes());
                break;
            default:
                LOGGER.debug("Unrecognized credential type: {}", type);
                granularPolicies = null;
                encodedCommand = null;
                roleDefJson = null;
                break;
        }
        AzureCredentialPrerequisites azurePrerequisites = new AzureCredentialPrerequisites(encodedCommand, roleDefJson, minimalRoleDef, granularPolicies);
        return new CredentialPrerequisitesResponse(cloudContext.getPlatform().value(), azurePrerequisites);
    }

}

