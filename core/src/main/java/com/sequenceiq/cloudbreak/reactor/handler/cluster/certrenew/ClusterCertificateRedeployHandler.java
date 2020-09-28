package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrenew;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRedeployRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRedeploySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRenewFailed;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterCertificateRedeployHandler implements EventHandler<ClusterCertificateRedeployRequest> {

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
    public void accept(Event<ClusterCertificateRedeployRequest> event) {
        ClusterCertificateRedeployRequest data = event.getData();
        Long stackId = data.getResourceId();
        LOGGER.debug("Redeploy certificate for stack 'id:{}'", stackId);
        Selectable response;
        try {
            clusterServiceRunner.updateSaltState(stackId);
            LOGGER.info("Certificate of the cluster has been redeployed successfully.");
            response = new ClusterCertificateRedeploySuccess(stackId);
        } catch (Exception ex) {
            LOGGER.warn("Redeploy of certificate has been failed for cluster", ex);
            response = new ClusterCertificateRenewFailed(stackId, ex);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
