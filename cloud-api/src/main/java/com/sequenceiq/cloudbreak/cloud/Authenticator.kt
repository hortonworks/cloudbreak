package com.sequenceiq.cloudbreak.cloud

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

/**
 * Authenticator class for the Cloud provider.
 */
interface Authenticator : CloudPlatformAware {

    /**
     * Invoked to authenticate against the Cloud provider. The authentication is invoked before each operation e.g. before [ (AuthenticatedContext,][ResourceConnector.launch]. The AuthenticatedContext context object is not cached by Cloudbreak.

     * @param cloudContext    the context containing information to identify which stack (cluster) is affected
     * *
     * @param cloudCredential credentials for the given platform
     * *
     * @return authenticated context object, shall contain a the authenticated client object. The client object needs to be threadsafe.
     */
    fun authenticate(cloudContext: CloudContext, cloudCredential: CloudCredential): AuthenticatedContext

}
