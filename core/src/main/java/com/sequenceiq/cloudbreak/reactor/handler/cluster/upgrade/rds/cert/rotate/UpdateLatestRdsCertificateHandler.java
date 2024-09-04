package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType.MIGRATE;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.migrate.MigrateRdsCertificateService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.rotate.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateLatestRdsCertificateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateLatestRdsCertificateResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdateLatestRdsCertificateHandler extends ExceptionCatcherEventHandler<UpdateLatestRdsCertificateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateLatestRdsCertificateHandler.class);

    @Inject
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Inject
    private MigrateRdsCertificateService migrateRdsCertificateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateLatestRdsCertificateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateLatestRdsCertificateRequest> event) {
        return new RotateRdsCertificateFailedEvent(resourceId, event.getData().getRotateRdsCertificateType(), e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpdateLatestRdsCertificateRequest> event) {
        UpdateLatestRdsCertificateRequest request = event.getData();
        Long stackId = request.getResourceId();
        rotateRdsCertificateService.updateLatestRdsCertificate(stackId);
        if (MIGRATE.equals(request.getRotateRdsCertificateType())) {
            migrateRdsCertificateService.enableSslOnClusterSide(stackId);
        }
        return new UpdateLatestRdsCertificateResult(stackId, request.getRotateRdsCertificateType());
    }
}
