package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_OPENSTACK_API_DEBUG;

import javax.annotation.PostConstruct;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.identity.Access;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@Component
public class OpenStackClient {

    @Value("${cb.openstack.api.debug:" + CB_OPENSTACK_API_DEBUG + "}")
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
        return createOSClient(access);
    }

    public KeystoneCredentialView createKeystoneCredential(AuthenticatedContext authenticatedContext) {
        return new KeystoneCredentialView(authenticatedContext);
    }

    public Access createAccess(AuthenticatedContext authenticatedContext) {
        KeystoneCredentialView osCredential = createKeystoneCredential(authenticatedContext);
        if ( osCredential.getVersion().equals(KeystoneCredentialView.CB_KEYSTONE_V2) )
            return OSFactory.builder().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword())
                    .tenantName(osCredential.getTenantName())
                    .authenticate().getAccess();

        if ( osCredential.getScope().equals(KeystoneCredentialView.CB_KEYSTONE_V3_DEFAULT_SCOPE) )
            return OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()/*"DEV"*/))
                    .authenticate()
                    .getAccess();
        else if ( osCredential.getScope().equals(KeystoneCredentialView.CB_KEYSTONE_V3_DOMAIN_SCOPE) )
            return OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()/*"DEV"*/))
                    .scopeToDomain(Identifier.byName(osCredential.getDomainName()))
                    .authenticate()
                    .getAccess();
        else
            return OSFactory.builderV3().endpoint(osCredential.getEndpoint())
                    .credentials(osCredential.getUserName(), osCredential.getPassword(), Identifier.byName(osCredential.getUserDomain()/*"DEV"*/))
                    .scopeToProject(Identifier.byName(osCredential.getProjectName()/*"vivek-madani"*/), Identifier.byName(osCredential.getProjectDomain()/*"DEV"*/))
                    .authenticate()
                    .getAccess();
    }

    public OSClient createOSClient(Access access) {
        return OSFactory.clientFromAccess(access);
    }

}
