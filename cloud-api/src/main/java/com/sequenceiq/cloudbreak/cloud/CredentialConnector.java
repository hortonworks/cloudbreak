package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialPrerequisitesV4Response;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

/**
 * Manages the credentials e.g public keys on Cloud Plarform side
 */
public interface CredentialConnector {

    /**
     * Check whether the credential (e.g public key) associated with a stack (cluster) has present on Cloud provider.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @return the status respone of method call
     */
    CloudCredentialStatus verify(@Nonnull AuthenticatedContext authenticatedContext);


    /**
     * Create the credential (e.g public key) associated with a stack (cluster) on Cloud provider.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @return the status respone of method call
     */
    CloudCredentialStatus create(@Nonnull AuthenticatedContext authenticatedContext);


    /**
     * Interactive login for credential creation.
     *
     * @return parameters for interactive login
     */
    default Map<String, String> interactiveLogin(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential,
            CredentialNotifier credentialNotifier) {
        throw new UnsupportedOperationException("Interactive login not supported!");
    }

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
    default CredentialPrerequisitesV4Response getPrerequisites(CloudContext cloudContext, String externalId, String deploymentAddress) {
        String message = String.format("There is no prerequisites for '%s' cloud platform!", cloudContext.getPlatform().value());
        throw new UnsupportedOperationException(message);
    }

    /**
     * Get the necessary information that is necessary to start an authorization code grant based credential creation flow.
     *
     * @param cloudContext the cloud context that holds the cloud related information
     * @param cloudCredential the cloud credential that holds the credential creation related information
     * @return the necessary information for a code grant flow based credential creation
     */
    default Map<String, String> initCodeGrantFlow(CloudContext cloudContext, CloudCredential cloudCredential) {
        String message = String.format("There is no prerequisites for '%s' cloud platform!", cloudContext.getPlatform().value());
        throw new UnsupportedOperationException(message);
    }
}
