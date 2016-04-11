package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudPlatformResponseToFlowFailureConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;

@Component("StackCreationFailureAction")
public class StackCreationFailureAction extends AbstractStackCreationAction<FlowFailureEvent> {
    @Inject
    private StackCreationService stackCreationService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private ServiceProviderConnectorAdapter connector;

    public StackCreationFailureAction() {
        super(FlowFailureEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, FlowFailureEvent payload, Map<Object, Object> variables) {
        stackCreationService.handleStackCreationFailure(context, payload.getException());
        sendEvent(context.getFlowId(), StackCreationEvent.STACK_CREATION_FAILED_EVENT.stringRepresentation(), payload);
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        return null;
    }

    protected void initPayloadConverterMap(List<PayloadConverter<FlowFailureEvent>> payloadConverters) {
        payloadConverters.add(new CloudPlatformResponseToFlowFailureConverter());
    }
}
