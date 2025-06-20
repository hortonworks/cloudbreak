package com.sequenceiq.cloudbreak.service.upgrade.defaultoutbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackOutboundTypeValidationV4Response;
import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class StackDefaultOutboundUpgradeServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:env-id";

    private static final String STACK_NAME_1 = "stack1";

    private static final String STACK_NAME_2 = "stack2";

    private static final String STACK_NAME_3 = "stack3";

    private static final Long STACK_ID_1 = 100L;

    private static final Long STACK_ID_2 = 200L;

    private static final Long STACK_ID_3 = 300L;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private StackService stackService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @InjectMocks
    private StackDefaultOutboundUpgradeService underTest;

    @Test
    void testGetStacksWithOutboundTypeWithMultipleStacksAndDifferentOutboundTypes() {
        // Given
        Set<StackListItem> stackList = createStackListItems();
        List<Resource> networks = createNetworkResources();

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(stackList);
        when(resourceService.findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK)))
                .thenReturn(networks);

        // Mock resource attribute util for different scenarios
        NetworkAttributes networkAttributes1 = createNetworkAttributes(OutboundType.LOAD_BALANCER);
        NetworkAttributes networkAttributes2 = createNetworkAttributes(OutboundType.DEFAULT);
        NetworkAttributes networkAttributes3 = createNetworkAttributes(OutboundType.PUBLIC_IP);

        when(resourceAttributeUtil.getTypedAttributes(networks.get(0), NetworkAttributes.class))
                .thenReturn(Optional.of(networkAttributes1));
        when(resourceAttributeUtil.getTypedAttributes(networks.get(1), NetworkAttributes.class))
                .thenReturn(Optional.of(networkAttributes2));
        when(resourceAttributeUtil.getTypedAttributes(networks.get(2), NetworkAttributes.class))
                .thenReturn(Optional.of(networkAttributes3));

        // When
        StackOutboundTypeValidationV4Response result = underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStackOutboundTypeMap());
        assertEquals(3, result.getStackOutboundTypeMap().size());
        assertEquals(OutboundType.LOAD_BALANCER, result.getStackOutboundTypeMap().get(STACK_NAME_1));
        assertEquals(OutboundType.DEFAULT, result.getStackOutboundTypeMap().get(STACK_NAME_2));
        assertEquals(OutboundType.PUBLIC_IP, result.getStackOutboundTypeMap().get(STACK_NAME_3));

        verify(stackService).getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD));
        verify(resourceService).findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK));
    }

    @Test
    void testGetStacksWithOutboundTypeWithNoNetworkResources() {
        // Given
        Set<StackListItem> stackList = createStackListItems();
        List<Resource> emptyNetworks = Collections.emptyList();

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(stackList);
        when(resourceService.findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK)))
                .thenReturn(emptyNetworks);

        // When
        StackOutboundTypeValidationV4Response result = underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStackOutboundTypeMap());
        assertEquals(3, result.getStackOutboundTypeMap().size());
        // All should default to NOT_DEFINED when no network resources found
        assertEquals(OutboundType.NOT_DEFINED, result.getStackOutboundTypeMap().get(STACK_NAME_1));
        assertEquals(OutboundType.NOT_DEFINED, result.getStackOutboundTypeMap().get(STACK_NAME_2));
        assertEquals(OutboundType.NOT_DEFINED, result.getStackOutboundTypeMap().get(STACK_NAME_3));
    }

    @Test
    void testGetStacksWithOutboundTypeWithNoNetworkAttributes() {
        // Given
        Set<StackListItem> stackList = createStackListItems();
        List<Resource> networks = createNetworkResources();

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(stackList);
        when(resourceService.findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK)))
                .thenReturn(networks);

        // Mock resource attribute util to return empty optionals (no attributes)
        when(resourceAttributeUtil.getTypedAttributes(any(Resource.class), eq(NetworkAttributes.class)))
                .thenReturn(Optional.empty());

        // When
        StackOutboundTypeValidationV4Response result = underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStackOutboundTypeMap());
        assertEquals(3, result.getStackOutboundTypeMap().size());
        // All should default to NOT_DEFINED when no network attributes found
        assertEquals(OutboundType.NOT_DEFINED, result.getStackOutboundTypeMap().get(STACK_NAME_1));
        assertEquals(OutboundType.NOT_DEFINED, result.getStackOutboundTypeMap().get(STACK_NAME_2));
        assertEquals(OutboundType.NOT_DEFINED, result.getStackOutboundTypeMap().get(STACK_NAME_3));
    }

    @Test
    void testGetStacksWithOutboundTypeWithEmptyStackList() {
        // Given
        Set<StackListItem> emptyStackList = Collections.emptySet();
        List<Resource> emptyNetworks = Collections.emptyList();

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(emptyStackList);
        when(resourceService.findByStackIdsAndType(Collections.emptyList(), ResourceType.AZURE_NETWORK))
                .thenReturn(emptyNetworks);

        // When
        StackOutboundTypeValidationV4Response result = underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStackOutboundTypeMap());
        assertEquals(0, result.getStackOutboundTypeMap().size());
    }

    @Test
    void testGetStacksWithOutboundTypeWithMixedNetworkResourceAvailability() {
        // Given
        Set<StackListItem> stackList = createStackListItems();
        // Only create network resources for first two stacks
        List<Resource> partialNetworks = Arrays.asList(
                createNetworkResource(STACK_ID_1),
                createNetworkResource(STACK_ID_2)
        );

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(stackList);
        when(resourceService.findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK)))
                .thenReturn(partialNetworks);

        NetworkAttributes networkAttributes1 = createNetworkAttributes(OutboundType.USER_ASSIGNED_NATGATEWAY);
        NetworkAttributes networkAttributes2 = createNetworkAttributes(OutboundType.USER_DEFINED_ROUTING);

        when(resourceAttributeUtil.getTypedAttributes(partialNetworks.get(0), NetworkAttributes.class))
                .thenReturn(Optional.of(networkAttributes1));
        when(resourceAttributeUtil.getTypedAttributes(partialNetworks.get(1), NetworkAttributes.class))
                .thenReturn(Optional.of(networkAttributes2));

        // When
        StackOutboundTypeValidationV4Response result = underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStackOutboundTypeMap());
        assertEquals(3, result.getStackOutboundTypeMap().size());
        assertEquals(OutboundType.USER_ASSIGNED_NATGATEWAY, result.getStackOutboundTypeMap().get(STACK_NAME_1));
        assertEquals(OutboundType.USER_DEFINED_ROUTING, result.getStackOutboundTypeMap().get(STACK_NAME_2));
        assertEquals(OutboundType.NOT_DEFINED, result.getStackOutboundTypeMap().get(STACK_NAME_3));
    }

    @Test
    void testGetStacksWithOutboundTypeWithNullOutboundTypeInNetworkAttributes() {
        // Given
        Set<StackListItem> stackList = Set.of(createStackListItem(STACK_ID_1, STACK_NAME_1));
        List<Resource> networks = List.of(createNetworkResource(STACK_ID_1));

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(stackList);
        when(resourceService.findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK)))
                .thenReturn(networks);

        // Create network attributes with null outbound type
        NetworkAttributes networkAttributes = createNetworkAttributes(null);
        when(resourceAttributeUtil.getTypedAttributes(networks.get(0), NetworkAttributes.class))
                .thenReturn(Optional.of(networkAttributes));

        // When
        StackOutboundTypeValidationV4Response result = underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStackOutboundTypeMap());
        assertEquals(1, result.getStackOutboundTypeMap().size());
        // Should default to NOT_DEFINED when outbound type is null (as per NetworkAttributes.getOutboundType())
        assertEquals(OutboundType.NOT_DEFINED, result.getStackOutboundTypeMap().get(STACK_NAME_1));
    }

    @Test
    void testGetStacksWithOutboundTypeWithSingleStack() {
        // Given
        Set<StackListItem> stackList = Set.of(createStackListItem(STACK_ID_1, STACK_NAME_1));
        List<Resource> networks = List.of(createNetworkResource(STACK_ID_1));

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(stackList);
        when(resourceService.findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK)))
                .thenReturn(networks);

        NetworkAttributes networkAttributes = createNetworkAttributes(OutboundType.LOAD_BALANCER);
        when(resourceAttributeUtil.getTypedAttributes(networks.getFirst(), NetworkAttributes.class))
                .thenReturn(Optional.of(networkAttributes));

        // When
        StackOutboundTypeValidationV4Response result = underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStackOutboundTypeMap());
        assertEquals(1, result.getStackOutboundTypeMap().size());
        assertEquals(OutboundType.LOAD_BALANCER, result.getStackOutboundTypeMap().get(STACK_NAME_1));
    }

    @Test
    void testGetStacksWithOutboundTypeWithAllOutboundTypes() {
        // Given - Test all possible OutboundType values
        Set<StackListItem> stackList = Set.of(
                createStackListItem(100L, "stack-load-balancer"),
                createStackListItem(200L, "stack-default"),
                createStackListItem(300L, "stack-not-defined"),
                createStackListItem(400L, "stack-public-ip"),
                createStackListItem(500L, "stack-user-assigned-natgateway"),
                createStackListItem(600L, "stack-user-defined-routing")
        );

        List<Resource> networks = Arrays.asList(
                createNetworkResource(100L),
                createNetworkResource(200L),
                createNetworkResource(300L),
                createNetworkResource(400L),
                createNetworkResource(500L),
                createNetworkResource(600L)
        );

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(stackList);
        when(resourceService.findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK)))
                .thenReturn(networks);

        // Mock different outbound types
        when(resourceAttributeUtil.getTypedAttributes(networks.get(0), NetworkAttributes.class))
                .thenReturn(Optional.of(createNetworkAttributes(OutboundType.LOAD_BALANCER)));
        when(resourceAttributeUtil.getTypedAttributes(networks.get(1), NetworkAttributes.class))
                .thenReturn(Optional.of(createNetworkAttributes(OutboundType.DEFAULT)));
        when(resourceAttributeUtil.getTypedAttributes(networks.get(2), NetworkAttributes.class))
                .thenReturn(Optional.of(createNetworkAttributes(OutboundType.NOT_DEFINED)));
        when(resourceAttributeUtil.getTypedAttributes(networks.get(3), NetworkAttributes.class))
                .thenReturn(Optional.of(createNetworkAttributes(OutboundType.PUBLIC_IP)));
        when(resourceAttributeUtil.getTypedAttributes(networks.get(4), NetworkAttributes.class))
                .thenReturn(Optional.of(createNetworkAttributes(OutboundType.USER_ASSIGNED_NATGATEWAY)));
        when(resourceAttributeUtil.getTypedAttributes(networks.get(5), NetworkAttributes.class))
                .thenReturn(Optional.of(createNetworkAttributes(OutboundType.USER_DEFINED_ROUTING)));

        // When
        StackOutboundTypeValidationV4Response result = underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStackOutboundTypeMap());
        assertEquals(6, result.getStackOutboundTypeMap().size());
        assertEquals(OutboundType.LOAD_BALANCER, result.getStackOutboundTypeMap().get("stack-load-balancer"));
        assertEquals(OutboundType.DEFAULT, result.getStackOutboundTypeMap().get("stack-default"));
        assertEquals(OutboundType.NOT_DEFINED, result.getStackOutboundTypeMap().get("stack-not-defined"));
        assertEquals(OutboundType.PUBLIC_IP, result.getStackOutboundTypeMap().get("stack-public-ip"));
        assertEquals(OutboundType.USER_ASSIGNED_NATGATEWAY, result.getStackOutboundTypeMap().get("stack-user-assigned-natgateway"));
        assertEquals(OutboundType.USER_DEFINED_ROUTING, result.getStackOutboundTypeMap().get("stack-user-defined-routing"));
    }

    @Test
    void testGetStacksWithOutboundTypeWithResourceAttributeUtilException() {
        // Given
        Set<StackListItem> stackList = Set.of(createStackListItem(STACK_ID_1, STACK_NAME_1));
        List<Resource> networks = List.of(createNetworkResource(STACK_ID_1));

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(stackList);
        when(resourceService.findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK)))
                .thenReturn(networks);

        // Mock resource attribute util to throw exception
        when(resourceAttributeUtil.getTypedAttributes(networks.get(0), NetworkAttributes.class))
                .thenThrow(new RuntimeException("Failed to parse attributes"));

        // When & Then - Exception should propagate
        try {
            underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);
        } catch (RuntimeException e) {
            assertEquals("Failed to parse attributes", e.getMessage());
        }
    }

    @Test
    void testGetStacksWithOutboundTypeWithNetworkResourcesForDifferentStacks() {
        // Given - Test the getResource private method indirectly by having network resources that don't match stack IDs
        Set<StackListItem> stackList = Set.of(
                createStackListItem(STACK_ID_1, STACK_NAME_1),
                createStackListItem(STACK_ID_2, STACK_NAME_2)
        );

        // Create network resources for different stack IDs (999L doesn't match any stack)
        List<Resource> networks = Arrays.asList(
                createNetworkResource(STACK_ID_1),
                createNetworkResource(999L)
        );

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(stackList);
        when(resourceService.findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK)))
                .thenReturn(networks);

        NetworkAttributes networkAttributes = createNetworkAttributes(OutboundType.LOAD_BALANCER);
        when(resourceAttributeUtil.getTypedAttributes(networks.get(0), NetworkAttributes.class))
                .thenReturn(Optional.of(networkAttributes));

        // When
        StackOutboundTypeValidationV4Response result = underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStackOutboundTypeMap());
        assertEquals(2, result.getStackOutboundTypeMap().size());
        assertEquals(OutboundType.LOAD_BALANCER, result.getStackOutboundTypeMap().get(STACK_NAME_1));
        assertEquals(OutboundType.NOT_DEFINED, result.getStackOutboundTypeMap().get(STACK_NAME_2));
    }

    @Test
    void testGetStacksWithOutboundTypeWithMultipleNetworkResourcesForSameStack() {
        // Given - Test case where multiple network resources exist for the same stack (should return first match)
        Set<StackListItem> stackList = Set.of(createStackListItem(STACK_ID_1, STACK_NAME_1));

        List<Resource> networks = Arrays.asList(
                createNetworkResource(STACK_ID_1),
                createNetworkResource(STACK_ID_1)
        );

        when(stackService.getByWorkspaceId(WORKSPACE_ID, ENV_CRN, List.of(StackType.DATALAKE, StackType.WORKLOAD)))
                .thenReturn(stackList);
        when(resourceService.findByStackIdsAndType(anyList(), eq(ResourceType.AZURE_NETWORK)))
                .thenReturn(networks);

        NetworkAttributes networkAttributes1 = createNetworkAttributes(OutboundType.LOAD_BALANCER);

        when(resourceAttributeUtil.getTypedAttributes(networks.get(0), NetworkAttributes.class))
                .thenReturn(Optional.of(networkAttributes1));
        // Note: Second stubbing is not needed since findFirst() only uses the first match

        // When
        StackOutboundTypeValidationV4Response result = underTest.getStacksWithOutboundType(WORKSPACE_ID, ENV_CRN);

        // Then
        assertNotNull(result);
        assertNotNull(result.getStackOutboundTypeMap());
        assertEquals(1, result.getStackOutboundTypeMap().size());
        // Should use the first matching resource (LOAD_BALANCER)
        assertEquals(OutboundType.LOAD_BALANCER, result.getStackOutboundTypeMap().get(STACK_NAME_1));
    }

    private Set<StackListItem> createStackListItems() {
        return Set.of(
                createStackListItem(STACK_ID_1, STACK_NAME_1),
                createStackListItem(STACK_ID_2, STACK_NAME_2),
                createStackListItem(STACK_ID_3, STACK_NAME_3)
        );
    }

    private StackListItem createStackListItem(Long stackId, String stackName) {
        StackListItem stackListItem = mock(StackListItem.class);
        when(stackListItem.getId()).thenReturn(stackId);
        when(stackListItem.getName()).thenReturn(stackName);
        return stackListItem;
    }

    private List<Resource> createNetworkResources() {
        return Arrays.asList(
                createNetworkResource(STACK_ID_1),
                createNetworkResource(STACK_ID_2),
                createNetworkResource(STACK_ID_3)
        );
    }

    private Resource createNetworkResource(Long stackId) {
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AZURE_NETWORK);

        Stack stack = new Stack();
        stack.setId(stackId);
        resource.setStack(stack);

        return resource;
    }

    private NetworkAttributes createNetworkAttributes(OutboundType outboundType) {
        NetworkAttributes networkAttributes = new NetworkAttributes();
        networkAttributes.setOutboundType(outboundType);
        return networkAttributes;
    }
}