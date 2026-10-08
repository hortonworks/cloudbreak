package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradeParcelSettingsPreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.common.model.OsType;

// TODO CB-33421: Remove with ClusterUpgradePropertiesResolver once in-flight flow events always carry clusterUpgradeProperties in JSON.
@ExtendWith(MockitoExtension.class)
class ClusterUpgradePropertiesResolverTest {

    private static final long STACK_ID = 10L;

    private static final String TARGET_IMAGE_ID = "targetImageId";

    @Mock
    private ClusterUpgradePropertiesFactory clusterUpgradePropertiesFactory;

    @InjectMocks
    private ClusterUpgradePropertiesResolver underTest;

    @Test
    void testResolveReturnsExistingProperties() throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        ClusterUpgradeValidationEvent event = new ClusterUpgradeValidationEvent("selector", STACK_ID, TARGET_IMAGE_ID, properties);

        assertEquals(properties, underTest.resolve(event));
    }

    @Test
    void testResolveRebuildsFromLegacyServiceValidationEvent() throws Exception {
        ClusterUpgradeServiceValidationEvent event = new ClusterUpgradeServiceValidationEvent(
                STACK_ID, TARGET_IMAGE_ID, null, true, false, "7.2.18", null, true);
        ClusterUpgradeProperties rebuilt = ClusterUpgradePropertiesTestUtils.withFlags(true, false, true);
        when(clusterUpgradePropertiesFactory.create(argThat(target -> target.getStackId().equals(STACK_ID) && target.getImageId().equals(TARGET_IMAGE_ID)),
                eq(true), eq(false), eq(true))).thenReturn(rebuilt);

        assertEquals(rebuilt, underTest.resolve(event));
        verify(clusterUpgradePropertiesFactory).create(argThat(target -> target.getStackId().equals(STACK_ID) && target.getImageId().equals(TARGET_IMAGE_ID)),
                eq(true), eq(false), eq(true));
    }

    @Test
    void testResolveRebuildsFromLegacyValidationEventWithDefaults() throws Exception {
        ClusterUpgradeValidationEvent event = new ClusterUpgradeValidationEvent("selector", STACK_ID, TARGET_IMAGE_ID, null);
        ClusterUpgradeProperties rebuilt = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        when(clusterUpgradePropertiesFactory.create(argThat(target -> target.getStackId().equals(STACK_ID) && target.getImageId().equals(TARGET_IMAGE_ID)),
                eq(false), eq(true), eq(false))).thenReturn(rebuilt);

        assertEquals(rebuilt, underTest.resolve(event));
        verify(clusterUpgradePropertiesFactory).create(argThat(target -> target.getStackId().equals(STACK_ID) && target.getImageId().equals(TARGET_IMAGE_ID)),
                eq(false), eq(true), eq(false));
    }

    @Test
    void testPreparationAndValidationTriggersReuseExistingProperties() throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(true, false, true);
        assertSame(properties, underTest.resolve(new ClusterUpgradeValidationTriggerEvent(STACK_ID, null,
                properties.targetImageId(), properties.lockComponents(), properties.rollingUpgradeEnabled(), properties.replaceVms(), properties, null)));
        assertSame(properties, underTest.resolve(new ClusterUpgradePreparationTriggerEvent(STACK_ID, null,
                new ImageChangeDto(STACK_ID, properties.targetImageId(), properties.imageCatalogName(), properties.imageCatalogUrl()),
                properties.runtimeVersion(), properties.currentOsType(), properties)));
        assertSame(properties, underTest.resolve(new ClusterUpgradeParcelSettingsPreparationEvent(STACK_ID, null, null, properties)));
        assertSame(properties, underTest.resolve(new ClusterUpgradePreparationEvent("selector", STACK_ID, null, TARGET_IMAGE_ID, properties)));
        verifyNoInteractions(clusterUpgradePropertiesFactory);
    }

    @Test
    void testValidationRequestPreservesTargetCatalogAndFlags() {
        ImageChangeDto image = new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, "custom-catalog", "custom-url");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(true, false, true);
        when(clusterUpgradePropertiesFactory.create(image, true, false, true)).thenReturn(properties);

        ClusterUpgradeValidationTriggerEvent event = new ClusterUpgradeValidationTriggerEvent(STACK_ID, null, image, true, false, true);

        assertSame(properties, underTest.resolve(event));
        verify(clusterUpgradePropertiesFactory).create(image, true, false, true);
    }

    @Test
    void testLegacyValidationTriggerPreservesFlags() throws Exception {
        ClusterUpgradeValidationTriggerEvent event = new ClusterUpgradeValidationTriggerEvent(STACK_ID, null, TARGET_IMAGE_ID, true, false, true, null, null);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(true, false, true);
        when(clusterUpgradePropertiesFactory.create(argThat(target -> target.getStackId().equals(STACK_ID) && target.getImageId().equals(TARGET_IMAGE_ID)),
                eq(true), eq(false), eq(true))).thenReturn(properties);

        assertSame(properties, underTest.resolve(event));
        verify(clusterUpgradePropertiesFactory).create(argThat(target -> target.getStackId().equals(STACK_ID) && target.getImageId().equals(TARGET_IMAGE_ID)),
                eq(true), eq(false), eq(true));
    }

    @Test
    void testLegacyPreparationTriggerPreservesTargetCatalog() throws Exception {
        ImageChangeDto image = new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, "custom-catalog", "custom-url");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(false, false, false);
        when(clusterUpgradePropertiesFactory.create(any(ImageChangeDto.class), eq(false), eq(false), eq(false))).thenReturn(properties);

        assertSame(properties, underTest.resolve(new ClusterUpgradePreparationTriggerEvent(STACK_ID, null, image, "7.3.2", OsType.RHEL8, null)));
        verify(clusterUpgradePropertiesFactory).create(same(image), eq(false), eq(false), eq(false));
    }

    @Test
    void testLegacyParcelSettingsEventRebuildsProperties() throws Exception {
        ImageChangeDto image = new ImageChangeDto(STACK_ID, TARGET_IMAGE_ID, "custom-catalog", "custom-url");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(false, false, false);
        when(clusterUpgradePropertiesFactory.create(any(ImageChangeDto.class), eq(false), eq(false), eq(false))).thenReturn(properties);

        assertSame(properties, underTest.resolve(new ClusterUpgradeParcelSettingsPreparationEvent(STACK_ID, image, OsType.RHEL8, null)));
        verify(clusterUpgradePropertiesFactory).create(argThat(target -> target.getImageId().equals(TARGET_IMAGE_ID)
                && target.getImageCatalogName().equals("custom-catalog") && target.getImageCatalogUrl().equals("custom-url")),
                eq(false), eq(false), eq(false));
    }

    @Test
    void testLegacyPreparationEventPreservesCurrentCatalogFallback() throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withFlags(false, false, false);
        when(clusterUpgradePropertiesFactory.create(any(ImageChangeDto.class), eq(false), eq(false), eq(false))).thenReturn(properties);

        assertSame(properties, underTest.resolve(new ClusterUpgradePreparationEvent("selector", STACK_ID, null, TARGET_IMAGE_ID, null)));
        verify(clusterUpgradePropertiesFactory).create(argThat(target -> target.getImageId().equals(TARGET_IMAGE_ID)
                && target.getStackId().equals(STACK_ID) && target.getImageCatalogName() == null && target.getImageCatalogUrl() == null),
                eq(false), eq(false), eq(false));
    }
}
