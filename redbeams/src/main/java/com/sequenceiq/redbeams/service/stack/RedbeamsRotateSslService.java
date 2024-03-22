package com.sequenceiq.redbeams.service.stack;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent;

@Service
public class RedbeamsRotateSslService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsRotateSslService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsFlowManager flowManager;

    public void rotateDatabaseServerSslCert(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());
        LOGGER.debug("Rotate ssl called for: {}", dbStack);
        flowManager.notify(RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_EVENT.selector(),
                new RedbeamsEvent(RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_EVENT.selector(), dbStack.getId()));
    }
}
