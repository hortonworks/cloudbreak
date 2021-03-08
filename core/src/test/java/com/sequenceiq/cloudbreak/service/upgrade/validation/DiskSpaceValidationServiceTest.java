package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static java.util.Collections.emptySet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(MockitoJUnitRunner.class)
public class DiskSpaceValidationServiceTest {

    private static final String IMAGE_CATALOG_URL = "image-catalog-url";

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String IMAGE_ID = "image-id";

    private static final long STACK_ID = 1L;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ParcelSizeService parcelSizeService;

    @InjectMocks
    private DiskSpaceValidationService underTest;

    private Stack stack;

    private List<GatewayConfig> gatewayConfigs;

    private Set<Node> nodes;

    private Map<String, String> freeSpaceByNodes;

    @Before
    public void before() {
        stack = createStack();
        gatewayConfigs = Collections.emptyList();
        nodes = Collections.emptySet();
        freeSpaceByNodes = createFreeSpaceByNodesMap();

        when(resourceService.getAllByStackId(STACK_ID)).thenReturn(emptySet());
        when(stackUtil.collectNodesWithDiskData(stack)).thenReturn(nodes);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        when(hostOrchestrator.getFreeDiskSpaceByNodes(nodes, gatewayConfigs)).thenReturn(freeSpaceByNodes);
    }

    @Test
    public void testValidateFreeSpaceForUpgradeShouldNotThrowExceptionWhenThereAreEnoughFreeSpaceForUpgrade() throws CloudbreakException {
        when(parcelSizeService.getAllParcelSize(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack)).thenReturn(900000L);

        underTest.validateFreeSpaceForUpgrade(stack, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);

        verifyMocks();
    }

    @Test
    public void testValidateFreeSpaceForUpgradeShouldThrowExceptionWhenThereAreNoEnoughFreeSpaceAndTheRequiredSpaceIsReturnedInMb() throws CloudbreakException {
        when(parcelSizeService.getAllParcelSize(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack)).thenReturn(920000L);

        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> {
            underTest.validateFreeSpaceForUpgrade(stack, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
        });
        assertEquals("There is not enough free space on the following nodes to perform upgrade operation: [host1]. The required free space is: 898 MB",
                exception.getMessage());

        verifyMocks();
    }

    @Test
    public void testValidateFreeSpaceForUpgradeShouldThrowExceptionWhenThereAreNoEnoughFreeSpaceAndTheRequiredSpaceIsReturnedInGb() throws CloudbreakException {
        when(parcelSizeService.getAllParcelSize(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack)).thenReturn(1750000L);

        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> {
            underTest.validateFreeSpaceForUpgrade(stack, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
        });
        assertEquals("There is not enough free space on the following nodes to perform upgrade operation: [host1, host2]. The required free space is: 1.7 GB",
                exception.getMessage());

        verifyMocks();
    }

    private void verifyMocks() throws CloudbreakException {
        verify(parcelSizeService).getAllParcelSize(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack);
        verify(resourceService).getAllByStackId(STACK_ID);
        verify(stackUtil).collectNodesWithDiskData(stack);
        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(hostOrchestrator).getFreeDiskSpaceByNodes(nodes, gatewayConfigs);
    }

    private Map<String, String> createFreeSpaceByNodesMap() {
        return Map.of(
                "host1", "910000",
                "host2", "1700000");
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        return stack;
    }
}