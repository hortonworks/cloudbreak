package com.sequenceiq.cloudbreak.service;

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
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class TlsSecurityService {

    public static final String SSH_PUBLIC_KEY_EXTENSION = ".pub";

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

    public void storeSSHKeys(Stack stack) throws CloudbreakSecuritySetupException {
        try {
            generateTempSshKeypair(stack.getId());
            SecurityConfig securityConfig = new SecurityConfig();
            securityConfig.setClientKey(BaseEncoding.base64().encode(readClientKey(stack.getId()).getBytes()));
            securityConfig.setClientCert(BaseEncoding.base64().encode(readClientCert(stack.getId()).getBytes()));
            securityConfig.setTemporarySshPrivateKey(BaseEncoding.base64().encode(readPrivateSshKey(stack.getId()).getBytes()));
            securityConfig.setTemporarySshPublicKey(BaseEncoding.base64().encode(readPublicSshKey(stack.getId()).getBytes()));
            securityConfig.setStack(stack);
            securityConfigRepository.save(securityConfig);
        } catch (IOException | JSchException e) {
            throw new CloudbreakSecuritySetupException("Failed to generate temporary SSH key pair.", e);
        }
    }

    public String getCertDir(Long stackId) {
        return Paths.get(certDir + "/stack-" + stackId).toString();
    }

    public String prepareCertDir(Long stackId) throws CloudbreakSecuritySetupException {
        Path stackCertDir = Paths.get(getCertDir(stackId));
        if (!Files.exists(stackCertDir)) {
            try {
                LOGGER.info("Creating directory for the keys and certificates under {}", certDir);
                Files.createDirectories(stackCertDir);
                prepareFiles(stackId);
            } catch (IOException | SecurityException se) {
                throw new CloudbreakSecuritySetupException("Failed to create directory: " + stackCertDir);
            }
        } else {
            prepareFiles(stackId);
        }
        return stackCertDir.toString();
    }

    private void prepareFiles(Long stackId) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdLazy(stackId);
        if (stack != null) {
            Long id = stack.getId();
            readServerCert(id);
            readClientCert(id);
            readClientKey(id);
            readPrivateSshKey(id);
            readPublicSshKey(id);
        }
    }

    public String getSshPrivateFileLocation(Long stackId) {
        return Paths.get(getCertDir(stackId) + "/" + getPrivateSshKeyFileName(stackId)).toString();
    }

    private String readSecurityFile(Long stackId, String fileName) throws CloudbreakSecuritySetupException {
        try {
            return FileReaderUtils.readFileFromPathToString(Paths.get(getCertDir(stackId) + "/" + fileName).toString());
        } catch (IOException | SecurityException se) {
            throw new CloudbreakSecuritySetupException("Failed to read file: " + getCertDir(stackId) + "/" + fileName);
        }
    }

    private void writeSecurityFile(Long stackId, String content, String fileName) throws CloudbreakSecuritySetupException {
        try {
            String path = Paths.get(getCertDir(stackId) + "/" + fileName).toString();
            File directory = new File(getCertDir(stackId));
            if (!directory.exists()) {
                Files.createDirectories(Paths.get(getCertDir(stackId)));
            }
            File file = new File(path);
            if (!file.exists()) {
                if (content != null) {
                    FileOutputStream output = new FileOutputStream(file);
                    IOUtils.write(Base64.decodeBase64(content), output);
                }
            }
        } catch (IOException | SecurityException se) {
            throw new CloudbreakSecuritySetupException("Failed to write file: " + getCertDir(stackId) + "/" + fileName);
        }
    }

    private boolean checkSecurityFileExist(Long stackId, String fileName) throws CloudbreakSecuritySetupException {
        try {
            String path = Paths.get(getCertDir(stackId) + "/" + fileName).toString();
            File directory = new File(getCertDir(stackId));
            if (!directory.exists()) {
                return false;
            }
            File file = new File(path);
            if (!file.exists()) {
                return false;
            }
        } catch (SecurityException se) {
            throw new CloudbreakSecuritySetupException("Failed to check file: " + getCertDir(stackId) + "/" + fileName);
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

    public String getPublicSshKeyFileName(Long stackId) {
        return SSH_KEY_PREFIX + stackId + SSH_PUBLIC_KEY_EXTENSION;
    }

    public String getPrivateSshKeyFileName(Long stackId) {
        return SSH_KEY_PREFIX + stackId;
    }

    public String generateTempSshKeypair(Long stackId) throws JSchException, IOException {
        LOGGER.info("Generating temporary SSH keypair.");
        String publicKeyPath = getCertDir(stackId) + getPublicSshKeyFileName(stackId);
        String privateKeyPath = getCertDir(stackId) + getPrivateSshKeyFileName(stackId);
        JSch jsch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, DEFAULT_KEY_SIZE);
        keyPair.writePrivateKey(privateKeyPath);
        keyPair.writePublicKey(publicKeyPath, SSH_PUBLIC_KEY_COMMENT);
        keyPair.dispose();
        LOGGER.info("Generated temporary SSH keypair: {}. Fingerprint: {}", privateKeyPath, keyPair.getFingerPrint());
        return privateKeyPath;
    }

    public GatewayConfig buildGatewayConfig(Long stackId, String publicIp, String privateIp) throws CloudbreakSecuritySetupException {
        prepareCertDir(stackId);
        return new GatewayConfig(publicIp, privateIp, prepareCertDir(stackId));
    }

    public HttpClientConfig buildTLSClientConfig(Long stackId, String apiAddress) throws CloudbreakSecuritySetupException {
        prepareCertDir(stackId);
        return new HttpClientConfig(apiAddress, prepareCertDir(stackId));
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

    public String readServerCert(Long stackId) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdWithSecurityConfig(stackId);
        if (!checkSecurityFileExist(stackId, "ca.pem")) {
            writeSecurityFile(stackId, stack.getSecurityConfig().getServerCert(), "ca.pem");
        }
        return readSecurityFile(stackId, "ca.pem");
    }


    public String readPrivateSshKey(Long stackId) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdWithSecurityConfig(stackId);
        if (!checkSecurityFileExist(stackId, getPrivateSshKeyFileName(stackId))) {
            writeSecurityFile(stackId, stack.getSecurityConfig().getTemporarySshPrivateKey(), getPrivateSshKeyFileName(stackId));
        }
        return readSecurityFile(stackId, getPrivateSshKeyFileName(stackId));
    }

    public String readPublicSshKey(Long stackId) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findByIdWithSecurityConfig(stackId);
        if (!checkSecurityFileExist(stackId, getPublicSshKeyFileName(stackId))) {
            writeSecurityFile(stackId, stack.getSecurityConfig().getTemporarySshPublicKey(), getPublicSshKeyFileName(stackId));
        }
        return readSecurityFile(stackId, getPublicSshKeyFileName(stackId));
    }

    public byte[] getCertificate(Long id) {
        String cert = securityConfigRepository.getServerCertByStackId(id);
        if (cert == null) {
            throw new NotFoundException("Stack doesn't exist, or certificate was not found for stack.");
        }
        return Base64.decodeBase64(cert);
    }

}
