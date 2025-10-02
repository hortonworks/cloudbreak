package com.sequenceiq.freeipa.service;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSecurityService.class);

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private ClusterProxyService clusterProxyService;

    public SecurityConfig generateSecurityKeys(String accountId, SecurityConfig securityConfig) {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setAccountId(accountId);
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        generateClientKeys(securityConfig);
        generateSaltBootSignKeypair(saltSecurityConfig);
        generateSaltSignKeypair(saltSecurityConfig);
        generateSaltMasterKeypair(saltSecurityConfig);
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
        saltSecurityConfig.setSaltBootSignPrivateKeyVault(PkiUtil.generatePemPrivateKeyInBase64());
    }

    private void generateSaltSignKeypair(SaltSecurityConfig saltSecurityConfig) {
        saltSecurityConfig.setSaltSignPrivateKeyVault(PkiUtil.generatePemPrivateKeyInBase64());
    }

    private void generateSaltMasterKeypair(SaltSecurityConfig saltSecurityConfig) {
        saltSecurityConfig.setSaltMasterPrivateKeyVault(PkiUtil.generatePemPrivateKeyInBase64());
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
        String saltMasterPrivateKey = saltSecurityConfig.getSaltMasterPrivateKeyVault() != null
                ? new String(decodeBase64(saltSecurityConfig.getSaltMasterPrivateKeyVault())) : null;
        String saltMasterPublicKey = saltSecurityConfig.getSaltMasterPublicKey() != null
                ? new String(decodeBase64(saltSecurityConfig.getSaltMasterPublicKey())) : null;
        GatewayConfig gatewayConfig =
                new GatewayConfig(connectionIp, gatewayInstance.getPublicIpWrapper(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN(),
                        getGatewayPort(stack.getGatewayport(), stack), gatewayInstance.getInstanceId(), conf.getServerCert(),
                        conf.getClientCert(), conf.getClientKey(), saltClientConfig.getSaltPassword(), saltClientConfig.getSaltBootPassword(),
                        saltClientConfig.getSignatureKeyPem(), knoxGatewayEnabled,
                        InstanceMetadataType.GATEWAY_PRIMARY.equals(gatewayInstance.getInstanceMetadataType()),
                        saltMasterPrivateKey, saltMasterPublicKey,
                        new String(decodeBase64(saltSignPrivateKeyB64)), new String(decodeBase64(saltSecurityConfig.getSaltSignPublicKey())),
                        null, null, null, null);
        gatewayConfig.withSaltVersion(getInstanceSaltVersion(gatewayInstance));

        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            gatewayConfig
                    .withPath(clusterProxyService.getProxyPathPgwAsFallBack(stack, gatewayInstance.getDiscoveryFQDN()))
                    .withProtocol(clusterProxyConfiguration.getClusterProxyProtocol());
        }
        return gatewayConfig;
    }

    public String getInstanceSaltVersion(InstanceMetaData instanceMetadata) {
        try {
            if (instanceMetadata.getImage() != null) {
                return instanceMetadata.getImage().get(Image.class).getPackageVersion(ImagePackageVersion.SALT);
            }
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.warn("Missing image information for instance: " + instanceMetadata.getInstanceId(), e);
        }
        return null;
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
