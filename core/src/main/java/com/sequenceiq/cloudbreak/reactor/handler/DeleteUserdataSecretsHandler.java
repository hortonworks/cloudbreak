package com.sequenceiq.cloudbreak.reactor.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.DeleteUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.DeleteUserdataSecretsFinished;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.DeleteUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DeleteUserdataSecretsHandler extends ExceptionCatcherEventHandler<DeleteUserdataSecretsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUserdataSecretsHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeleteUserdataSecretsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DeleteUserdataSecretsRequest> event) {
        LOGGER.error("Unexpected error occurred while deleting userdata secrets.", e);
        return new DeleteUserdataSecretsFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DeleteUserdataSecretsRequest> event) {
        DeleteUserdataSecretsRequest request = event.getData();
        Long stackId = request.getResourceId();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        String environmentCrn = stackService.findEnvironmentCrnByStackId(stackId);
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(environmentCrn);
        if (environment.isEnableSecretEncryption()) {
            try {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                LOGGER.info("Deleting userdata secrets for stack [{}]...", stack.getName());
                userdataSecretsService.deleteUserdataSecretsForStack(stack, cloudContext, cloudCredential);
            } catch (RuntimeException e) {
                if (request.getTerminationType().isForced()) {
                    LOGGER.warn("Failed to delete userdata secrets for stack. Some secret resources might not have been deleted on the provider!", e);
                } else {
                    throw e;
                }
            }
        }
        return new DeleteUserdataSecretsFinished(stackId);
    }
}
