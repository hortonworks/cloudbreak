package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrenew;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateReissueRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateReissueSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRenewFailed;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterCertificateReissueHandler extends ExceptionCatcherEventHandler<ClusterCertificateReissueRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCertificateReissueHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterCertificateReissueRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterCertificateReissueRequest> event) {
        return new ClusterCertificateRenewFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterCertificateReissueRequest> event) {
        ClusterCertificateReissueRequest data = event.getData();
        Long stackId = data.getResourceId();
        LOGGER.debug("Reissue certificate for stack 'id:{}'", stackId);
        Selectable response;
        try {
            reissueCertificate(stackId);
            LOGGER.info("Certificate of the cluster has been reissued successfully.");
            response = new ClusterCertificateReissueSuccess(stackId);
        } catch (Exception ex) {
            LOGGER.warn("The certificate of the cluster could not be reissued via PEM service.", ex);
            response = new ClusterCertificateRenewFailed(stackId, ex);
        }
        return response;
    }

    private void reissueCertificate(Long stackId) {
        Stack stack = stackService.getByIdWithGatewayInTransaction(stackId);
        clusterPublicEndpointManagementService.renewCertificate(stack);
    }
}
