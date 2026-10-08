package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFinishedEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.validation.service.ServiceUpgradeValidationRequest;
import com.sequenceiq.cloudbreak.service.upgrade.validation.service.ServiceUpgradeValidator;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeServiceValidationHandlerTest {

    @Mock
    private ClusterUpgradePropertiesResolver resolver;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stack;

    @Mock
    private ServiceUpgradeValidator validator;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private ClusterUpgradeServiceValidationHandler underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "serviceUpgradeValidators", List.of(validator));
    }

    @Test
    void legacyPropertiesRebuildFailureFailsFlowWithoutResolvingAgain() throws Exception {
        ClusterUpgradeServiceValidationEvent request = JsonUtil.readValue("""
                {"@type":"com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent",
                "resourceId":1,"imageId":"target","lockComponents":true,"rollingUpgradeEnabled":false,"replaceVms":true}
                """, ClusterUpgradeServiceValidationEvent.class);
        NotFoundException failure = new NotFoundException("Target image unavailable after restart");
        when(resolver.resolve(request)).thenThrow(failure);

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event<ClusterUpgradeValidationFailureEvent>> emitted = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event()), emitted.capture());
        assertThat(emitted.getValue().getData().getException()).isSameAs(failure);
        verify(resolver).resolve(request);
    }

    @Test
    void validationUsesResolvedOptionsAndKeepsPropertiesWhenServiceCheckIsSkipped() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(true, false, true);
        ClusterUpgradeServiceValidationEvent request = new ClusterUpgradeServiceValidationEvent(1L, properties.targetImageId(), properties);
        when(resolver.resolve(request)).thenReturn(properties);
        when(stackDtoService.getById(1L)).thenReturn(stack);
        IllegalStateException failure = new IllegalStateException("CM unavailable");
        ArgumentCaptor<ServiceUpgradeValidationRequest> validation = ArgumentCaptor.forClass(ServiceUpgradeValidationRequest.class);
        doThrow(failure).when(validator).validate(validation.capture());

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event<ClusterUpgradeValidationFinishedEvent>> emitted = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT.event()), emitted.capture());
        assertThat(emitted.getValue().getData().getClusterUpgradeProperties()).isSameAs(properties);
        assertThat(emitted.getValue().getData().getException()).isSameAs(failure);
        assertThat(validation.getValue().clusterUpgradeProperties()).isSameAs(properties);
        assertThat(validation.getValue().lockComponents()).isTrue();
        assertThat(validation.getValue().rollingUpgradeEnabled()).isFalse();
        assertThat(validation.getValue().replaceVms()).isTrue();
        verify(resolver, times(1)).resolve(request);
    }
}
