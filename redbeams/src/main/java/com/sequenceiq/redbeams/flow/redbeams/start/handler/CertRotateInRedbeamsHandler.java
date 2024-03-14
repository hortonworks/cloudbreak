package com.sequenceiq.redbeams.flow.redbeams.start.handler;

import static org.slf4j.LoggerFactory.getLogger;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.event.CertRotateInRedbeamsRequest;
import com.sequenceiq.redbeams.flow.redbeams.start.event.CertRotateInRedbeamsSuccess;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerFailed;
import com.sequenceiq.redbeams.service.rotate.CloudProviderCertRotator;
import com.sequenceiq.redbeams.service.stack.DBStackUpdater;

@Component
public class CertRotateInRedbeamsHandler extends ExceptionCatcherEventHandler<CertRotateInRedbeamsRequest> {

    private static final Logger LOGGER = getLogger(CertRotateInRedbeamsHandler.class);

    @Inject
    private DBStackUpdater dbStackUpdater;

    @Inject
    private CloudProviderCertRotator cloudProviderCertRotator;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CertRotateInRedbeamsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CertRotateInRedbeamsRequest> event) {
        LOGGER.error("Cannot rotate the root CERT, error: ", e);
        return new StartDatabaseServerFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CertRotateInRedbeamsRequest> event) {
        LOGGER.debug("CERT rotation has been started");
        CertRotateInRedbeamsRequest data = event.getData();
        try {
            cloudProviderCertRotator.rotate(
                    data.getResourceId(),
                    data.getCloudContext(),
                    data.getCloudCredential(),
                    data.getDbStack());
            dbStackUpdater.updateSslConfig(
                    data.getResourceId(),
                    data.getCloudContext(),
                    data.getCloudCredential(),
                    data.getDbStack());
        } catch (Exception e) {
            throw new CloudbreakServiceException(e);
        }
        return new CertRotateInRedbeamsSuccess(data.getResourceId());
    }
}
