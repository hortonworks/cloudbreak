package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.sequenceiq.cloudbreak.api.model.InstanceMetadataType;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class TlsSecurityService {

    public static final String SALT_SIGN_KEY_PREFIX = "/cb-salt-sign-key-";

    private static final String PUBLIC_KEY_EXTENSION = ".pub";

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSecurityService.class);

    private static final String SSH_PUBLIC_KEY_COMMENT = "cloudbreak";

    private static final int DEFAULT_KEY_SIZE = 2048;

    private static final String SSH_KEY_PREFIX = "/cb-ssh-key-";

    @Value("${cb.cert.dir:}")
    private String certDir;

    @Value("#{'${cb.cert.dir:}/${cb.tls.cert.file:}'}")
    private String clientCert;

    @Value("#{'${cb.cert.dir:}/${cb.tls.private.key.file:}'}")
    private String clientPrivateKey;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public SecurityConfig storeSSHKeys(Long stackId) throws CloudbreakSecuritySetupException {
        try {
            generateTempSshKeypair(stackId);
            generateSaltSignKeypair(stackId);
            SecurityConfig securityConfig = new SecurityConfig();
            securityConfig.setClientKey(BaseEncoding.base64().encode(readClientKey(stackId).getBytes()));
            securityConfig.setClientCert(BaseEncoding.base64().encode(readClientCert(stackId).getBytes()));
            securityConfig.setCloudbreakSshPrivateKey(BaseEncoding.base64().encode(readPrivateTempSshKey(stackId).getBytes()));
            securityConfig.setCloudbreakSshPublicKey(BaseEncoding.base64().encode(readPublicTempSshKey(stackId).getBytes()));
            securityConfig.setSaltSignPublicKey(BaseEncoding.base64().encode(readPublicSaltSignKey(stackId).getBytes()));
            securityConfig.setSaltSignPrivateKey(BaseEncoding.base64().encode(readPrivateSaltSignKey(stackId).getBytes()));
            return securityConfig;
        } catch (IOException | JSchException e) {
            throw new CloudbreakSecuritySetupException("Failed to generate temporary SSH key pair.", e);
        }
    }

    public String createServerCertDir(Long stackId, InstanceMetaData gatewayInstance) throws CloudbreakSecuritySetupException {
        String serverCertDir = getCertDir(stackId, gatewayInstance);
        Path serverCertPath = Paths.get(serverCertDir);
        if (!Files.exists(serverCertPath)) {
            try {
                Files.createDirectories(serverCertPath);
            } catch (IOException | SecurityException se) {
                throw new CloudbreakSecuritySetupException("Failed to create directory: " + serverCertPath.toString());
            }
        }
        return serverCertDir;
    }

    public String getCertDir(Long stackId) {
        return getCertDir(stackId, null);
    }

    public String getCertDir(Long stackId, InstanceMetaData gatewayInstance) {
        if (gatewayInstance != null) {
            return Paths.get(certDir + "/stack-" + stackId + "/gw-" + gatewayInstance.getId()).toString();
        }
        return Paths.get(certDir + "/stack-" + stackId).toString();
    }

    public String prepareCertDir(Long stackId) throws CloudbreakSecuritySetupException {
        return prepareCertDir(stackId, null);
    }

    private String prepareCertDir(Long stackId, InstanceMetaData gatewayInstance) throws CloudbreakSecuritySetupException {
        Path stackCertDir = Paths.get(getCertDir(stackId, gatewayInstance));
        if (!Files.exists(stackCertDir)) {
            try {
                LOGGER.info("Creating directory for the keys and certificates under {}", certDir);
                Files.createDirectories(stackCertDir);
                prepareFiles(stackId, gatewayInstance);
            } catch (IOException | SecurityException se) {
                throw new CloudbreakSecuritySetupException("Failed to create directory: " + stackCertDir);
            }
        } else {
            prepareFiles(stackId, gatewayInstance);
        }
        return stackCertDir.toString();
    }

    private void prepareFiles(Long stackId, InstanceMetaData gatewayInstance) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdWithSecurityConfig(stackId);
        if (stack != null && stack.getSecurityConfig() != null) {
            Long id = stack.getId();
            // In case of byos there is no server machine
            if (!BYOS.equals(stack.getCredential().cloudPlatform())) {
                if (gatewayInstance != null) {
                    readServerCert(id, gatewayInstance);
                }
            }
            readClientCert(id);
            readClientKey(id);
            readPrivateTempSshKey(id);
            readPublicTempSshKey(id);
            readPrivateSaltSignKey(id);
            readPublicSaltSignKey(id);
        }
    }

    public String getSshPrivateFileLocation(Long stackId) {
        return Paths.get(getCertDir(stackId) + "/" + getPrivateSshKeyFileName(stackId)).toString();
    }

    private String readSecurityFile(Long stackId, String fileName) throws CloudbreakSecuritySetupException {
        return readSecurityFile(stackId, null, fileName);
    }

    private String readSecurityFile(Long stackId, InstanceMetaData gatewayInstance, String fileName) throws CloudbreakSecuritySetupException {
        try {
            return FileReaderUtils.readFileFromPath(Paths.get(getCertDir(stackId, gatewayInstance) + "/" + fileName));
        } catch (IOException | SecurityException se) {
            throw new CloudbreakSecuritySetupException("Failed to read file: " + getCertDir(stackId, gatewayInstance) + "/" + fileName);
        }
    }

    private void writeSecurityFile(Long stackId, String content, String fileName) throws CloudbreakSecuritySetupException {
        writeSecurityFile(stackId, null, content, fileName);
    }

    private void writeSecurityFile(Long stackId, InstanceMetaData gatewayInstance, String content, String fileName) throws CloudbreakSecuritySetupException {
        try {
            String path = Paths.get(getCertDir(stackId, gatewayInstance) + "/" + fileName).toString();
            File directory = new File(getCertDir(stackId, gatewayInstance));
            if (!directory.exists()) {
                Files.createDirectories(Paths.get(getCertDir(stackId, gatewayInstance)));
            }
            File file = new File(path);
            if (!file.exists()) {
                if (content != null) {
                    try (FileOutputStream output = new FileOutputStream(file)) {
                        IOUtils.write(Base64.decodeBase64(content), output);
                    }
                }
            }
        } catch (IOException | SecurityException se) {
            throw new CloudbreakSecuritySetupException("Failed to write file: " + getCertDir(stackId, gatewayInstance) + "/" + fileName);
        }
    }

    private boolean checkSecurityFileExist(Long stackId, String fileName) throws CloudbreakSecuritySetupException {
        return checkSecurityFileExist(stackId, null, fileName);
    }

    private boolean checkSecurityFileExist(Long stackId, InstanceMetaData gatewayInstance, String fileName) throws CloudbreakSecuritySetupException {
        try {
            String path = Paths.get(getCertDir(stackId, gatewayInstance) + "/" + fileName).toString();
            File directory = new File(getCertDir(stackId, gatewayInstance));
            if (!directory.exists()) {
                return false;
            }
            File file = new File(path);
            if (!file.exists()) {
                return false;
            }
        } catch (SecurityException se) {
            throw new CloudbreakSecuritySetupException("Failed to check file: " + getCertDir(stackId, gatewayInstance) + "/" + fileName);
        }
        return true;
    }

    public void copyClientKeys(Long stackId) throws CloudbreakSecuritySetupException {
        try {
            Path stackCertDir = Paths.get(getCertDir(stackId));
            File file = new File(stackCertDir.toString());
            if (!file.exists()) {
                Files.createDirectories(stackCertDir);
            }
            Files.copy(Paths.get(clientPrivateKey), Paths.get(stackCertDir + "/key.pem"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get(clientCert), Paths.get(stackCertDir + "/cert.pem"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CloudbreakSecuritySetupException(String.format("Failed to copy client certificate to certificate directory."
                    + " Check if '%s' and '%s' exist.", clientCert, clientPrivateKey), e);
        }
    }

    public String getPublicSaltSignKeyFileName(Long stackId) {
        return SALT_SIGN_KEY_PREFIX + stackId + PUBLIC_KEY_EXTENSION;
    }

    public String getPrivateSaltSignKeyFileName(Long stackId) {
        return SALT_SIGN_KEY_PREFIX + stackId;
    }

    public String getPublicSshKeyFileName(Long stackId) {
        return SSH_KEY_PREFIX + stackId + PUBLIC_KEY_EXTENSION;
    }

    public String getPrivateSshKeyFileName(Long stackId) {
        return SSH_KEY_PREFIX + stackId;
    }

    public String generateTempSshKeypair(Long stackId) throws JSchException, IOException {
        return generateSshKeypair(stackId, getPublicSshKeyFileName(stackId), getPrivateSshKeyFileName(stackId));
    }

    public String generateSaltSignKeypair(Long stackId) throws JSchException, IOException {
        return generateSshKeypair(stackId, getPublicSaltSignKeyFileName(stackId), getPrivateSaltSignKeyFileName(stackId));
    }

    private String generateSshKeypair(Long stackId, String publicKeyName, String privateKeyName) throws JSchException, IOException {
        LOGGER.info("Generating temporary SSH keypair.");
        String publicKeyPath = getCertDir(stackId) + publicKeyName;
        String privateKeyPath = getCertDir(stackId) + privateKeyName;
        JSch jsch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, DEFAULT_KEY_SIZE);
        keyPair.writePrivateKey(privateKeyPath);
        keyPair.writePublicKey(publicKeyPath, SSH_PUBLIC_KEY_COMMENT);
        keyPair.dispose();
        LOGGER.info("Generated temporary SSH keypair: {}. Fingerprint: {}", privateKeyPath, keyPair.getFingerPrint());
        return privateKeyPath;
    }

    public GatewayConfig buildGatewayConfig(Long stackId, InstanceMetaData gatewayInstance, Integer gatewayPort,
            SaltClientConfig saltClientConfig, Boolean knoxGatewayEnabled) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        prepareCertDir(stackId, gatewayInstance);
        String connectionIp = getGatewayIp(stack, gatewayInstance);
        HttpClientConfig conf = buildTLSClientConfig(stackId, connectionIp, gatewayInstance);
        String saltSignKey = conf.getCertDir() + SALT_SIGN_KEY_PREFIX + stackId;
        return new GatewayConfig(connectionIp, gatewayInstance.getPublicIpWrapper(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN(),
                gatewayPort, conf.getCertDir(), conf.getServerCert(), conf.getClientCert(),
                conf.getClientKey(), saltClientConfig.getSaltPassword(), saltClientConfig.getSaltBootPassword(), saltClientConfig.getSignatureKeyPem(),
                knoxGatewayEnabled, InstanceMetadataType.GATEWAY_PRIMARY.equals(gatewayInstance.getInstanceMetadataType()),
                saltSignKey, saltSignKey + PUBLIC_KEY_EXTENSION);
    }

    public String getGatewayIp(Stack stack, InstanceMetaData gatewayInstance) {
        String gatewayIP = gatewayInstance.getPublicIpWrapper();
        if (stack.getSecurityConfig().usePrivateIpToTls()) {
            gatewayIP = gatewayInstance.getPrivateIp();
        }
        return gatewayIP;
    }

    public HttpClientConfig buildTLSClientConfigForPrimaryGateway(Long stackId, String apiAddress) throws CloudbreakSecuritySetupException {
        return buildTLSClientConfig(stackId, apiAddress, stackRepository.findOneWithLists(stackId).getPrimaryGatewayInstance());
    }

    public HttpClientConfig buildTLSClientConfig(Long stackId, String apiAddress, InstanceMetaData gateway) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        if (!BYOS.equals(stack.cloudPlatform())) {
            prepareCertDir(stackId, gateway);
            return new HttpClientConfig(apiAddress, stack.getGatewayPort(), prepareCertDir(stackId), prepareCertDir(stackId, gateway));
        } else {
            return new HttpClientConfig(apiAddress, stack.getGatewayPort());
        }
    }

    public String readClientKey(Long stackId) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdWithSecurityConfig(stackId);
        if (!checkSecurityFileExist(stackId, "key.pem")) {
            writeSecurityFile(stackId, stack.getSecurityConfig().getClientKey(), "key.pem");
        }
        return readSecurityFile(stackId, "key.pem");
    }

    public String readClientCert(Long stackId) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdWithSecurityConfig(stackId);
        if (!checkSecurityFileExist(stackId, "cert.pem")) {
            writeSecurityFile(stackId, stack.getSecurityConfig().getClientCert(), "cert.pem");
        }
        return readSecurityFile(stackId, "cert.pem");
    }

    public String readServerCert(Long stackId, InstanceMetaData gwInstance) throws CloudbreakSecuritySetupException {
        if (!checkSecurityFileExist(stackId, gwInstance, "ca.pem")) {
            writeSecurityFile(stackId, gwInstance, gwInstance.getServerCert(), "ca.pem");
        }
        return readSecurityFile(stackId, gwInstance, "ca.pem");
    }

    public String readPrivateSaltSignKey(Long stackId) throws CloudbreakSecuritySetupException {
        return readPrivateSaltSignKey(stackId, getPrivateSaltSignKeyFileName(stackId));
    }

    public String readPrivateTempSshKey(Long stackId) throws CloudbreakSecuritySetupException {
        return readPrivateSshKey(stackId, getPrivateSshKeyFileName(stackId));
    }

    private String readPrivateSaltSignKey(Long stackId, String keyName) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdWithSecurityConfig(stackId);
        if (stack.getSecurityConfig() == null) {
            return readSecurityFile(stackId, keyName);
        }
        String key = stack.getSecurityConfig().getSaltSignPrivateKey();
        if (key != null) {
            if (!checkSecurityFileExist(stackId, keyName)) {
                writeSecurityFile(stackId, key, keyName);
            }
            return readSecurityFile(stackId, keyName);
        }
        return null;
    }

    private String readPrivateSshKey(Long stackId, String keyName) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdWithSecurityConfig(stackId);
        if (!checkSecurityFileExist(stackId, keyName)) {
            writeSecurityFile(stackId, stack.getSecurityConfig().getCloudbreakSshPrivateKey(), keyName);
        }
        return readSecurityFile(stackId, keyName);
    }

    public String readPublicSaltSignKey(Long stackId) throws CloudbreakSecuritySetupException {
        return readPublicSaltSignKey(stackId, getPublicSaltSignKeyFileName(stackId));
    }

    public String readPublicTempSshKey(Long stackId) throws CloudbreakSecuritySetupException {
        return readPublicSshKey(stackId, getPublicSshKeyFileName(stackId));
    }

    private String readPublicSshKey(Long stackId, String sshKeyName) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdWithSecurityConfig(stackId);
        if (!checkSecurityFileExist(stackId, sshKeyName)) {
            writeSecurityFile(stackId, stack.getSecurityConfig().getCloudbreakSshPublicKey(), sshKeyName);
        }
        return readSecurityFile(stackId, sshKeyName);
    }

    private String readPublicSaltSignKey(Long stackId, String keyName) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdWithSecurityConfig(stackId);
        if (stack.getSecurityConfig() == null) {
            return readSecurityFile(stackId, keyName);
        }
        String key = stack.getSecurityConfig().getSaltSignPublicKey();
        if (key != null) {
            if (!checkSecurityFileExist(stackId, keyName)) {
                writeSecurityFile(stackId, key, keyName);
            }
            return readSecurityFile(stackId, keyName);
        }
        return null;
    }

    public byte[] getCertificate(Long id) {
        String cert = instanceMetaDataRepository.getServerCertByStackId(id);
        if (cert == null) {
            throw new NotFoundException("Stack doesn't exist, or certificate was not found for stack.");
        }
        return Base64.decodeBase64(cert);
    }
}
