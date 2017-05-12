package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.FACING;

import javax.annotation.PostConstruct;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.v2.Access;
import org.openstack4j.model.identity.v3.Token;
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
        createAccess(authenticatedContext);
        return authenticatedContext;
    }

    public OSClient createOSClient(AuthenticatedContext authenticatedContext) {
        String facing = authenticatedContext.getCloudCredential().getStringParameter(FACING);

        if (isV2Keystone(authenticatedContext)) {
            Access access = authenticatedContext.getParameter(Access.class);
            return OSFactory.clientFromAccess(access, Facing.value(facing));
        } else {
            Token token = authenticatedContext.getParameter(Token.class);
            return OSFactory.clientFromToken(token, Facing.value(facing));
        }
    }

    public String getV2TenantId(AuthenticatedContext authenticatedContext) {
        KeystoneCredentialView osCredential = createKeystoneCredential(authenticatedContext);
        String facing = authenticatedContext.getCloudCredential().getStringParameter(FACING);
        Access access = authenticatedContext.getParameter(Access.class);
        return OSFactory.clientFromAccess(access, Facing.value(facing)).identity().tenants().getByName(osCredential.getTenantName()).getId();
    }

    public boolean isV2Keystone(AuthenticatedContext authenticatedContext) {
        KeystoneCredentialView osCredential = createKeystoneCredential(authenticatedContext);
        return osCredential.getVersion().equals(KeystoneCredentialView.CB_KEYSTONE_V2);
    }

    public KeystoneCredentialView createKeystoneCredential(AuthenticatedContext authenticatedContext) {
        return new KeystoneCredentialView(authenticatedContext);
    }

    private void createAccess(AuthenticatedContext authenticatedContext) {
        KeystoneCredentialView osCredential = createKeystoneCredential(authenticatedContext);
        if (osCredential.getVersion().equals(KeystoneCredentialView.CB_KEYSTONE_V2)) {
            Access access = OSFactory.builderV2().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword())
                    .tenantName(osCredential.getTenantName())
                    .authenticate()
                    .getAccess();
            authenticatedContext.putParameter(Access.class, access);
        } else if (osCredential.getScope().equals(KeystoneCredentialView.CB_KEYSTONE_V3_DEFAULT_SCOPE)) {
            Token token = OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                    .authenticate()
                    .getToken();
            authenticatedContext.putParameter(Token.class, token);
        } else if (osCredential.getScope().equals(KeystoneCredentialView.CB_KEYSTONE_V3_DOMAIN_SCOPE)) {
            Token token = OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                    .scopeToDomain(Identifier.byName(osCredential.getDomainName()))
                    .authenticate()
                    .getToken();
            authenticatedContext.putParameter(Token.class, token);
        } else if (osCredential.getScope().equals(KeystoneCredentialView.CB_KEYSTONE_V3_PROJECT_SCOPE)) {
            Token token = OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                    .scopeToProject(Identifier.byName(osCredential.getProjectName()), Identifier.byName(osCredential.getProjectDomain()))
                    .authenticate()
                    .getToken();
            authenticatedContext.putParameter(Token.class, token);
        } else {
            throw new CloudConnectorException("Unsupported keystone version");
        }
    }

}
