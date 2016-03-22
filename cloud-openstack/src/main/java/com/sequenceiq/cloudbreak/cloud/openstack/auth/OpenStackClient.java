package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.FACING;

import javax.annotation.PostConstruct;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.Access;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@Component
public class OpenStackClient {

    @Value("${cb.openstack.api.debug:}")
    private boolean debug;

    @PostConstruct
    public void init() {
        OSFactory.enableHttpLoggingFilter(debug);
    }

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        Access access = createAccess(authenticatedContext);
        authenticatedContext.putParameter(Access.class, access);
        return authenticatedContext;
    }

    public OSClient createOSClient(AuthenticatedContext authenticatedContext) {
        Access access = authenticatedContext.getParameter(Access.class);
        String facing = authenticatedContext.getCloudCredential().getStringParameter(FACING);
        return createOSClient(access, Facing.value(facing));
    }

    public KeystoneCredentialView createKeystoneCredential(AuthenticatedContext authenticatedContext) {
        return new KeystoneCredentialView(authenticatedContext);
    }

    private OSClient createOSClient(Access access, Facing facing) {
        return OSFactory.clientFromAccess(access, facing);
    }

    private Access createAccess(AuthenticatedContext authenticatedContext) {
        KeystoneCredentialView osCredential = createKeystoneCredential(authenticatedContext);
        if (osCredential.getVersion().equals(KeystoneCredentialView.CB_KEYSTONE_V2)) {
            return OSFactory.builder().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword())
                    .tenantName(osCredential.getTenantName())
                    .authenticate().getAccess();
        } else if (osCredential.getScope().equals(KeystoneCredentialView.CB_KEYSTONE_V3_DEFAULT_SCOPE)) {
            return OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                    .authenticate()
                    .getAccess();
        } else if (osCredential.getScope().equals(KeystoneCredentialView.CB_KEYSTONE_V3_DOMAIN_SCOPE)) {
            return OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                    .scopeToDomain(Identifier.byName(osCredential.getDomainName()))
                    .authenticate()
                    .getAccess();
        } else if (osCredential.getScope().equals(KeystoneCredentialView.CB_KEYSTONE_V3_PROJECT_SCOPE)) {
            return OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                    .scopeToProject(Identifier.byName(osCredential.getProjectName()), Identifier.byName(osCredential.getProjectDomain()))
                    .authenticate()
                    .getAccess();
        } else {
            throw new CloudConnectorException("Unsupported keystone version");
        }
    }

}
