package com.sequenceiq.cloudbreak.cloud.arm;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

@Service
public class ArmCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmCredentialConnector.class);

    @Inject
    private ArmInteractiveLogin armInteractiveLogin;

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext) {
        try {
            AzureRMClient client = authenticatedContext.getParameter(AzureRMClient.class);
            client.getToken();
        } catch (NullPointerException ex) {
            String message = "Invalid App ID or Tenant ID or Password";
            LOGGER.error(message, ex);
            return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.FAILED, ex, message);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.FAILED, e, e.getMessage());
        }
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public Map<String, String> interactiveLogin(AuthenticatedContext authenticatedContext, ExtendedCloudCredential extendedCloudCredential) {
        return armInteractiveLogin.login(authenticatedContext, extendedCloudCredential);
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.DELETED);
    }
}
