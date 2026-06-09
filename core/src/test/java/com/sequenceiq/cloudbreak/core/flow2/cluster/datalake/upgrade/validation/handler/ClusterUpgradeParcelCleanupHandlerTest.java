package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeParcelCleanupHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String IMAGE_ID = "targetImageId";

    @InjectMocks
    private ClusterUpgradeParcelCleanupHandler underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ParcelService parcelService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    @BeforeEach
    void setUp() {
        lenient().when(clusterUpgradePropertiesResolver.resolveUnchecked(any())).thenAnswer(invocation ->
                ((ClusterUpgradeValidationEvent) invocation.getArgument(0)).getClusterUpgradeProperties());
    }

    @Test
    void testParcelCleanupUsesClusterUpgradeProperties() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        Set<ClouderaManagerProduct> products = properties.getAllTargetProducts();
        ClusterUpgradeValidationEvent request = new ClusterUpgradeValidationEvent("selector", STACK_ID, IMAGE_ID, properties);
        HandlerEvent<ClusterUpgradeValidationEvent> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(request);

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelService.getRequiredProductsFromProducts(stackDto, products)).thenReturn(products);

        Selectable result = underTest.doAccept(handlerEvent);

        assertEquals(START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT.event(), ((ClusterUpgradeValidationEvent) result).selector());
        verify(parcelService).getRequiredProductsFromProducts(eq(stackDto), any());
        verify(parcelService).removeUnusedParcelVersions(stackDto, products);
    }
}
