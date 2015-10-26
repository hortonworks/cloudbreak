package com.sequenceiq.cloudbreak.core.flow.service;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ImageCheckerContext;
import com.sequenceiq.cloudbreak.service.cluster.flow.ImageStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.event.CheckImageComplete;
import com.sequenceiq.cloudbreak.service.stack.event.PrepareImageComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

@Component
public class ProvisioningSetupService {

    public static final int MAX_ATTEMPTS_FOR_IMAGE_OPS = -1;
    public static final int IMAGE_POLLING_INTERVAL = 5000;
    public static final int MAX_FAILURE_COUNT = 5;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private CloudPlatformResolver cloudPlatformResolver;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private PollingService<ImageCheckerContext> imageCheckerContextPollingService;

    @Inject
    private ImageStatusCheckerTask imageStatusCheckerTask;

    public ProvisionSetupComplete setup(Stack stack) throws Exception {
        return (ProvisionSetupComplete)
                cloudPlatformResolver.provisioning(stack.cloudPlatform()).setupProvisioning(stack);
    }

    public PrepareImageComplete prepareImage(Stack stack) throws Exception {
        PrepareImageComplete prepareImageComplete = (PrepareImageComplete)
                cloudPlatformResolver.provisioning(stack.cloudPlatform()).prepareImage(stack);
        return prepareImageComplete;
    }

    public CheckImageComplete checkImage(Stack stack) throws Exception {
        imageCheckerContextPollingService.pollWithTimeout(imageStatusCheckerTask, new ImageCheckerContext(stack),
                IMAGE_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_IMAGE_OPS, MAX_FAILURE_COUNT);
        return new CheckImageComplete(stack.cloudPlatform(), stack.getId());
    }

}
