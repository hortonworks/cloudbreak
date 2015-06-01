package com.sequenceiq.cloudbreak.cloud.openstack;

import javax.annotation.PostConstruct;

import org.openstack4j.api.OSClient;
import org.openstack4j.core.transport.internal.HttpLoggingFilter;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@Component
@Lazy
public class OpenStackClient {

    @Value("${cb.openstack.api.debug:true}")
    private boolean debug;

    @PostConstruct
    public void init() {
        System.getProperties().setProperty(HttpLoggingFilter.class.getName(), Boolean.toString(debug));
    }

    public OSClient createOSClient(CloudCredential credential) {
        KeystoneCredentialView osCredential = new KeystoneCredentialView(credential);

        return OSFactory.builder().endpoint(osCredential.getEndpoint())
                .credentials(osCredential.getUserName(), osCredential.getPassword())
                .tenantName(osCredential.getTenantName())
                .authenticate();
    }


}
