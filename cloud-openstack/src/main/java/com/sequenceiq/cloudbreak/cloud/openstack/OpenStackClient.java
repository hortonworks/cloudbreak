package com.sequenceiq.cloudbreak.cloud.openstack;

import javax.annotation.PostConstruct;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.identity.Access;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@Component
public class OpenStackClient {

    @Value("${cb.openstack.api.debug:true}")
    private boolean debug;

    @PostConstruct
    public void init() {
        OSFactory.enableHttpLoggingFilter(debug);
    }

    public AuthenticatedContext createAuthenticatedContext(StackContext stackContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(stackContext, cloudCredential);
        Access access = createAccess(cloudCredential);
        authenticatedContext.putParameter(Access.class, access);
        return authenticatedContext;
    }

    public OSClient createOSClient(AuthenticatedContext authenticatedContext) {
        Access access = authenticatedContext.getParameter(Access.class);
        return createOSClient(access);
    }

    public Access createAccess(CloudCredential credential) {
        KeystoneCredentialView osCredential = new KeystoneCredentialView(credential);
        return OSFactory.builder().endpoint(osCredential.getEndpoint())
                .credentials(osCredential.getUserName(), osCredential.getPassword())
                .tenantName(osCredential.getTenantName())
                .authenticate().getAccess();
    }


    public OSClient createOSClient(Access access) {
        return OSFactory.clientFromAccess(access);
    }


}
