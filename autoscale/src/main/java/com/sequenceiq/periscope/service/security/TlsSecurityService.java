package com.sequenceiq.periscope.service.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.CloudbreakClient;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.repository.SecurityConfigRepository;

@Service
public class TlsSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSecurityService.class);
    private static final String CERT_DIR = "/certs";
    private static final String KEY_FILE = "key.pem";
    private static final String CERT_FILE = "cert.pem";
    private static final String SERVER_CERT_FILE = "ca.pem";
    private static final String DIR_PREFIX = "/stack-";

    @Value("${periscope.cert.dir:" + CERT_DIR + "}")
    private String certDir;
    @Value("#{'${periscope.cert.dir:" + CERT_DIR + "}' + '/' + '${periscope.tls.cert.file:client.pem}'}")
    private String clientCertName;
    @Value("#{'${periscope.cert.dir:" + CERT_DIR + "}' + '/' + '${periscope.tls.private.key.file:client-key.pem}'}")
    private String clientPrivateKeyName;

    @Autowired
    private CloudbreakClient cloudbreakClient;
    @Autowired
    private SecurityConfigRepository securityConfigRepository;

    public SecurityConfig prepareSecurityConfig(Long stackId) {
        Path stackCertDir = getCertDir(stackId);
        if (!Files.exists(stackCertDir)) {
            try {
                LOGGER.info("Creating directory for the certificates: {}", stackCertDir);
                Files.createDirectory(stackCertDir);
            } catch (IOException e) {
                throw new TlsConfigurationException("Failed to create directory " + stackCertDir, e);
            }
        }
        Path clientKeyDst = stackCertDir.resolve(KEY_FILE);
        Path clientCertDst = stackCertDir.resolve(CERT_FILE);
        try {
            Files.copy(Paths.get(clientPrivateKeyName), clientKeyDst, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get(clientCertName), clientCertDst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new TlsConfigurationException("Failed to copy client certificate to " + stackCertDir, e);
        }

        byte[] serverCert;

        try {
            serverCert = cloudbreakClient.stackEndpoint().getCertificate(stackId).getCertificate();
            Files.write(stackCertDir.resolve(SERVER_CERT_FILE), serverCert, StandardOpenOption.CREATE);
        } catch (Exception e) {
            throw new TlsConfigurationException("Failed to write server certificate to " + stackCertDir, e);
        }

        byte[] clientKey;
        byte[] clientCert;

        try {
            clientKey = Files.readAllBytes(clientKeyDst);
            clientCert = Files.readAllBytes(clientCertDst);
        } catch (IOException e) {
            throw new TlsConfigurationException("Failed to read client certificate file from " + stackCertDir, e);
        }

        return new SecurityConfig(clientKey, clientCert, serverCert);
    }

    public TlsConfiguration getConfiguration(Cluster cluster) {
        Path certDir = getCertDir(cluster.getStackId());
        Path clientKeyPath = certDir.resolve(KEY_FILE);
        Path clientCertPath = certDir.resolve(CERT_FILE);
        Path serverCertPath = certDir.resolve(SERVER_CERT_FILE);
        try {
            if (!Files.exists(certDir)) {
                LOGGER.info("Recreating certificate directory [{}] because it doesn't exist.", certDir);
                Files.createDirectory(certDir);
            }
            if (!Files.exists(clientKeyPath) || !Files.exists(clientCertPath) || !Files.exists(serverCertPath)) {
                LOGGER.info("Recreating certificate files in {} because they don't exist.", certDir);
                SecurityConfig securityConfig = securityConfigRepository.findByClusterId(cluster.getId());
                Files.write(clientKeyPath, securityConfig.getClientKey(), StandardOpenOption.CREATE);
                Files.write(clientCertPath, securityConfig.getClientCert(), StandardOpenOption.CREATE);
                Files.write(serverCertPath, securityConfig.getServerCert(), StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            throw new TlsConfigurationException("Failed to write certificates to file.", e);
        }
        return new TlsConfiguration(clientKeyPath.toString(), clientCertPath.toString(), serverCertPath.toString());
    }

    private Path getCertDir(Long stackId) {
        return Paths.get(certDir + DIR_PREFIX + stackId);
    }
}
