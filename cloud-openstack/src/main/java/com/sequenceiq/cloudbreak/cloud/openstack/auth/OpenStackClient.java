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
        createAccessOrToken(authenticatedContext);
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

    public OSClient createOSClient(CloudCredential cloudCredential) {
        String facing = cloudCredential.getStringParameter(FACING);

        if (isV2Keystone(cloudCredential)) {
            Access access = createAccess(cloudCredential);
            return OSFactory.clientFromAccess(access, Facing.value(facing));
        } else {
            Token token = createToken(cloudCredential);
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

    public boolean isV2Keystone(CloudCredential cloudCredential) {
        KeystoneCredentialView osCredential = createKeystoneCredential(cloudCredential);
        return osCredential.getVersion().equals(KeystoneCredentialView.CB_KEYSTONE_V2);
    }

    public KeystoneCredentialView createKeystoneCredential(CloudCredential cloudCredential) {
        return new KeystoneCredentialView(cloudCredential);
    }

    public KeystoneCredentialView createKeystoneCredential(AuthenticatedContext authenticatedContext) {
        return new KeystoneCredentialView(authenticatedContext);
    }

    private Access createAccess(CloudCredential cloudCredential) {
        KeystoneCredentialView osCredential = createKeystoneCredential(cloudCredential);

        if (KeystoneCredentialView.CB_KEYSTONE_V2.equals(osCredential.getVersion())) {
            Access access = OSFactory.builderV2().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword())
                    .tenantName(osCredential.getTenantName())
                    .authenticate()
                    .getAccess();
            return access;
        }
        return null;
    }

    private Token createToken(CloudCredential cloudCredential) {
        KeystoneCredentialView osCredential = createKeystoneCredential(cloudCredential);

        if (KeystoneCredentialView.CB_KEYSTONE_V3_DEFAULT_SCOPE.equals(osCredential.getScope())) {
            Token token = OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                    .authenticate()
                    .getToken();
            return token;
        } else if (KeystoneCredentialView.CB_KEYSTONE_V3_DOMAIN_SCOPE.equals(osCredential.getScope())) {
            Token token = OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                    .scopeToDomain(Identifier.byName(osCredential.getDomainName()))
                    .authenticate()
                    .getToken();
            return token;
        } else if (KeystoneCredentialView.CB_KEYSTONE_V3_PROJECT_SCOPE.equals(osCredential.getScope())) {
            Token token = OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()))
                    .scopeToProject(Identifier.byName(osCredential.getProjectName()), Identifier.byName(osCredential.getProjectDomain()))
                    .authenticate()
                    .getToken();
            return token;
        }
        return null;
    }

    private void createAccessOrToken(AuthenticatedContext authenticatedContext) {
        Access access = createAccess(authenticatedContext.getCloudCredential());
        Token token = createToken(authenticatedContext.getCloudCredential());

        if (token == null && access == null) {
            throw new CloudConnectorException("Unsupported keystone version");
        } else if (token != null) {
            authenticatedContext.putParameter(Token.class, token);
        } else {
            authenticatedContext.putParameter(Access.class, access);
        }
    }

}
