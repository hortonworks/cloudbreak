package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRotateHostCertificateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRotateHostCertificateResult;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.service.cluster.CertificateExpirationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterStartRotateHostCertificateHandler extends ExceptionCatcherEventHandler<ClusterStartRotateHostCertificateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartRotateHostCertificateHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CertificateExpirationService certExpirationService;

    @Inject
    private SecretRotationSaltService saltService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterStartRotateHostCertificateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterStartRotateHostCertificateRequest> event) {
        return new ClusterStartRotateHostCertificateResult("Host certificate rotation failed during cluster start", e, event.getData());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterStartRotateHostCertificateRequest> event) {
        ClusterStartRotateHostCertificateRequest request = event.getData();
        StackDto stackDto = stackDtoService.getById(request.getResourceId());
        try {
            if (validateCertificateFullyExpired(stackDto)) {
                LOGGER.info("Host certificates are expired on cluster {}, rotating via salt during cluster start.", stackDto.getName());
                saltService.executeSaltStateOnPrimaryGateway(stackDto,
                        List.of("cloudera.manager.rotate.host-cert-expired-token-generation"));
                saltService.executeSaltState(stackDto,
                        stackDto.getAllFunctioningNodes().stream().map(Node::getHostname).collect(Collectors.toSet()),
                        List.of("cloudera.manager.rotate.host-cert-manual-renewal"));
            } else {
                LOGGER.info("Host certificates are valid on cluster {}, skipping rotation during cluster start.", stackDto.getName());
            }
            return new ClusterStartRotateHostCertificateResult(request);
        } catch (CloudbreakOrchestratorFailedException e) {
            return new ClusterStartRotateHostCertificateResult("Host certificate rotation failed during cluster start", e, request);
        }
    }

    private boolean validateCertificateFullyExpired(StackDto stackDto) {
        try {
            return certExpirationService.validateCertificateFullyExpired(stackDto);
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("Failed to check host certificate expiration on cluster {}, skipping rotation.", stackDto.getName(), e);
            return false;
        }
    }
}
