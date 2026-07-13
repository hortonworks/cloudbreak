package com.sequenceiq.cloudbreak.cloud.openstack.client;

import static com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView.CB_KEYSTONE_V3_DOMAIN_SCOPE;
import static com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView.CB_KEYSTONE_V3_PROJECT_SCOPE;

import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.openstack.OSFactory;

import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

public final class KeystoneTokenFactory {

    private KeystoneTokenFactory() {
    }

    public static Token authenticate(Config config, String endpoint, KeystoneCredentialView credential) {
        return switch (credential.getScope()) {
            case CB_KEYSTONE_V3_DOMAIN_SCOPE -> OSFactory.builderV3().withConfig(config).endpoint(endpoint)
                    .credentials(credential.getUserName(), credential.getPassword(), Identifier.byName(credential.getUserDomain()))
                    .scopeToDomain(Identifier.byName(credential.getDomainName()))
                    .authenticate()
                    .getToken();
            case CB_KEYSTONE_V3_PROJECT_SCOPE -> OSFactory.builderV3().withConfig(config).endpoint(endpoint)
                    .credentials(credential.getUserName(), credential.getPassword(), Identifier.byName(credential.getUserDomain()))
                    .scopeToProject(Identifier.byName(credential.getProjectName()), Identifier.byName(credential.getProjectDomain()))
                    .authenticate()
                    .getToken();
            default -> throw new IllegalArgumentException("Unsupported keystone scope: " + credential.getScope());
        };
    }
}
