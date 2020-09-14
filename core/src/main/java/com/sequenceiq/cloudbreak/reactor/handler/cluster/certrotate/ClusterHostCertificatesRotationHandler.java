package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrotate;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCertificatesRotationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class ClusterHostCertificatesRotationHandler extends ExceptionCatcherEventHandler<ClusterHostCertificatesRotationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterHostCertificatesRotationHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterHostCertificatesRotationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterHostCertificatesRotationRequest> event) {
        return new ClusterCertificatesRotationFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        LOGGER.debug("Accepting Cluster Manager host certificates rotation request...");
        ClusterHostCertificatesRotationRequest request = event.getData();
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            apiConnectors.getConnector(stack).rotateHostCertificates();
            result = new ClusterHostCertificatesRotationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.info("Cluster Manager host certificates rotation failed", e);
            result = new ClusterCertificatesRotationFailed(request.getResourceId(), e);
        }
        return result;
    }
}
