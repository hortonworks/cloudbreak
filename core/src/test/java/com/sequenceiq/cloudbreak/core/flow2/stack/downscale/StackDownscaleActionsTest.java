package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.DownscaleRemoveUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.DownscaleRemoveUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class StackDownscaleActionsTest {

    private static final String INSTANCE_GROUP_NAME = "worker";

    private static final Integer ADJUSTMENT = -3;

    private static final Long STACK_ID = 123L;

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StackView stack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    private StackScalingFlowContext context;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private EnvironmentService environmentClientService;

    @InjectMocks
    private StackDownscaleActions underTest;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

    @BeforeEach
    void setUp() {
        when(stack.getId()).thenReturn(STACK_ID);
        context = new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential,
                Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT), Map.of(INSTANCE_GROUP_NAME, Set.of(0L, 1L, 2L)), Map.of(),
                false, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, ADJUSTMENT.longValue()));
    }

    private AbstractStackDownscaleAction<DownscaleStackResult> getDownscaleRemoveUserdataSecretsAction() {
        AbstractStackDownscaleAction<DownscaleStackResult> action =
                (AbstractStackDownscaleAction<DownscaleStackResult>) underTest.downscaleRemoveUserdataSecretsAction();
        initActionPrivateFields(action);
        return action;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDownscaleRemoveUserdataSecretsAction(boolean secretEncryptionEnabled) throws Exception {
        DownscaleStackResult payload = new DownscaleStackResult(STACK_ID, List.of());
        Map<Object, Object> variables = Map.of();
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(secretEncryptionEnabled)
                .build());
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(getDownscaleRemoveUserdataSecretsAction()).doExecute(context, payload, variables);

        verify(reactorEventFactory).createEvent(anyMap(), payloadArgumentCaptor.capture());
        if (secretEncryptionEnabled) {
            verify(eventBus).notify("DOWNSCALEREMOVEUSERDATASECRETSREQUEST", event);
            DownscaleRemoveUserdataSecretsRequest request = (DownscaleRemoveUserdataSecretsRequest) payloadArgumentCaptor.getValue();
            assertEquals(STACK_ID, request.getResourceId());
            assertEquals(cloudContext, request.getCloudContext());
            assertEquals(cloudCredential, request.getCloudCredential());
            assertThat(request.getInstancePrivateIds()).hasSameElementsAs(List.of(0L, 1L, 2L));
        } else {
            verify(eventBus).notify("DOWNSCALEREMOVEUSERDATASECRETSSUCCESS", event);
            DownscaleRemoveUserdataSecretsSuccess success = (DownscaleRemoveUserdataSecretsSuccess) payloadArgumentCaptor.getValue();
            assertEquals(STACK_ID, success.getResourceId());
        }
    }
}
