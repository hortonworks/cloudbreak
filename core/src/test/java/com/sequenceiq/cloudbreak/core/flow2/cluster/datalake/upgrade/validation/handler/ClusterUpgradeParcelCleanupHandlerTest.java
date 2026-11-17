package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeImageValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeParcelCleanupHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String IMAGE_ID = "targetImageId";

    private static final long REQUIRED_FREE_SPACE = 19464135L;

    private static final Set<String> WARNING_MESSAGES = Set.of("cloud provider validation warning");

    @InjectMocks
    private ClusterUpgradeParcelCleanupHandler underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ParcelService parcelService;

    @Mock
    private StackDto stackDto;

    @Test
    void testParcelCleanupUsesClusterUpgradeProperties() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        Set<ClouderaManagerProduct> products = properties.getAllTargetProducts();
        ClusterUpgradeImageValidationFinishedEvent request = new ClusterUpgradeImageValidationFinishedEvent("selector", STACK_ID, IMAGE_ID, properties,
                REQUIRED_FREE_SPACE, WARNING_MESSAGES);
        HandlerEvent<ClusterUpgradeImageValidationFinishedEvent> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(request);

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelService.getRequiredProductsForUpgrade(stackDto, properties)).thenReturn(products);

        Selectable result = underTest.doAccept(handlerEvent);

        ClusterUpgradeImageValidationFinishedEvent finishedEvent = assertInstanceOf(ClusterUpgradeImageValidationFinishedEvent.class, result);
        assertEquals(START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT.event(), finishedEvent.selector());
        assertEquals(REQUIRED_FREE_SPACE, finishedEvent.getRequiredFreeSpace());
        assertEquals(WARNING_MESSAGES, finishedEvent.getWarningMessages());
        verify(parcelService).getRequiredProductsForUpgrade(stackDto, properties);
        verify(parcelService).removeUnusedParcelVersions(stackDto, products);
    }

    @Test
    void testParcelCleanupFailureReturnsFailureEvent() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        ClusterUpgradeImageValidationFinishedEvent request = new ClusterUpgradeImageValidationFinishedEvent("selector", STACK_ID, IMAGE_ID, properties,
                REQUIRED_FREE_SPACE, WARNING_MESSAGES);
        HandlerEvent<ClusterUpgradeImageValidationFinishedEvent> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(request);
        when(stackDtoService.getById(STACK_ID)).thenThrow(new IllegalStateException("failure"));

        Selectable result = underTest.doAccept(handlerEvent);

        assertInstanceOf(ClusterUpgradeValidationFailureEvent.class, result);
    }
}
