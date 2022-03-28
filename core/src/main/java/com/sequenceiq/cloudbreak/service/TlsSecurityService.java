package com.sequenceiq.cloudbreak.service;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.FixedSizePreloadCache;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class TlsSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSecurityService.class);

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterProxyEnablementService clusterProxyEnablementService;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Value("${cb.security.keypair.cache.size:10}")
    private int keyPairCacheSize;

    private FixedSizePreloadCache<KeyPair> keyPairCache;

    @PostConstruct
    public void init() {
        keyPairCache = new FixedSizePreloadCache<>(keyPairCacheSize, PkiUtil::generateKeypair);
    }

    @Measure(TlsSecurityService.class)
    public SecurityConfig generateSecurityKeys(Workspace workspace) {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setWorkspace(workspace);
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setWorkspace(workspace);
        saltSecurityConfig.setSaltBootPassword(PasswordUtil.generatePassword());
        saltSecurityConfig.setSaltPassword(PasswordUtil.generatePassword());
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);

        setClientKeys(securityConfig, keyPairCache.pop(), keyPairCache.pop());
        setSaltBootSignKeypair(saltSecurityConfig, convertKeyPair(keyPairCache.pop()));
        setSaltSignKeypair(securityConfig, convertKeyPair(keyPairCache.pop()));

        return securityConfig;
    }

    private Pair<String, String> convertKeyPair(KeyPair keyPair) {
        String privateKey = PkiUtil.convert(keyPair.getPrivate());
        String publicKey = PkiUtil.convertOpenSshPublicKey(keyPair.getPublic());
        return new ImmutablePair<>(privateKey, publicKey);
    }

    private void setClientKeys(SecurityConfig securityConfig, KeyPair identity, KeyPair signKey) {
        X509Certificate cert = PkiUtil.cert(identity, "cloudbreak", signKey);

        String clientPrivateKey = PkiUtil.convert(identity.getPrivate());
        String clientCert = PkiUtil.convert(cert);

        securityConfig.setClientKey(BaseEncoding.base64().encode(clientPrivateKey.getBytes()));
        securityConfig.setClientCert(BaseEncoding.base64().encode(clientCert.getBytes()));
    }

    private void setSaltBootSignKeypair(SaltSecurityConfig saltSecurityConfig, Pair<String, String> keyPair) {
        saltSecurityConfig.setSaltBootSignPublicKey(BaseEncoding.base64().encode(keyPair.getValue().getBytes()));
        saltSecurityConfig.setSaltBootSignPrivateKey(BaseEncoding.base64().encode(keyPair.getKey().getBytes()));
    }

    private void setSaltSignKeypair(SecurityConfig securityConfig, Pair<String, String> keyPair) {
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        saltSecurityConfig.setSaltSignPublicKey(BaseEncoding.base64().encode(keyPair.getValue().getBytes()));
        saltSecurityConfig.setSaltSignPrivateKey(BaseEncoding.base64().encode(keyPair.getKey().getBytes()));
    }

    public GatewayConfig buildGatewayConfig(Stack stack, InstanceMetaData gatewayInstance, Integer gatewayPort,
            SaltClientConfig saltClientConfig, Boolean knoxGatewayEnabled) {
        Long stackId = stack.getId();
        LOGGER.info("Build gateway config for stack with id: {}, gatewayInstance: {}, gatewayPort: {}, knoxGatewayEnabled: {}",
                stackId, gatewayInstance, gatewayPort, knoxGatewayEnabled);
        SecurityConfig securityConfig = getSecurityConfigByStackIdOrThrowNotFound(stackId);
        String connectionIp = getGatewayIp(securityConfig, gatewayInstance, stack);
        HttpClientConfig conf = buildTLSClientConfig(stackId, stack.getCloudPlatform(), connectionIp, gatewayInstance);
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String saltSignPrivateKeyB64 = saltSecurityConfig.getSaltSignPrivateKey();
        GatewayConfig gatewayConfig = new GatewayConfig(connectionIp, gatewayInstance.getPublicIpWrapper(), gatewayInstance.getPrivateIp(),
                gatewayInstance.getDiscoveryFQDN(), getGatewayPort(gatewayPort, stack), gatewayInstance.getInstanceId(),
                conf.getServerCert(), conf.getClientCert(), conf.getClientKey(), saltClientConfig.getSaltPassword(), saltClientConfig.getSaltBootPassword(),
                saltClientConfig.getSignatureKeyPem(), knoxGatewayEnabled,
                InstanceMetadataType.GATEWAY_PRIMARY.equals(gatewayInstance.getInstanceMetadataType()), new String(decodeBase64(saltSignPrivateKeyB64)),
                new String(decodeBase64(saltSecurityConfig.getSaltSignPublicKey())), securityConfig.getUserFacingCert(), securityConfig.getUserFacingKey());
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            LOGGER.info("Create config for cluster proxy");
            gatewayConfig
                    .withPath(clusterProxyService.getProxyPath(stack.getResourceCrn(), gatewayInstance.getInstanceId()))
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

    public HttpClientConfig buildTLSClientConfigForPrimaryGateway(Long stackId, String apiAddress, String cloudPlatform) {
        InstanceMetaData primaryGateway = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stackId).orElse(null);
        return buildTLSClientConfig(stackId, cloudPlatform, apiAddress, primaryGateway);
    }

    public HttpClientConfig buildTLSClientConfig(Long stackId, String cloudPlatform, String apiAddress, InstanceMetaData gateway) {
        Optional<SecurityConfig> securityConfig = securityConfigService.findOneByStackId(stackId);
        if (securityConfig.isEmpty()) {
            return decorateWithClusterProxyConfig(stackId, cloudPlatform, new HttpClientConfig(apiAddress));
        } else {
            LOGGER.info("Security config is not empty");
            String serverCert = gateway == null ? null : gateway.getServerCert() == null ? null : new String(decodeBase64(gateway.getServerCert()));
            String clientCertB64 = securityConfig.get().getClientCert();
            String clientKeyB64 = securityConfig.get().getClientKey();
            HttpClientConfig httpClientConfig = new HttpClientConfig(apiAddress, serverCert,
                    new String(decodeBase64(clientCertB64)), new String(decodeBase64(clientKeyB64)));
            return decorateWithClusterProxyConfig(stackId, cloudPlatform, httpClientConfig);
        }
    }

    private HttpClientConfig decorateWithClusterProxyConfig(Long stackId, String cloudPlatform, HttpClientConfig httpClientConfig) {
        LOGGER.info("Decorate with cluster proxy config");
        if (clusterProxyEnablementService.isClusterProxyApplicable(cloudPlatform)) {
            LOGGER.info("Cluster proxy is applicable");
            Stack stack = stackService.getById(stackId);
            if (clusterProxyService.useClusterProxyForCommunication(stack)) {
                LOGGER.info("Cluster proxy for communication");
                return httpClientConfig.withClusterProxy(clusterProxyConfiguration.getClusterProxyUrl(), stack.getResourceCrn());
            }
        }
        return httpClientConfig;
    }

    public CertificateV4Response getCertificates(Long stackId) {
        SecurityConfig securityConfig = getSecurityConfigByStackIdOrThrowNotFound(stackId);
        String serverCert = instanceMetaDataService.getServerCertByStackId(stackId)
                .orElse(null);
        return new CertificateV4Response(serverCert, securityConfig.getClientKeySecret(), securityConfig.getClientCertSecret());
    }

    private SecurityConfig getSecurityConfigByStackIdOrThrowNotFound(Long stackId) {
        return securityConfigService.findOneByStackId(stackId).orElseThrow(() -> new NotFoundException("Security config doesn't exist."));
    }

}
