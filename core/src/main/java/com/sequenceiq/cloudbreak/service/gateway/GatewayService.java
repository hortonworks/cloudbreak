package com.sequenceiq.cloudbreak.service.gateway;

import java.security.KeyPair;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyCertificate;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxySecretProvider;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.repository.GatewayRepository;
import com.sequenceiq.cloudbreak.service.ClusterProxyRotationService;
import com.sequenceiq.cloudbreak.view.GatewayView;

@Service
public class GatewayService {

    @Value("${cb.https.port}")
    private String httpsPort;

    @Inject
    private GatewayRepository repository;

    @Inject
    private ClusterProxyRotationService clusterProxyRotationService;

    @Inject
    private ClusterProxySecretProvider clusterProxySecretProvider;

    public Gateway save(Gateway gateway) {
        gateway.setGatewayPort(Integer.valueOf(httpsPort));
        return repository.save(gateway);
    }

    public Optional<GatewayView> getByClusterId(Long clusterId) {
        return Optional.ofNullable(repository.findViewByClusterId(clusterId).orElse(null));
    }

    public Optional<Gateway> getById(Long id) {
        return repository.findById(id);
    }

    public void generateAndUpdateSignKeys(GatewayView gateway) {
        if (gateway != null) {
            if (gateway.getSignCert() == null) {
                Gateway existed = repository.findById(gateway.getId())
                        .map(this::generateSignKeys)
                        .orElseThrow(NotFoundException.notFound("Gateway should be exist"));
                save(existed);
            }
        }
    }

    public Gateway generateSignKeys(Gateway gateway) {
        ClusterProxyCertificate clusterProxyCertificate = clusterProxySecretProvider.generateSignKeys();

        gateway.setSignKey(clusterProxyCertificate.getSignKey());
        gateway.setSignPub(clusterProxyCertificate.getSignPub());
        gateway.setSignCert(clusterProxyCertificate.getSignCert());
        gateway.setTokenCert(clusterProxyCertificate.getSignTokenCert());
        //in case of service rollback we need this for another release
        gateway.setSignPubDeprecated(clusterProxyCertificate.getSignPub());
        gateway.setSignCertDeprecated(clusterProxyCertificate.getSignCert());
        return gateway;
    }

    public Gateway rotateGatewayKeys(Long currentGatewayId, GatewayView newGateway) {
        Optional<Gateway> gatewayOptional = repository.findById(currentGatewayId);
        if (gatewayOptional.isPresent()) {
            Gateway gateway = gatewayOptional.get();
            gateway.setSignKey(newGateway.getSignKey());
            gateway.setSignPub(newGateway.getSignPub());
            gateway.setSignCert(newGateway.getSignCert());
            gateway.setTokenCert(newGateway.getSignCert());
            //in case of service rollback we need this for another release
            gateway.setSignPubDeprecated(newGateway.getSignPub());
            gateway.setSignCertDeprecated(newGateway.getSignCert());

            return repository.save(gateway);
        }
        return null;
    }

    public GatewayView putLegacyFieldsIntoVaultIfNecessary(GatewayView gatewayView) {
        Gateway gateway = repository.findById(gatewayView.getId()).orElseThrow(NotFoundException.notFound("Gateway should exist"));
        if (gateway.getSignCertSecret() == null || gateway.getSignCertSecret().getRaw() == null) {
            gateway.setSignCert(gateway.getSignCertDeprecated());
        }
        if (gateway.getSignPubSecret() == null || gateway.getSignPubSecret().getRaw() == null) {
            gateway.setSignPub(gateway.getSignPubDeprecated());
        }
        return save(gateway);
    }

    public void setLegacyFieldsForServiceRollback(GatewayView gatewayView) {
        Gateway gateway = repository.findById(gatewayView.getId()).orElseThrow(NotFoundException.notFound("Gateway should exist"));
        gateway.setSignCertDeprecated(gatewayView.getSignCert());
        gateway.setSignPubDeprecated(gatewayView.getSignPub());
        gateway.setTokenCertDeprecated(gatewayView.getTokenCert());
        save(gateway);
    }

    public GatewayView putLegacyTokenCertIntoVaultIfNecessary(GatewayView gatewayView, ReadConfigResponse readConfigResponse) {
        if (shouldStoreTokenKeysFromClusterProxy(readConfigResponse)) {
            Gateway gateway = repository.findById(gatewayView.getId()).orElseThrow(NotFoundException.notFound("Gateway should exist"));
            KeyPair keyPairFromClusterProxy = clusterProxyRotationService.readClusterProxyTokenKeys(readConfigResponse);
            if (gateway.getTokenKeySecret() == null || gateway.getTokenKeySecret().getRaw() == null) {
                gateway.setTokenKeySecret(PkiUtil.convert(keyPairFromClusterProxy.getPrivate()));
                gateway.setTokenPubSecret(PkiUtil.convertPemPublicKey(keyPairFromClusterProxy.getPublic()));
            }
            if (gateway.getTokenCertSecret() == null || gateway.getTokenCertSecret().getRaw() == null) {
                gateway.setTokenCertSecret(gateway.getTokenCert());
            }
            return save(gateway);

        }
        return gatewayView;
    }

    private boolean shouldStoreTokenKeysFromClusterProxy(ReadConfigResponse readConfigResponse) {
        return StringUtils.isNotBlank(readConfigResponse.getKnoxSecretRef())
                && readConfigResponse.getKnoxSecretRef().startsWith("cluster-proxy/");
    }

}
