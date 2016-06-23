package com.sequenceiq.cloudbreak.cloud.openstack.auth

import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.FACING

import javax.annotation.PostConstruct

import org.openstack4j.api.OSClient
import org.openstack4j.api.types.Facing
import org.openstack4j.model.common.Identifier
import org.openstack4j.model.identity.Access
import org.openstack4j.openstack.OSFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView

@Component
class OpenStackClient {

    @Value("${cb.openstack.api.debug:}")
    private val debug: Boolean = false

    @PostConstruct
    fun init() {
        OSFactory.enableHttpLoggingFilter(debug)
    }

    fun createAuthenticatedContext(cloudContext: CloudContext, cloudCredential: CloudCredential): AuthenticatedContext {
        val authenticatedContext = AuthenticatedContext(cloudContext, cloudCredential)
        val access = createAccess(authenticatedContext)
        authenticatedContext.putParameter(Access::class.java, access)
        return authenticatedContext
    }

    fun createOSClient(authenticatedContext: AuthenticatedContext): OSClient {
        val access = authenticatedContext.getParameter<Access>(Access::class.java)
        val facing = authenticatedContext.cloudCredential.getStringParameter(FACING)
        return createOSClient(access, Facing.value(facing))
    }

    fun createKeystoneCredential(authenticatedContext: AuthenticatedContext): KeystoneCredentialView {
        return KeystoneCredentialView(authenticatedContext)
    }

    private fun createOSClient(access: Access, facing: Facing): OSClient {
        return OSFactory.clientFromAccess(access, facing)
    }

    private fun createAccess(authenticatedContext: AuthenticatedContext): Access {
        val osCredential = createKeystoneCredential(authenticatedContext)
        if (osCredential.version == KeystoneCredentialView.CB_KEYSTONE_V2) {
            return OSFactory.builder().endpoint(osCredential.endpoint).credentials(osCredential.userName, osCredential.password).tenantName(osCredential.tenantName).authenticate().access
        } else if (osCredential.scope == KeystoneCredentialView.CB_KEYSTONE_V3_DEFAULT_SCOPE) {
            return OSFactory.builderV3().endpoint(osCredential.endpoint).credentials(osCredential.userName, osCredential.password, Identifier.byName(osCredential.userDomain)).authenticate().access
        } else if (osCredential.scope == KeystoneCredentialView.CB_KEYSTONE_V3_DOMAIN_SCOPE) {
            return OSFactory.builderV3().endpoint(osCredential.endpoint).credentials(osCredential.userName, osCredential.password, Identifier.byName(osCredential.userDomain)).scopeToDomain(Identifier.byName(osCredential.domainName)).authenticate().access
        } else if (osCredential.scope == KeystoneCredentialView.CB_KEYSTONE_V3_PROJECT_SCOPE) {
            return OSFactory.builderV3().endpoint(osCredential.endpoint).credentials(osCredential.userName, osCredential.password, Identifier.byName(osCredential.userDomain)).scopeToProject(Identifier.byName(osCredential.projectName), Identifier.byName(osCredential.projectDomain)).authenticate().access
        } else {
            throw CloudConnectorException("Unsupported keystone version")
        }
    }

}
