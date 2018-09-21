package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.api.model.v3.credential.CredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
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
    default CredentialPrerequisites getPrerequisites(CloudContext cloudContext, String externalId) {
        String message = String.format("There is no prerequisites for '%s' cloud platform!", cloudContext.getPlatform().value());
        throw new UnsupportedOperationException(message);
    }

}
