package com.sequenceiq.cloudbreak.cloud

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus

/**
 * Manages the credentials e.g public keys on Cloud Plarform side
 */
interface CredentialConnector {

    /**
     * Check whether the credential (e.g public key) associated with a stack (cluster) has present on Cloud provider.

     * @param authenticatedContext the authenticated context which holds the client object
     * *
     * @return the status respone of method call
     */
    fun verify(authenticatedContext: AuthenticatedContext): CloudCredentialStatus


    /**
     * Create the credential (e.g public key) associated with a stack (cluster) on Cloud provider.

     * @param authenticatedContext the authenticated context which holds the client object
     * *
     * @return the status respone of method call
     */
    fun create(authenticatedContext: AuthenticatedContext): CloudCredentialStatus


    /**
     * Delete the credential (e.g public key) associated with a stack (cluster) from Cloud provider.

     * @param authenticatedContext the authenticated context which holds the client object
     * *
     * @return the status respone of method call
     */
    fun delete(authenticatedContext: AuthenticatedContext): CloudCredentialStatus

}
