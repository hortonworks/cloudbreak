package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.handler;

import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_EXISTS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.PollBindUserCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.ValidateKerberosConfigEvent;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaOperationCheckerTask;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaOperationPollerObject;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;

import reactor.bus.Event;

@Component
public class PollBindUserCreationHandler extends ExceptionCatcherEventHandler<PollBindUserCreationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollBindUserCreationHandler.class);

    @Value("${cb.freeipa.binduser.poll.interval:5000}")
    private int pollIntervalMilliSec;

    @Value("${cb.freeipa.binduser.poll.waitTime:600}")
    private long pollWaitTimeSec;

    @Value("${cb.freeipa.binduser.poll.maxError:5}")
    private int pollMaxError;

    @Inject
    private OperationV1Endpoint operationV1Endpoint;

    @Inject
    private PollingService<FreeIpaOperationPollerObject> freeIpaOperationChecker;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PollBindUserCreationEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<PollBindUserCreationEvent> event) {
        LOGGER.error("Unexpected error during polling bind user creation operation with id [{}]", event.getData().getOperationId(), e);
        return new StackFailureEvent(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), event.getData().getResourceId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<PollBindUserCreationEvent> event) {
        PollBindUserCreationEvent data = event.getData();
        FreeIpaOperationPollerObject operationPollerObject = new FreeIpaOperationPollerObject(data.getOperationId(),
                OperationType.BIND_USER_CREATE.name(), operationV1Endpoint, data.getAccountId(), regionAwareInternalCrnGeneratorFactory);
        ExtendedPollingResult result = freeIpaOperationChecker.pollWithAbsoluteTimeout(new FreeIpaOperationCheckerTask<>(), operationPollerObject,
                pollIntervalMilliSec, pollWaitTimeSec, pollMaxError);
        if (result.isSuccess()) {
            return new ValidateKerberosConfigEvent(VALIDATE_KERBEROS_CONFIG_EXISTS_EVENT.event(), data.getResourceId(), true);
        } else {
            StringBuilder errorMessage = new StringBuilder("Bind user creation failed");
            if (result.getException() != null) {
                errorMessage.append(" with: ");
                errorMessage.append(result.getException().getMessage());
            }
            return new StackFailureEvent(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), data.getResourceId(), new Exception(errorMessage.toString()));
        }
    }
}
