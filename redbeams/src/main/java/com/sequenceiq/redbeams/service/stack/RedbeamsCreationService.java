package com.sequenceiq.redbeams.service.stack;

import com.sequenceiq.redbeams.api.model.describe.DescribeRedbeamsResponse;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.service.RedbeamsFlowManager;
import com.sequenceiq.redbeams.service.crn.CrnService;

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

    public DescribeRedbeamsResponse launchDatabase(AllocateDatabaseServerRequest request, String accountId) {
        // TODO: Actually launch a database instance
        /*
        checkIfAlreadyExistsInEnvironment(request, accountId);

        String userId = crnService.getCurrentUserId();
        Stack stack = stackConverter.convert(request, accountId, userId);
        stack.setResourceCrn(crnService.createCrn(accountId, Crn.ResourceType.FREEIPA));

        fillInstanceMetadata(stack);

        String template = templateService.waitGetTemplate(stack, getPlatformTemplateRequest);
        stack.setTemplate(template);
        stackService.save(stack);
        flowManager.notify(FlowChainTriggers.PROVISION_TRIGGER_EVENT, new RedbeamsEvent(FlowChainTriggers.PROVISION_TRIGGER_EVENT, stack.getId()));
         */
        return new DescribeRedbeamsResponse();
    }
}
