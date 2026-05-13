package com.sequenceiq.cloudbreak.cloud.openstack;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.credential.CredentialVerificationContext;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClusterProxyService;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;

@Component
public class OpenStackCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackCredentialConnector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackClusterProxyService clusterProxyService;

    @Override
    public CloudCredentialStatus verify(@Nonnull AuthenticatedContext authenticatedContext, CredentialVerificationContext credentialVerificationContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(@Nonnull AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public CloudCredentialStatus delete(@Nonnull AuthenticatedContext authenticatedContext) {
        KeystoneCredentialView credential = openStackClient.createKeystoneCredential(authenticatedContext);
        if (StringUtils.isNotBlank(credential.getRemoteEnvironmentCrn())) {
            LOGGER.info("Credential has remoteEnvironmentCrn set, deregistering services from cluster proxy");
            String accountId = authenticatedContext.getCloudContext().getAccountId();
            try {
                clusterProxyService.deregisterServices(accountId, credential.getName());
            } catch (ClusterProxyException e) {
                LOGGER.warn("Failed to deregister OpenStack services from cluster proxy, continuing with delete", e);
            }
        }
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.DELETED);
    }
}
