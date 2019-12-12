package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrenew;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateReissueRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateReissueSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRenewFailed;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterCertificateReissueHandler implements EventHandler<ClusterCertificateReissueRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCertificateReissueHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterCertificateReissueRequest.class);
    }

    @Override
    public void accept(Event<ClusterCertificateReissueRequest> event) {
        ClusterCertificateReissueRequest data = event.getData();
        Long stackId = data.getResourceId();
        LOGGER.debug("Reissue certificate for stack 'id:{}'", stackId);
        Selectable response;
        try {
            boolean certReissued = reissueCertificate(stackId);
            if (certReissued) {
                LOGGER.info("Certificate of the cluster has been reissued successfully.");
                response = new ClusterCertificateReissueSuccess(stackId);
            } else {
                String message = "The certificate of the cluster could not be reissued via PEM service.";
                LOGGER.warn(message);
                response = new ClusterCertificateRenewFailed(stackId, new IllegalStateException(message));
            }
        } catch (Exception ex) {
            LOGGER.warn("Reissue of certificate has been failed for cluster", ex);
            response = new ClusterCertificateRenewFailed(stackId, ex);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }

    private boolean reissueCertificate(Long stackId) throws TransactionService.TransactionExecutionException {
        return transactionService.required(() -> {
                    Stack stack = stackService.getById(stackId);
                    return clusterPublicEndpointManagementService.renewCertificate(stack);
                });
    }
}
