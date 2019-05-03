package com.sequenceiq.freeipa.service;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.repository.SecurityConfigRepository;

@Component
public class TlsSecurityService {

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    public SecurityConfig generateSecurityKeys() {
        SecurityConfig securityConfig = new SecurityConfig();
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        generateClientKeys(securityConfig);
        generateSaltBootSignKeypair(saltSecurityConfig);
        generateSaltSignKeypair(securityConfig);
        generateSaltPassword(saltSecurityConfig);
        generateSaltBootPassword(saltSecurityConfig);
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

    private void generateSaltBootSignKeypair(SaltSecurityConfig saltSecurityConfig) {
        KeyPair keyPair = PkiUtil.generateKeypair();
        String privateKey = PkiUtil.convert(keyPair.getPrivate());
        String publicKey = PkiUtil.convertOpenSshPublicKey(keyPair.getPublic());
        saltSecurityConfig.setSaltBootSignPublicKey(BaseEncoding.base64().encode(publicKey.getBytes()));
        saltSecurityConfig.setSaltBootSignPrivateKey(BaseEncoding.base64().encode(privateKey.getBytes()));
    }

    private void generateSaltSignKeypair(SecurityConfig securityConfig) {
        KeyPair keyPair = PkiUtil.generateKeypair();
        String privateKey = PkiUtil.convert(keyPair.getPrivate());
        String publicKey = PkiUtil.convertOpenSshPublicKey(keyPair.getPublic());
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        saltSecurityConfig.setSaltSignPublicKey(BaseEncoding.base64().encode(publicKey.getBytes()));
        saltSecurityConfig.setSaltSignPrivateKey(BaseEncoding.base64().encode(privateKey.getBytes()));
    }

    private void generateSaltBootPassword(SaltSecurityConfig saltSecurityConfig) {
        saltSecurityConfig.setSaltBootPassword(PasswordUtil.generatePassword());
    }

    private void generateSaltPassword(SaltSecurityConfig saltSecurityConfig) {
        saltSecurityConfig.setSaltPassword(PasswordUtil.generatePassword());
    }

    public GatewayConfig buildGatewayConfig(Long stackId, InstanceMetaData gatewayInstance, Integer gatewayPort,
            SaltClientConfig saltClientConfig, Boolean knoxGatewayEnabled) {
        SecurityConfig securityConfig = securityConfigRepository.findOneByStackId(stackId);
        String connectionIp = getGatewayIp(securityConfig, gatewayInstance);
        HttpClientConfig conf = buildTLSClientConfig(stackId, connectionIp, gatewayInstance);
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String saltSignPrivateKeyB64 = saltSecurityConfig.getSaltSignPrivateKey();
        return new GatewayConfig(connectionIp, gatewayInstance.getPublicIpWrapper(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN(),
                gatewayPort, conf.getServerCert(), conf.getClientCert(), conf.getClientKey(),
                saltClientConfig.getSaltPassword(), saltClientConfig.getSaltBootPassword(), saltClientConfig.getSignatureKeyPem(),
                knoxGatewayEnabled, InstanceMetadataType.GATEWAY_PRIMARY.equals(gatewayInstance.getInstanceMetadataType()),
                new String(decodeBase64(saltSignPrivateKeyB64)), new String(decodeBase64(saltSecurityConfig.getSaltSignPublicKey())));
    }

    public String getGatewayIp(SecurityConfig securityConfig, InstanceMetaData gatewayInstance) {
        String gatewayIP = gatewayInstance.getPublicIpWrapper();
        if (securityConfig.isUsePrivateIpToTls()) {
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
        if (securityConfig == null) {
            return new HttpClientConfig(apiAddress);
        } else {
            String serverCert = gateway == null ? null : gateway.getServerCert() == null ? null : new String(decodeBase64(gateway.getServerCert()));
            String clientCertB64 = securityConfig.getClientCert();
            String clientKeyB64 = securityConfig.getClientKey();
            return new HttpClientConfig(apiAddress, serverCert,
                    new String(decodeBase64(clientCertB64)), new String(decodeBase64(clientKeyB64)));
        }
    }

    public CertificateV4Response getCertificates(Long stackId) {
        SecurityConfig securityConfig = Optional.ofNullable(securityConfigRepository.findOneByStackId(stackId))
                .orElseThrow(() -> new NotFoundException("Security config doesn't exist."));
        String serverCert = Optional.ofNullable(instanceMetaDataRepository.getServerCertByStackId(stackId))
                .orElseThrow(() -> new NotFoundException("Server certificate was not found."));
        return new CertificateV4Response(serverCert, securityConfig.getClientKey(), securityConfig.getClientCert());
    }

}
