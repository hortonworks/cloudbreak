package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CERT_DIR;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_TLS_CERT_FILE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_TLS_PRIVATE_KEY_FILE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

@Component
public class ProvisioningSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningSetupService.class);

    private static final String SSH_KEY_PREFIX = "/cb-ssh-key-";
    private static final String SSH_PUBLIC_KEY_EXTENSION = ".pub";
    private static final String SSH_PUBLIC_KEY_COMMENT = "cloudbreak";

    public static final String SSH_PRIVATE_KEY_PATH = "ssh.private.key.path";
    public static final String SSH_PUBLIC_KEY_PATH = "ssh.public.key.path";

    @Value("${cb.cert.dir:" + CB_CERT_DIR + "}")
    private String certDir;

    @Value("${cb.tls.cert.file:" + CB_TLS_CERT_FILE + "}")
    private String clientCert;

    @Value("${cb.tls.private.key.file:" + CB_TLS_PRIVATE_KEY_FILE + "}")
    private String clientPrivateKey;

    @Autowired
    private StackRepository stackRepository;

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    public ProvisionSetupComplete setup(Stack stack) throws Exception {
        ProvisionSetupComplete setupComplete = (ProvisionSetupComplete) provisionSetups.get(stack.cloudPlatform()).setupProvisioning(stack);
        Path stackCertDir = createCertDir(stack.getId());
        stack.setCertDir(stackCertDir.toString());
        stackRepository.save(stack);
        copyClientKeys(stackCertDir);
        String keyName = SSH_KEY_PREFIX + stack.getId();
        String keyPath = generateTempSshKeypair(stackCertDir, keyName);
        setupComplete.getSetupProperties().put(SSH_PRIVATE_KEY_PATH, keyPath);
        setupComplete.getSetupProperties().put(SSH_PUBLIC_KEY_PATH, keyPath + SSH_PUBLIC_KEY_EXTENSION);
        return setupComplete;
    }

    private Path createCertDir(Long stackId) throws IOException, CloudbreakException {
        LOGGER.info("Creating directory for the keys and certificates under {}", certDir);
        Path stackCertDir = Paths.get(certDir + "/stack-" + stackId);
        if (!Files.exists(stackCertDir)) {
            try {
                Files.createDirectories(stackCertDir);
            } catch (SecurityException se) {
                throw new CloudbreakException("Failed to create directory: " + stackCertDir);
            }
        }
        return stackCertDir;
    }

    private void copyClientKeys(Path stackCertDir) throws IOException {
        Files.copy(Paths.get(clientPrivateKey), Paths.get(stackCertDir + "/key.pem"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(clientCert), Paths.get(stackCertDir + "/cert.pem"), StandardCopyOption.REPLACE_EXISTING);
    }

    private String generateTempSshKeypair(Path stackCertDir, String keyName) throws JSchException, IOException {
        LOGGER.info("Generating temporary SSH keypair.");
        String publicKeyPath = stackCertDir + keyName + SSH_PUBLIC_KEY_EXTENSION;
        String privateKeyPath = stackCertDir + keyName;
        JSch jsch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048);
        keyPair.writePrivateKey(privateKeyPath);
        keyPair.writePublicKey(publicKeyPath, SSH_PUBLIC_KEY_COMMENT);
        keyPair.dispose();
        LOGGER.info("Generated temporary SSH keypair: {}. Fingerprint: {}", privateKeyPath, keyPair.getFingerPrint());
        return privateKeyPath;
    }
}
