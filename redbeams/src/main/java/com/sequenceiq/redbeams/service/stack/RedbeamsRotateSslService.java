package com.sequenceiq.redbeams.service.stack;

import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateRedbeamsEvent;

@Service
public class RedbeamsRotateSslService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsRotateSslService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsFlowManager flowManager;

    public FlowIdentifier rotateDatabaseServerSslCert(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());
        LOGGER.debug("Rotate to latest ssl called for: {}", dbStack);
        return flowManager.notify(REDBEAMS_SSL_CERT_ROTATE_EVENT.selector(),
                new SslCertRotateRedbeamsEvent(REDBEAMS_SSL_CERT_ROTATE_EVENT.selector(), dbStack.getId(), false));
    }

    public FlowIdentifier updateToLatestDatabaseServerSslCert(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());
        LOGGER.debug("Update to latest ssl called for: {}", dbStack);
        return flowManager.notify(REDBEAMS_SSL_CERT_ROTATE_EVENT.selector(),
                new SslCertRotateRedbeamsEvent(REDBEAMS_SSL_CERT_ROTATE_EVENT.selector(), dbStack.getId(), true));
    }
}
