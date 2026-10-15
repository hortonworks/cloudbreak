package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.RESOLVE_CLUSTER_UPGRADE_PROPERTIES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.CLUSTER_UPGRADE_PROPERTIES_RESOLVED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
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
import com.sequenceiq.cloudbreak.service.upgrade.ComponentVersionComparator;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeComponents;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePermissionProvider;
import com.sequenceiq.cloudbreak.service.upgrade.VersionComparisonContextFactory;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradePropertiesResolveHandlerTest {

    @Mock
    private ClusterUpgradePropertiesResolver resolver;

    @Mock
    private EventBus eventBus;

    @Spy
    private UpgradePermissionProvider upgradePermissionProvider = new UpgradePermissionProvider();

    @InjectMocks
    private ClusterUpgradePropertiesResolveHandler underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(upgradePermissionProvider, "versionComparisonContextFactory", new VersionComparisonContextFactory());
        ReflectionTestUtils.setField(upgradePermissionProvider, "componentVersionComparator", new ComponentVersionComparator());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void resolvesPropertiesAndEmitsCompletion(boolean components) {
        ClusterUpgradeValidationTriggerEvent request = request(components);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(false, true, true);
        when(resolver.resolve(request)).thenReturn(properties);
        if (components) {
            doNothing().when(upgradePermissionProvider).validateRequestedComponentVersions(properties);
        }

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event<ClusterUpgradeValidationEvent>> emitted = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(CLUSTER_UPGRADE_PROPERTIES_RESOLVED_EVENT.event()), emitted.capture());
        assertThat(underTest.selector()).isEqualTo(RESOLVE_CLUSTER_UPGRADE_PROPERTIES_EVENT.event());
        assertThat(emitted.getValue().getData().getClusterUpgradeProperties()).isSameAs(properties);
        assertThat(emitted.getValue().getData().getResourceId()).isEqualTo(request.getResourceId());
        if (components) {
            verify(upgradePermissionProvider).validateRequestedComponentVersions(properties);
        } else {
            verifyNoInteractions(upgradePermissionProvider);
        }
    }

    @Test
    void catalogFailureFailsValidationWithTheOriginalException() {
        ClusterUpgradeValidationTriggerEvent request = request(false);
        CloudbreakServiceException failure = new CloudbreakServiceException("Image catalog is not reachable");
        when(resolver.resolve(request)).thenThrow(failure);

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event<ClusterUpgradeValidationFailureEvent>> emitted = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event()), emitted.capture());
        assertThat(emitted.getValue().getData().getException()).isSameAs(failure);
        assertThat(emitted.getValue().getData().getResourceId()).isEqualTo(request.getResourceId());
        verifyNoInteractions(upgradePermissionProvider);
    }

    @Test
    void requestedCmDowngradeFailsValidation() {
        ClusterUpgradeValidationTriggerEvent request = request(true);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(false, true, true);
        properties.currentImage().packageVersions().putAll(Map.of(ImagePackageVersion.CM.getKey(), "7.9.0",
                ImagePackageVersion.CM_BUILD_NUMBER.getKey(), "100"));
        properties.targetImage().packageVersions().putAll(Map.of(ImagePackageVersion.CM.getKey(), "7.2.0",
                ImagePackageVersion.CM_BUILD_NUMBER.getKey(), "200"));
        when(resolver.resolve(request)).thenReturn(properties);

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event<ClusterUpgradeValidationFailureEvent>> emitted = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event()), emitted.capture());
        assertThat(emitted.getValue().getData().getException()).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cloudera Manager");
    }

    private ClusterUpgradeValidationTriggerEvent request(boolean components) {
        return new ClusterUpgradeValidationTriggerEvent(1L, null, new ImageChangeDto(1L, "target", "custom", "custom-url",
                components ? new UpgradeComponents(null, List.of()) : null), false, true, true);
    }
}
