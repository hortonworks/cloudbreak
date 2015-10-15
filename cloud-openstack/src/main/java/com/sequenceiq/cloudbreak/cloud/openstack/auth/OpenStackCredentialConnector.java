package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Keypair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@Service
public class OpenStackCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackCredentialConnector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext auth) {
        LOGGER.info("Create credential: {}", auth.getCloudCredential());

        OSClient client = openStackClient.createOSClient(auth);

        KeystoneCredentialView keystoneCredential = openStackClient.createKeystoneCredential(auth);

        String keyPairName = keystoneCredential.getKeyPairName();
        Keypair keyPair = client.compute().keypairs().get(keyPairName);
        if (keyPair != null) {
            throw new CloudConnectorException(String.format("Key with name: %s already exist", keyPairName));
        }

        keyPair = client.compute().keypairs().create(keyPairName, keystoneCredential.getPublicKey());
        LOGGER.info("Credential has been created: {}, kp: {}", auth.getCloudCredential(), keyPair);
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext auth) {
        LOGGER.info("Delete credential: {}", auth.getCloudCredential());

        OSClient client = openStackClient.createOSClient(auth);
        KeystoneCredentialView keystoneCredential = openStackClient.createKeystoneCredential(auth);
        String keyPairName = keystoneCredential.getKeyPairName();

        client.compute().keypairs().delete(keyPairName);

        LOGGER.info("Credential has been deleted: {}", auth.getCloudCredential());

        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.DELETED);
    }

}
