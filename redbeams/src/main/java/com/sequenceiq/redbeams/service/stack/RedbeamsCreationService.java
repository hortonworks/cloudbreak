package com.sequenceiq.redbeams.service.stack;

import com.sequenceiq.redbeams.api.model.describe.DatabaseServerAllocationOutcomeV4Response;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.service.crn.CrnService;

import java.security.SecureRandom;
import java.util.Random;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RedbeamsCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsCreationService.class);

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private RedbeamsFlowManager flowManager;

    @Inject
    private CrnService crnService;

    private final Random random = new SecureRandom();

    public DatabaseServerAllocationOutcomeV4Response launchDatabase(AllocateDatabaseServerRequest request, String accountId) {
        // Replace resourceId with something non-random
        flowManager.notify(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.selector(),
                new RedbeamsEvent(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.selector(), random.nextLong()));
        return new DatabaseServerAllocationOutcomeV4Response();
    }
}
