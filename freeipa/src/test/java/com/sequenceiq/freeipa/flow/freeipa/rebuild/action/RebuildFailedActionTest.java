package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup.ValidateBackupFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.instance.InstanceFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class RebuildFailedActionTest {

    private static final int PAYLOAD_CONVERTER_COUNT = 6;

    private static final String OPERATION_ID = "operationId";

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private OperationService operationService;

    @InjectMocks
    private RebuildFailedAction underTest;

    @Test
    void doExecute() throws Exception {
        Stack stack = new Stack();
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudStack cloudStack = mock(CloudStack.class);
        StackContext context = new StackContext(mock(FlowParameters.class), stack, cloudContext, cloudCredential, cloudStack);

        underTest.doExecute(context, new RebuildFailureEvent(4L, ERROR, new CloudbreakException("asdf")),
                Map.of(OperationAwareAction.OPERATION_ID, OPERATION_ID));

        ArgumentCaptor<Object> payloadCapture = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), payloadCapture.capture());
        StackEvent payload = (StackEvent) payloadCapture.getValue();
        assertEquals(4L, payload.getResourceId());
        assertEquals(FreeIpaRebuildFlowEvent.REBUILD_FAILURE_HANDLED_EVENT.event(), payload.selector());
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_FAILED, "Failed to rebuild FreeIPA: asdf");
        verify(operationService).failOperation(stack.getAccountId(), OPERATION_ID, "Failed to rebuild FreeIPA: asdf");
    }

    @Test
    void initPayloadConverterMap() {
        ArrayList<PayloadConverter<RebuildFailureEvent>> converters = new ArrayList<>(PAYLOAD_CONVERTER_COUNT);

        underTest.initPayloadConverterMap(converters);

        assertEquals(PAYLOAD_CONVERTER_COUNT, converters.size());
        assertTrue(Stream.of(UpscaleStackResult.class, DownscaleStackCollectResourcesResult.class, DownscaleStackResult.class, ValidateBackupFailed.class,
                        StackFailureEvent.class, InstanceFailureEvent.class)
                .allMatch(clazz -> converters.stream().anyMatch(converter -> converter.canConvert(clazz))));
    }
}