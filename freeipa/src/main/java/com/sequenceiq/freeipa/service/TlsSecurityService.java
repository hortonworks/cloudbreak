package com.sequenceiq.freeipa.service;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;

@Component
public class TlsSecurityService {

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private ClusterProxyService clusterProxyService;

    public SecurityConfig generateSecurityKeys(String accountId) {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setAccountId(accountId);
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setAccountId(accountId);
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

        String clientKeyEncoded = BaseEncoding.base64().encode(clientPrivateKey.getBytes());
        securityConfig.setClientKeyVault(clientKeyEncoded);
        String clientCertEncoded = BaseEncoding.base64().encode(clientCert.getBytes());
        securityConfig.setClientCertVault(clientCertEncoded);
    }

    private void generateSaltBootSignKeypair(SaltSecurityConfig saltSecurityConfig) {
        KeyPair keyPair = PkiUtil.generateKeypair();
        String privateKey = PkiUtil.convert(keyPair.getPrivate());
        String publicKey = PkiUtil.convertPemPublicKey(keyPair.getPublic());
        saltSecurityConfig.setSaltBootSignPublicKey(BaseEncoding.base64().encode(publicKey.getBytes()));
        String saltBootSignPrivateKey = BaseEncoding.base64().encode(privateKey.getBytes());
        saltSecurityConfig.setSaltBootSignPrivateKeyVault(saltBootSignPrivateKey);
    }

    private void generateSaltSignKeypair(SecurityConfig securityConfig) {
        KeyPair keyPair = PkiUtil.generateKeypair();
        String privateKey = PkiUtil.convert(keyPair.getPrivate());
        String publicKey = PkiUtil.convertPemPublicKey(keyPair.getPublic());
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        saltSecurityConfig.setSaltSignPublicKey(BaseEncoding.base64().encode(publicKey.getBytes()));
        String saltSignPrivateKey = BaseEncoding.base64().encode(privateKey.getBytes());
        saltSecurityConfig.setSaltSignPrivateKeyVault(saltSignPrivateKey);
    }

    private void generateSaltBootPassword(SaltSecurityConfig saltSecurityConfig) {
        String saltBootPassword = PasswordUtil.generatePassword();
        saltSecurityConfig.setSaltBootPasswordVault(saltBootPassword);
    }

    private void generateSaltPassword(SaltSecurityConfig saltSecurityConfig) {
        String saltPassword = PasswordUtil.generatePassword();
        saltSecurityConfig.setSaltPasswordVault(saltPassword);
    }

    public GatewayConfig buildGatewayConfig(Stack stack, InstanceMetaData gatewayInstance,
            SaltClientConfig saltClientConfig, Boolean knoxGatewayEnabled) {
        SecurityConfig securityConfig = securityConfigService.findOneByStack(stack);
        String connectionIp = getGatewayIp(securityConfig, gatewayInstance, stack);
        HttpClientConfig conf = buildTLSClientConfig(stack, connectionIp, gatewayInstance);
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String saltSignPrivateKeyB64 = saltSecurityConfig.getSaltSignPrivateKeyVault();
        GatewayConfig gatewayConfig =
                new GatewayConfig(connectionIp, gatewayInstance.getPublicIpWrapper(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN(),
                        getGatewayPort(stack.getGatewayport(), stack), gatewayInstance.getInstanceId(), conf.getServerCert(),
                        conf.getClientCert(), conf.getClientKey(), saltClientConfig.getSaltPassword(), saltClientConfig.getSaltBootPassword(),
                        saltClientConfig.getSignatureKeyPem(), knoxGatewayEnabled,
                        InstanceMetadataType.GATEWAY_PRIMARY.equals(gatewayInstance.getInstanceMetadataType()),
                        null, null,
                        new String(decodeBase64(saltSignPrivateKeyB64)), new String(decodeBase64(saltSecurityConfig.getSaltSignPublicKey())),
                        null, null);
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            gatewayConfig
                    .withPath(clusterProxyService.getProxyPathPgwAsFallBack(stack, gatewayInstance.getDiscoveryFQDN()))
                    .withProtocol(clusterProxyConfiguration.getClusterProxyProtocol());
        }
        return gatewayConfig;
    }

    private String getGatewayIp(SecurityConfig securityConfig, InstanceMetaData gatewayInstance, Stack stack) {
        String gatewayIP = gatewayInstance.getPublicIpWrapper();
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            gatewayIP = clusterProxyConfiguration.getClusterProxyHost();
        } else if (securityConfig.isUsePrivateIpToTls()) {
            gatewayIP = gatewayInstance.getPrivateIp();
        }
        return gatewayIP;
    }

    private Integer getGatewayPort(Integer stackPort, Stack stack) {
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            return clusterProxyConfiguration.getClusterProxyPort();
        } else {
            return stackPort;
        }
    }

    public HttpClientConfig buildTLSClientConfig(Stack stack, String apiAddress, InstanceMetaData gateway) {
        SecurityConfig securityConfig = securityConfigService.findOneByStack(stack);
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
}
