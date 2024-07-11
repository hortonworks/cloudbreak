package com.sequenceiq.cloudbreak.reactor;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpscaleCreateUserdataSecretsHandler extends ExceptionCatcherEventHandler<UpscaleCreateUserdataSecretsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleCreateUserdataSecretsHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Inject
    private EncryptionKeyService encryptionKeyService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleCreateUserdataSecretsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpscaleCreateUserdataSecretsRequest> event) {
        LOGGER.error("Unexpected error occurred while creating userdata secrets!", e);
        return new UpscaleCreateUserdataSecretsFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpscaleCreateUserdataSecretsRequest> event) {
        UpscaleCreateUserdataSecretsRequest request = event.getData();
        Long stackId = request.getResourceId();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        List<Long> instancePrivateIds = request.getInstancePrivateIds();
        List<Long> createdSecretResourceIds = new ArrayList<>();

        if (!instancePrivateIds.isEmpty()) {
            LOGGER.info("Creating userdata secret resources for private ids [{}]...", instancePrivateIds);
            Stack stack = stackService.getById(stackId);
            List<Resource> newSecretResources = userdataSecretsService.createUserdataSecrets(stack, instancePrivateIds, request.getCloudContext(),
                    request.getCloudCredential());
            List<String> secretResourceReferences = new ArrayList<>();
            newSecretResources.forEach(resource -> {
                createdSecretResourceIds.add(resource.getId());
                secretResourceReferences.add(resource.getResourceReference());
            });
            encryptionKeyService.updateCloudSecretManagerEncryptionKeyAccess(stack, cloudContext, cloudCredential, secretResourceReferences, List.of());
        }
        return new UpscaleCreateUserdataSecretsSuccess(stackId, createdSecretResourceIds);
    }
}
