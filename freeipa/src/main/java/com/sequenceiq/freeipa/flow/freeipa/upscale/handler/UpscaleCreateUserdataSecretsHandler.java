package com.sequenceiq.freeipa.flow.freeipa.upscale.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleCreateUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleCreateUserdataSecretsSuccess;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class UpscaleCreateUserdataSecretsHandler extends ExceptionCatcherEventHandler<UpscaleCreateUserdataSecretsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleCreateUserdataSecretsHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleCreateUserdataSecretsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpscaleCreateUserdataSecretsRequest> event) {
        LOGGER.error("Unexpected error occurred while creating userdata secrets!", e);
        return new UpscaleFailureEvent(resourceId, "Creating userdata secrets", Set.of(), Map.of(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpscaleCreateUserdataSecretsRequest> event) {
        UpscaleCreateUserdataSecretsRequest request = event.getData();
        Long stackId = request.getResourceId();
        List<Long> createdSecretResourceIds = new ArrayList<>();
        List<Long> instancePrivateIds = request.getInstancePrivateIds();
        if (!instancePrivateIds.isEmpty()) {
            LOGGER.info("Creating userdata secret resources for private ids [{}]...", instancePrivateIds);
            Stack stack = stackService.getStackById(stackId);
            List<Resource> newSecretResources = userdataSecretsService.createUserdataSecrets(stack, instancePrivateIds, request.getCloudContext(),
                    request.getCloudCredential());
            createdSecretResourceIds.addAll(newSecretResources.stream().map(Resource::getId).toList());
        }
        return new UpscaleCreateUserdataSecretsSuccess(stackId, createdSecretResourceIds);
    }
}
