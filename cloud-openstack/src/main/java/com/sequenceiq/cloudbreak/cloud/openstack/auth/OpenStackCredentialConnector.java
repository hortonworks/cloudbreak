package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import static com.sequenceiq.cloudbreak.cloud.model.CloudCredential.SMART_SENSE_ID;
import static java.lang.String.format;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Keypair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackSmartSenseIdGenerator;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@Service
public class OpenStackCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackCredentialConnector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackSmartSenseIdGenerator smartSenseIdGenerator;

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        String smartSenseId = smartSenseIdGenerator.getSmartSenseId();
        if (StringUtils.isNoneEmpty(smartSenseId)) {
            credential.putParameter(SMART_SENSE_ID, smartSenseId);
        }
        return new CloudCredentialStatus(credential, CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext auth) {
        LOGGER.info("Create credential: {}", auth.getCloudCredential());
        OSClient client = openStackClient.createOSClient(auth);

        KeystoneCredentialView keystoneCredential = openStackClient.createKeystoneCredential(auth);

        String keyPairName = keystoneCredential.getKeyPairName();
        Keypair keyPair = client.compute().keypairs().get(keyPairName);
        if (keyPair != null) {
            return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.FAILED, null, format("Key with name: %s already exist", keyPairName));
        }

        try {
            keyPair = client.compute().keypairs().create(keyPairName, keystoneCredential.getPublicKey());
            LOGGER.info("Credential has been created: {}, kp: {}", auth.getCloudCredential(), keyPair);
            return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.CREATED);
        } catch (Exception e) {
            LOGGER.error("Failed to create credential", e);
            return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.FAILED, e, e.getMessage());
        }
    }

    @Override
    public Map<String, String> interactiveLogin(AuthenticatedContext authenticatedContext, ExtendedCloudCredential extendedCloudCredential) {
        throw new UnsupportedOperationException("Interactive login not supported on Openstack");
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
