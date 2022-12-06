package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterDbCertRotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterDbCertRotationResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterDbCertRotationHandler extends ExceptionCatcherEventHandler<ClusterDbCertRotationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDbCertRotationHandler.class);

    @Inject
    private RedbeamsDbCertificateProvider dbCertificateProvider;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterService clusterService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterDbCertRotationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterDbCertRotationRequest> event) {
        return new ClusterDbCertRotationResult("Cannot rotate the DB root CERT on the cluster", e, event.getData());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterDbCertRotationRequest> event) {
        LOGGER.info("Start rotation of the DB cert on the Cluster");
        ClusterDbCertRotationRequest request = event.getData();
        Long resourceId = event.getData().getResourceId();
        StackDto stackDto = stackDtoService.getById(resourceId);
        RedbeamsDbCertificateProvider.RedbeamsDbSslDetails relatedSslCerts = dbCertificateProvider.getRelatedSslCerts(stackDto);
        clusterService.updateDbSslCert(stackDto.getCluster().getId(), relatedSslCerts);
        return new ClusterDbCertRotationResult(request);
    }
}
