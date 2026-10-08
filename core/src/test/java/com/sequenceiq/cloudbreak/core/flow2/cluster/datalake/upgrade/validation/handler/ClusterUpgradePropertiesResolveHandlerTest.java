package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.RESOLVE_CLUSTER_UPGRADE_PROPERTIES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.CLUSTER_UPGRADE_PROPERTIES_RESOLVED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradePropertiesResolveHandlerTest {

    @Mock
    private ClusterUpgradePropertiesResolver resolver;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private ClusterUpgradePropertiesResolveHandler underTest;

    @Test
    void resolvesPropertiesAndEmitsCompletion() {
        ClusterUpgradeValidationTriggerEvent request = request();
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(false, true, true);
        when(resolver.resolve(request)).thenReturn(properties);

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event<ClusterUpgradeValidationEvent>> emitted = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(CLUSTER_UPGRADE_PROPERTIES_RESOLVED_EVENT.event()), emitted.capture());
        assertThat(underTest.selector()).isEqualTo(RESOLVE_CLUSTER_UPGRADE_PROPERTIES_EVENT.event());
        assertThat(emitted.getValue().getData().getClusterUpgradeProperties()).isSameAs(properties);
        assertThat(emitted.getValue().getData().getResourceId()).isEqualTo(request.getResourceId());
    }

    @Test
    void catalogFailureFailsValidationWithTheOriginalException() {
        ClusterUpgradeValidationTriggerEvent request = request();
        CloudbreakServiceException failure = new CloudbreakServiceException("Image catalog is not reachable");
        when(resolver.resolve(request)).thenThrow(failure);

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event<ClusterUpgradeValidationFailureEvent>> emitted = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event()), emitted.capture());
        assertThat(emitted.getValue().getData().getException()).isSameAs(failure);
        assertThat(emitted.getValue().getData().getResourceId()).isEqualTo(request.getResourceId());
    }

    private ClusterUpgradeValidationTriggerEvent request() {
        return new ClusterUpgradeValidationTriggerEvent(1L, null, new ImageChangeDto(1L, "target", "custom", "custom-url"), false, true, true);
    }
}
