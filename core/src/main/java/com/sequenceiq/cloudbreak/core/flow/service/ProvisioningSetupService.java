package com.sequenceiq.cloudbreak.core.flow.service;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ImageCheckerContext;
import com.sequenceiq.cloudbreak.service.cluster.flow.ImageStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.event.CheckImageComplete;
import com.sequenceiq.cloudbreak.service.stack.event.PrepareImageComplete;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

@Component
public class ProvisioningSetupService {

    public static final int MAX_ATTEMPTS_FOR_IMAGE_OPS = -1;
    public static final int IMAGE_POLLING_INTERVAL = 5000;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private CloudPlatformResolver cloudPlatformResolver;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private PollingService<ImageCheckerContext> imageCheckerContextPollingService;

    @Inject
    private ImageStatusCheckerTask imageStatusCheckerTask;

    public ProvisionSetupComplete setup(Stack stack) throws Exception {
        ProvisionSetupComplete setupComplete = (ProvisionSetupComplete)
                cloudPlatformResolver.provisioning(stack.cloudPlatform()).setupProvisioning(stack);
        tlsSecurityService.copyClientKeys(stack.getId());
        tlsSecurityService.setupSSHKeys(stack);
        return setupComplete;
    }

    public PrepareImageComplete prepareImage(Stack stack) throws Exception {
        PrepareImageComplete prepareImageComplete = (PrepareImageComplete)
                cloudPlatformResolver.provisioning(stack.cloudPlatform()).prepareImage(stack);
        return prepareImageComplete;
    }

    public CheckImageComplete checkImage(Stack stack) throws Exception {
        imageCheckerContextPollingService.pollWithTimeout(imageStatusCheckerTask, new ImageCheckerContext(stack),
                IMAGE_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_IMAGE_OPS);
        return new CheckImageComplete(stack.cloudPlatform(), stack.getId());
    }

}
