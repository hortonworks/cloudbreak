package com.sequenceiq.redbeams.service.stack;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTerminationOutcomeV4Response;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerRequest;
import com.sequenceiq.redbeams.service.crn.CrnService;

import java.security.SecureRandom;
import java.util.Random;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RedbeamsTerminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsTerminationService.class);

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private RedbeamsFlowManager flowManager;

    @Inject
    private CrnService crnService;

    private final Random random = new SecureRandom();

    public DatabaseServerTerminationOutcomeV4Response terminateDatabaseServer(TerminateDatabaseServerRequest request, String accountId) {
        // Replace resourceId with something non-random
        flowManager.notify(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector(),
                new RedbeamsEvent(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector(), random.nextLong()));
        return new DatabaseServerTerminationOutcomeV4Response();
    }
}
