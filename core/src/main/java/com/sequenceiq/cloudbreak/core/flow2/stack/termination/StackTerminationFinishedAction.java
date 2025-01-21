package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.recovery.RecoveryTeardownService;

@Component("StackTerminationFinishedAction")
public class StackTerminationFinishedAction extends AbstractStackTerminationAction<TerminateStackResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFinishedAction.class);

    @Inject
    private StackTerminationService stackTerminationService;

    @Inject
    private RecoveryTeardownService recoveryTeardownService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    public StackTerminationFinishedAction() {
        super(TerminateStackResult.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminateStackResult payload, Map<Object, Object> variables) {
        StackDtoDelegate stackDtoDelegate = context.getStack();
        if (context.getTerminationType().isRecovery()) {
            LOGGER.debug("Recovery is in progress, skipping stack termination finalization!");
            recoveryTeardownService.handleRecoveryTeardownSuccess(stackDtoDelegate.getStack(), payload);
        } else {
            LOGGER.debug("Termination type is {}, executing stack termination finalization!", context.getTerminationType());
            Optional<SdxBasicView> sdxBasicView = platformAwareSdxConnector
                    .getSdxBasicViewByEnvironmentCrn(stackDtoDelegate.getEnvironmentCrn());
            sdxBasicView.ifPresent(sdx ->
                    platformAwareSdxConnector.tearDownDatahub(sdx.crn(), stackDtoDelegate.getResourceCrn()));
            stackTerminationService.finishStackTermination(stackDtoDelegate.getStack(), context.getTerminationType().isForced(), payload);
        }
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackTerminationContext context) {
        return new StackEvent(StackTerminationEvent.TERMINATION_FINALIZED_EVENT.event(), context.getStack().getId());
    }
}
