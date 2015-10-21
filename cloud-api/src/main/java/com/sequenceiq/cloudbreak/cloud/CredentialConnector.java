package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;

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
    CloudCredentialStatus verify(AuthenticatedContext authenticatedContext);


    /**
     * Create the credential (e.g public key) associated with a stack (cluster) on Cloud provider.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @return the status respone of method call
     */
    CloudCredentialStatus create(AuthenticatedContext authenticatedContext);


    /**
     * Delete the credential (e.g public key) associated with a stack (cluster) from Cloud provider.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @return the status respone of method call
     */
    CloudCredentialStatus delete(AuthenticatedContext authenticatedContext);

}
