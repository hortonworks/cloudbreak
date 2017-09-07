package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;
import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyPair;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceMetadataType;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class TlsSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSecurityService.class);

    @Value("#{'${cb.cert.dir:}/${cb.tls.cert.file:}'}")
    private String clientCert;

    @Value("#{'${cb.cert.dir:}/${cb.tls.private.key.file:}'}")
    private String clientPrivateKey;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public SecurityConfig storeSSHKeys() throws CloudbreakSecuritySetupException {
        SecurityConfig securityConfig = new SecurityConfig();
        copyClientKeys(securityConfig);
        generateTempSshKeypair(securityConfig);
        generateSaltSignKeypair(securityConfig);
        return securityConfig;
    }

    private void copyClientKeys(SecurityConfig securityConfig) throws CloudbreakSecuritySetupException {
        try {
            securityConfig.setClientKey(BaseEncoding.base64().encode(FileReaderUtils.readFileFromPath(Paths.get(clientPrivateKey)).getBytes()));
            securityConfig.setClientCert(BaseEncoding.base64().encode(FileReaderUtils.readFileFromPath(Paths.get(clientCert)).getBytes()));
        } catch (IOException e) {
            throw new CloudbreakSecuritySetupException(String.format("Failed to copy client certificate."
                    + " Check if '%s' and '%s' exist.", clientCert, clientPrivateKey), e);
        }
    }

    public void generateTempSshKeypair(SecurityConfig securityConfig) {
        KeyPair keyPair = PkiUtil.generateKeypair();
        String privateKey = PkiUtil.convert(keyPair.getPrivate());
        String publicKey = PkiUtil.convertOpenSshPublicKey(keyPair.getPublic());
        securityConfig.setCloudbreakSshPublicKey(BaseEncoding.base64().encode(publicKey.getBytes()));
        securityConfig.setCloudbreakSshPrivateKey(BaseEncoding.base64().encode(privateKey.getBytes()));
    }

    public void generateSaltSignKeypair(SecurityConfig securityConfig) {
        KeyPair keyPair = PkiUtil.generateKeypair();
        String privateKey = PkiUtil.convert(keyPair.getPrivate());
        String publicKey = PkiUtil.convertOpenSshPublicKey(keyPair.getPublic());
        securityConfig.setSaltSignPublicKey(BaseEncoding.base64().encode(publicKey.getBytes()));
        securityConfig.setSaltSignPrivateKey(BaseEncoding.base64().encode(privateKey.getBytes()));
    }

    public GatewayConfig buildGatewayConfig(Long stackId, InstanceMetaData gatewayInstance, Integer gatewayPort,
            SaltClientConfig saltClientConfig, Boolean knoxGatewayEnabled) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        SecurityConfig securityConfig = stack.getSecurityConfig();
        String connectionIp = getGatewayIp(stack, gatewayInstance);
        HttpClientConfig conf = buildTLSClientConfig(stackId, connectionIp, gatewayInstance);
        return new GatewayConfig(connectionIp, gatewayInstance.getPublicIpWrapper(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN(),
                gatewayPort, conf.getServerCert(), conf.getClientCert(), conf.getClientKey(),
                saltClientConfig.getSaltPassword(), saltClientConfig.getSaltBootPassword(), saltClientConfig.getSignatureKeyPem(),
                knoxGatewayEnabled, InstanceMetadataType.GATEWAY_PRIMARY.equals(gatewayInstance.getInstanceMetadataType()),
                securityConfig.getSaltSignPrivateKeyDecoded(), securityConfig.getSaltSignPublicKeyDecoded());
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

    public HttpClientConfig buildTLSClientConfig(Long stackId, String apiAddress, InstanceMetaData gateway) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        if (!BYOS.equals(stack.cloudPlatform())) {
            SecurityConfig securityConfig = stack.getSecurityConfig();
            return new HttpClientConfig(apiAddress,
                    gateway.getServerCert(), securityConfig.getClientCertDecoded(), securityConfig.getClientKeyDecoded());
        } else {
            return new HttpClientConfig(apiAddress);
        }
    }

    public CertificateResponse getCertificates(Long stackId) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        if (stack == null) {
            throw new NotFoundException("Stack doesn't exist.");
        }
        String serverCert = instanceMetaDataRepository.getServerCertByStackId(stackId);
        if (serverCert == null) {
            throw new NotFoundException("Server certificate was not found.");
        }
        SecurityConfig securityConfig = stack.getSecurityConfig();
        return new CertificateResponse(decodeBase64(serverCert),
                securityConfig.getClientKeyDecoded().getBytes(), securityConfig.getClientCertDecoded().getBytes());
    }

}
