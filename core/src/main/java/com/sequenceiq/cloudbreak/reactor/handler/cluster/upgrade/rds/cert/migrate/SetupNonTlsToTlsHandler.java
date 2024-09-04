package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.migrate;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType.MIGRATE;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.migrate.MigrateRdsCertificateService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateTlsRdsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateTlsRdsResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SetupNonTlsToTlsHandler extends ExceptionCatcherEventHandler<UpdateTlsRdsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetupNonTlsToTlsHandler.class);

    @Inject
    private MigrateRdsCertificateService migrateRdsCertificateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateTlsRdsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateTlsRdsRequest> event) {
        return new RotateRdsCertificateFailedEvent(resourceId, event.getData().getRotateRdsCertificateType(), e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpdateTlsRdsRequest> event) {
        UpdateTlsRdsRequest request = event.getData();
        Long stackId = request.getResourceId();
        if (MIGRATE.equals(request.getRotateRdsCertificateType())) {
            migrateRdsCertificateService.migrateRdsToTls(stackId);
            migrateRdsCertificateService.migrateStackToTls(stackId);
            migrateRdsCertificateService.updateNonTlsToTlsIfRequired(stackId);
        }
        return new UpdateTlsRdsResult(stackId, request.getRotateRdsCertificateType());
    }
}
