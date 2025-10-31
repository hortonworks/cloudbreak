package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrenew;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRedeployRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRedeploySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRenewFailed;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterCertificateRedeployHandler extends ExceptionCatcherEventHandler<ClusterCertificateRedeployRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCertificateRedeployHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterCertificateRedeployRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterCertificateRedeployRequest> event) {
        return new ClusterCertificateRenewFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterCertificateRedeployRequest> event) {
        ClusterCertificateRedeployRequest data = event.getData();
        Long stackId = data.getResourceId();
        LOGGER.debug("Redeploy certificate for stack 'id:{}'", stackId);
        Selectable response;
        try {
            clusterServiceRunner.redeployGatewayCertificate(stackId);
            LOGGER.info("Certificate of the cluster has been redeployed successfully.");
            response = new ClusterCertificateRedeploySuccess(stackId);
        } catch (Exception ex) {
            String msg = "Certificate couldn't be redeployed to the cluster: ";
            LOGGER.warn(msg, ex);
            response = new ClusterCertificateRenewFailed(stackId, new CloudbreakOrchestratorFailedException(msg + ex.getMessage(), ex));
        }
        return response;
    }
}
