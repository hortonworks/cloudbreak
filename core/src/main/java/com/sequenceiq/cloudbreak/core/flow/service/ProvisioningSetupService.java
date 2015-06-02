package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_TEMP_SSH_KEY_LOCATION;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

@Component
public class ProvisioningSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningSetupService.class);

    public static final String SSH_PRIVATEKEY_LOCATION = "ssh.privatekey.location";
    public static final String SSH_PUBLICKEY_LOCATION = "ssh.publickey.location";
    private static final String CB_SSH_KEY_PREFIX = "/cb-ssh-key-";

    @Value("${cb.temp.ssh.key.location:" + CB_TEMP_SSH_KEY_LOCATION + "}")
    private String sshKeyLocation;

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    public ProvisionSetupComplete setup(Stack stack) throws Exception {
        ProvisionSetupComplete setupComplete = (ProvisionSetupComplete) provisionSetups.get(stack.cloudPlatform()).setupProvisioning(stack);
        LOGGER.info("Generating temporary SSH keypair.");
        String publicKeyFilename = sshKeyLocation + "/cb-ssh-key-" + stack.getId() + ".pub";
        String privateKeyFilename = sshKeyLocation + CB_SSH_KEY_PREFIX + stack.getId();
        JSch jsch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048);
        keyPair.writePrivateKey(privateKeyFilename);
        keyPair.writePublicKey(publicKeyFilename, "cloudbreak");
        keyPair.dispose();
        LOGGER.info("Generated temporary SSH keypair: {}. Fingerprint: {}", privateKeyFilename, keyPair.getFingerPrint());
        setupComplete.getSetupProperties().put(SSH_PRIVATEKEY_LOCATION, privateKeyFilename);
        setupComplete.getSetupProperties().put(SSH_PUBLICKEY_LOCATION, publicKeyFilename);
        return setupComplete;
    }
}
