package com.sequenceiq.cloudbreak.service;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceMetadataType;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;

@Component
public class TlsSecurityService {

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public SecurityConfig storeSSHKeys() {
        SecurityConfig securityConfig = new SecurityConfig();
        generateClientKeys(securityConfig);
        generateTempSshKeypair(securityConfig);
        generateSaltSignKeypair(securityConfig);
        return securityConfig;
    }

    private void generateClientKeys(SecurityConfig securityConfig) {
        KeyPair identity = PkiUtil.generateKeypair();
        KeyPair signKey = PkiUtil.generateKeypair();
        X509Certificate cert = PkiUtil.cert(identity, "cloudbreak", signKey);

        String clientPrivateKey = PkiUtil.convert(identity.getPrivate());
        String clientCert = PkiUtil.convert(cert);

        securityConfig.setClientKey(BaseEncoding.base64().encode(clientPrivateKey.getBytes()));
        securityConfig.setClientCert(BaseEncoding.base64().encode(clientCert.getBytes()));
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
        SaltClientConfig saltClientConfig, Boolean knoxGatewayEnabled) {
        SecurityConfig securityConfig = securityConfigRepository.findOneByStackId(stackId);
        String connectionIp = getGatewayIp(securityConfig, gatewayInstance);
        HttpClientConfig conf = buildTLSClientConfig(stackId, connectionIp, gatewayInstance);
        return new GatewayConfig(connectionIp, gatewayInstance.getPublicIpWrapper(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN(),
            gatewayPort, conf.getServerCert(), conf.getClientCert(), conf.getClientKey(),
            saltClientConfig.getSaltPassword(), saltClientConfig.getSaltBootPassword(), saltClientConfig.getSignatureKeyPem(),
            knoxGatewayEnabled, InstanceMetadataType.GATEWAY_PRIMARY.equals(gatewayInstance.getInstanceMetadataType()),
            securityConfig.getSaltSignPrivateKeyDecoded(), securityConfig.getSaltSignPublicKeyDecoded());
    }

    public String getGatewayIp(SecurityConfig securityConfig, InstanceMetaData gatewayInstance) {
        String gatewayIP = gatewayInstance.getPublicIpWrapper();
        if (securityConfig.usePrivateIpToTls()) {
            gatewayIP = gatewayInstance.getPrivateIp();
        }
        return gatewayIP;
    }

    public HttpClientConfig buildTLSClientConfigForPrimaryGateway(Long stackId, String apiAddress) {
        InstanceMetaData primaryGateway = instanceMetaDataRepository.getPrimaryGatewayInstanceMetadata(stackId);
        return buildTLSClientConfig(stackId, apiAddress, primaryGateway);
    }

    public HttpClientConfig buildTLSClientConfig(Long stackId, String apiAddress, InstanceMetaData gateway) {
        SecurityConfig securityConfig = securityConfigRepository.findOneByStackId(stackId);
        return securityConfig != null ? new HttpClientConfig(apiAddress,
                gateway.getServerCert(), securityConfig.getClientCertDecoded(), securityConfig.getClientKeyDecoded()) : new HttpClientConfig(apiAddress);
    }

    public CertificateResponse getCertificates(Long stackId) {
        SecurityConfig securityConfig = securityConfigRepository.findOneByStackId(stackId);
        if (securityConfig == null) {
            throw new NotFoundException("Security config doesn't exist.");
        }
        String serverCert = instanceMetaDataRepository.getServerCertByStackId(stackId);
        if (serverCert == null) {
            throw new NotFoundException("Server certificate was not found.");
        }
        return new CertificateResponse(decodeBase64(serverCert),
            securityConfig.getClientKeyDecoded().getBytes(), securityConfig.getClientCertDecoded().getBytes());
    }

}
