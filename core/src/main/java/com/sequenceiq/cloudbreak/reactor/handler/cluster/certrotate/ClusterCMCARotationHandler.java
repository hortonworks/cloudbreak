package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrotate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCMCARotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCMCARotationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCertificatesRotationFailed;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterCMCARotationHandler extends ExceptionCatcherEventHandler<ClusterCMCARotationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCMCARotationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private SecretRotationSaltService saltService;

    @Inject
    private ClusterService clusterService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterCMCARotationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterCMCARotationRequest> event) {
        return new ClusterCertificatesRotationFailed(resourceId, e);
    }

    protected Selectable doAccept(HandlerEvent<ClusterCMCARotationRequest> event) {
        LOGGER.debug("Accepting Cluster Manager CMCA certificate rotation request...");
        ClusterCMCARotationRequest request = event.getData();
        try {
            if (CertificateRotationType.ALL.equals(request.getCertificateRotationType())) {
                StackDto stackDto = stackDtoService.getById(request.getResourceId());
                Cluster cluster = clusterService.getCluster(stackDto.getCluster().getId());
                String newTrustStoreVaultSecretJson = uncachedSecretServiceForRotation
                                .update(cluster.getTrustStorePwdSecret().getSecret(), PasswordUtil.generateCmAndPostgresConformPassword());
                String newKeyStorePwdVaultSecretJson = uncachedSecretServiceForRotation
                        .update(cluster.getKeyStorePwdSecret().getSecret(), PasswordUtil.generateCmAndPostgresConformPassword());
                cluster.setTrustStorePwdSecret(new SecretProxy(newTrustStoreVaultSecretJson));
                cluster.setKeyStorePwdSecret(new SecretProxy(newKeyStorePwdVaultSecretJson));
                clusterService.save(cluster);
                Map<String, SaltPillarProperties> saltPillarPropertiesMap =
                        Map.of("cloudera-manager-autotls", clusterHostServiceRunner.getClouderaManagerAutoTlsPillarProperties(cluster));
                saltService.updateSaltPillar(stackDto, saltPillarPropertiesMap);
                saltService.executeSaltState(stackDto, Set.of(stackDto.getPrimaryGatewayInstance().getDiscoveryFQDN()),
                        List.of("cloudera.manager.rotate.cmca-renewal"));
            }
            return new ClusterCMCARotationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.info("Cluster Manager CMCA certificate rotation failed. ", e);
            return new ClusterCertificatesRotationFailed(request.getResourceId(), e);
        }
    }
}
