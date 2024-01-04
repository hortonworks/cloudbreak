package com.sequenceiq.cloudbreak.cloud;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponses;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.credential.CredentialVerificationContext;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.common.model.CredentialType;

/**
 * Manages the credentials e.g public keys on Cloud Plarform side
 */
public interface CredentialConnector {

    /**
     * Check whether the credential (e.g public key) associated with a stack (cluster) has present on Cloud provider.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @return the status response of method call
     */
    CloudCredentialStatus verify(@Nonnull AuthenticatedContext authenticatedContext, CredentialVerificationContext credentialVerificationContext);

    /**
     * Check whether the credential (e.g public key) associated with a stack (cluster) has present on Cloud provider.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @return the status response of method call
     */
    default CDPServicePolicyVerificationResponses verifyByServices(@Nonnull AuthenticatedContext authenticatedContext,
        List<String> services, Map<String, String> experiencePrerequisites) {
        throw new UnsupportedOperationException("Verification for services which are not CB related not supported!");
    }

    /**
     * Create the credential (e.g public key) associated with a stack (cluster) on Cloud provider.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @return the status respone of method call
     */
    CloudCredentialStatus create(@Nonnull AuthenticatedContext authenticatedContext);

    /**
     * Delete the credential (e.g public key) associated with a stack (cluster) from Cloud provider.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @return the status respone of method call
     */
    CloudCredentialStatus delete(@Nonnull AuthenticatedContext authenticatedContext);

    /**
     * Get the necessary information that is necessary for a successful credential creation.
     *
     * @param cloudContext the cloud context that holds the cloud related information
     * @param externalId that should be added as prerequisites
     * @return the necessary prerequisites for credential creation
     */
    default CredentialPrerequisitesResponse getPrerequisites(CloudContext cloudContext, String externalId, String auditExternalId, String deploymentAddress,
        CredentialType type) {
        String message = String.format("There is no prerequisites for '%s' cloud platform!", cloudContext.getPlatform().value());
        throw new UnsupportedOperationException(message);
    }
}
