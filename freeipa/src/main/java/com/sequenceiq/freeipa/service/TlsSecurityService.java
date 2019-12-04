package com.sequenceiq.freeipa.service;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import javax.inject.Inject;

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
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class TlsSecurityService {

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private StackService stackService;

    public SecurityConfig generateSecurityKeys(String accountId) {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setAccountId(accountId);
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

        String clientKeyEncoded = BaseEncoding.base64().encode(clientPrivateKey.getBytes());
        securityConfig.setClientKey(clientKeyEncoded);
        securityConfig.setClientKeyVault(clientKeyEncoded);
        String clientCertEncoded = BaseEncoding.base64().encode(clientCert.getBytes());
        securityConfig.setClientCert(clientCertEncoded);
        securityConfig.setClientCertVault(clientCertEncoded);
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
        Stack stack = stackService.getStackById(stackId);
        SecurityConfig securityConfig = securityConfigService.findOneByStack(stack);
        String connectionIp = getGatewayIp(securityConfig, gatewayInstance, stack);
        HttpClientConfig conf = buildTLSClientConfig(stack, connectionIp, gatewayInstance);
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String saltSignPrivateKeyB64 = saltSecurityConfig.getSaltSignPrivateKey();
        GatewayConfig gatewayConfig =
                new GatewayConfig(connectionIp, gatewayInstance.getPublicIpWrapper(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN(),
                        getGatewayPort(gatewayPort, stack), gatewayInstance.getInstanceId(), conf.getServerCert(),
                        conf.getClientCert(), conf.getClientKey(), saltClientConfig.getSaltPassword(), saltClientConfig.getSaltBootPassword(),
                        saltClientConfig.getSignatureKeyPem(), knoxGatewayEnabled,
                        InstanceMetadataType.GATEWAY_PRIMARY.equals(gatewayInstance.getInstanceMetadataType()),
                        new String(decodeBase64(saltSignPrivateKeyB64)), new String(decodeBase64(saltSecurityConfig.getSaltSignPublicKey())),
                        null, null);
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            gatewayConfig
                    .withPath(clusterProxyService.getProxyPath(stack.getResourceCrn()))
                    .withProtocol(clusterProxyConfiguration.getClusterProxyProtocol());
        }
        return gatewayConfig;
    }

    public String getGatewayIp(SecurityConfig securityConfig, InstanceMetaData gatewayInstance, Stack stack) {
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

    public HttpClientConfig buildTLSClientConfigForPrimaryGateway(Stack stack, String apiAddress) {
        InstanceMetaData primaryGateway = instanceMetaDataRepository.getPrimaryGatewayInstanceMetadata(stack.getId());
        return buildTLSClientConfig(stack, apiAddress, primaryGateway);
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
