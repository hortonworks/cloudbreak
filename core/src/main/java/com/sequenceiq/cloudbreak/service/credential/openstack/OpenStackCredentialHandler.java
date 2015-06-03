package com.sequenceiq.cloudbreak.service.credential.openstack;

import java.util.List;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Keypair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;
import com.sequenceiq.cloudbreak.service.credential.RsaPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.stack.connector.openstack.OpenStackUtil;

@Component
public class OpenStackCredentialHandler implements CredentialHandler<OpenStackCredential> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackCredentialHandler.class);

    @Inject
    private RsaPublicKeyValidator rsaPublicKeyValidator;

    @Inject
    private OpenStackUtil openStackUtil;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    @Override
    public OpenStackCredential init(OpenStackCredential credential) {
        rsaPublicKeyValidator.validate(credential);
        validateCredential(credential);
        createCredentialIfAbsent(credential);
        return credential;
    }

    @Override
    public boolean delete(OpenStackCredential credential) {
        return true;
    }

    @Override
    public OpenStackCredential update(OpenStackCredential credential) throws Exception {
        return credential;
    }

    private void createCredentialIfAbsent(OpenStackCredential credential) {
        OSClient osClient = openStackUtil.createOSClient(credential);
        String keyPairName = openStackUtil.getKeyPairName(credential);
        List<? extends Keypair> keyPairs = osClient.compute().keypairs().list();
        boolean exists = false;
        for (Keypair keyPair : keyPairs) {
            if (keyPair.getName().equalsIgnoreCase(keyPairName)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            osClient.compute().keypairs().create(keyPairName, credential.getPublicKey());
        }
    }

    private void validateCredential(OpenStackCredential credential) {
        try {
            openStackUtil.createOSClient(credential);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to authenticate with OpenStack on endpoint: %s", credential.getEndpoint());
            if (!(e instanceof NullPointerException)) {
                errorMessage += String.format(" due to: %s", e.getMessage().replace("(Disable debug mode to suppress these details.)", ""));
            }
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }
}
