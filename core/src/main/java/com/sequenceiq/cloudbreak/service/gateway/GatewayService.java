package com.sequenceiq.cloudbreak.service.gateway;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.repository.GatewayRepository;
import com.sequenceiq.cloudbreak.view.GatewayView;

@Service
public class GatewayService {

    @Value("${cb.https.port}")
    private String httpsPort;

    @Inject
    private GatewayRepository repository;

    public Gateway save(Gateway gateway) {
        gateway.setGatewayPort(Integer.valueOf(httpsPort));
        return repository.save(gateway);
    }

    public Optional<GatewayView> getByClusterId(Long clusterId) {
        return Optional.ofNullable(repository.findByClusterId(clusterId).orElse(null));
    }

    public Boolean existsByClusterId(Long clusterId) {
        return repository.existsByClusterId(clusterId);
    }

    public void generateAndUpdateSignKeys(GatewayView gateway) {
        if (gateway != null) {
            if (gateway.getSignCert() == null) {
                KeyPair identityKey = PkiUtil.generateKeypair();
                KeyPair signKeyPair = PkiUtil.generateKeypair();
                X509Certificate cert = PkiUtil.cert(identityKey, "signing", signKeyPair);

                String signKey = PkiUtil.convert(identityKey.getPrivate());
                String signPub = PkiUtil.convert(identityKey.getPublic());
                String signCert = PkiUtil.convert(cert);
                Gateway existed = repository.findById(gateway.getId()).orElseThrow(NotFoundException.notFound("Gateway should be exist"));
                existed.setSignKey(signKey);
                existed.setSignPub(signPub);
                existed.setSignCert(signCert);
                save(existed);
            }
        }
    }
}
