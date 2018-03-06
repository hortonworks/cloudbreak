package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;

import javax.annotation.Nonnull;

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
    Map<String, String> interactiveLogin(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential,
            CredentialNotifier credentialNotifier);

    /**
     * Delete the credential (e.g public key) associated with a stack (cluster) from Cloud provider.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @return the status respone of method call
     */
    CloudCredentialStatus delete(@Nonnull AuthenticatedContext authenticatedContext);

}
