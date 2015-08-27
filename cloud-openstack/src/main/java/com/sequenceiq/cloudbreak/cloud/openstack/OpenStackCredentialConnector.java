package com.sequenceiq.cloudbreak.cloud.openstack;

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

    private static final String CB_KEYPAIR_NAME = "cb-keypair-";

    @Inject
    private OpenStackClient openStackClient;


    @Override
    public CloudCredentialStatus create(AuthenticatedContext authenticatedContext) {
        LOGGER.info("Create credential: {}", authenticatedContext.getCloudCredential());

        OSClient client = openStackClient.createOSClient(authenticatedContext);

        KeystoneCredentialView keystoneCredential = openStackClient.createKeystoneCredential(authenticatedContext.getCloudCredential());

        String keyPairName = keystoneCredential.getKeyPairName();
        Keypair keyPair = client.compute().keypairs().get(keyPairName);
        if (keyPair != null) {
            throw new CloudConnectorException(String.format("Key with name: %s already exist", keyPairName));
        }

        keyPair = client.compute().keypairs().create(keyPairName, keystoneCredential.getPublicKey());

        LOGGER.info("Credential has been created: {}, kp: {}", authenticatedContext.getCloudCredential(), keyPair);


        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext authenticatedContext) {
        LOGGER.info("Deleted credential: {}", authenticatedContext.getCloudCredential());

        KeystoneCredentialView keystoneCredentialView = openStackClient.createKeystoneCredential(authenticatedContext.getCloudCredential());

        OSClient client = openStackClient.createOSClient(authenticatedContext);
        KeystoneCredentialView keystoneCredential = openStackClient.createKeystoneCredential(authenticatedContext.getCloudCredential());
        String keyPairName = keystoneCredential.getKeyPairName();

        client.compute().keypairs().delete(keyPairName);

        LOGGER.info("Credential has been deleted: {}", authenticatedContext.getCloudCredential());

        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.DELETED);

    }

}
