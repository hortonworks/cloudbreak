package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class HostMetadataSetupTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private HostMetadataSetup hostMetadataSetup;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Captor
    private ArgumentCaptor<Set<InstanceMetaData>> instanceMetadataCaptor;

    @Test
    @DisplayName("when multiple non-gateway instances are repaired and scaled we shouldn't make any changes")
    public void testSetupNewHostMetadataWithMultiNonGatewayInstances() throws CloudbreakException, CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = createInstanceMetadata("id1", InstanceMetadataType.CORE, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData im2 = createInstanceMetadata("id2", InstanceMetadataType.CORE, 2L, "10.0.0.2", "host2", false);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(im1, im2));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(hostOrchestrator.getMembers(any(), any())).thenReturn(Map.of("10.0.0.1", "host1", "10.0.0.2", "host2"));

        hostMetadataSetup.setupNewHostMetadata(STACK_ID, List.of("10.0.0.1", "10.0.0.2"));

        verify(instanceMetaDataService, times(0)).getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID);
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetadataCaptor.capture());
        Set<InstanceMetaData> actualIm = instanceMetadataCaptor.getValue();
        InstanceMetaData expectedIm1 = createInstanceMetadata("id1", InstanceMetadataType.CORE, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData expectedIm2 = createInstanceMetadata("id2", InstanceMetadataType.CORE, 2L, "10.0.0.2", "host2", false);
        assertMetadataEquals(expectedIm1, actualIm.stream().filter(im -> im.getPrivateId() == 1L).findFirst().get());
        assertMetadataEquals(expectedIm2, actualIm.stream().filter(im -> im.getPrivateId() == 2L).findFirst().get());
    }

    @Test
    @DisplayName("when a single primary gateway is repaired and scaled we should not make any changes in the primary gateway assignments")
    public void testSetupNewHostMetadataWithSingplePrimaryChange() throws CloudbreakException, CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY_PRIMARY, 1L, "10.0.0.1", "host1", true);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(im1));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(hostOrchestrator.getMembers(any(), any())).thenReturn(Map.of("10.0.0.1", "host1"));

        hostMetadataSetup.setupNewHostMetadata(STACK_ID, List.of("10.0.0.1"));

        verify(instanceMetaDataService, times(0)).getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID);
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetadataCaptor.capture());
        InstanceMetaData expectedMetadata = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY_PRIMARY, 1L, "10.0.0.1", "host1", true);
        assertMetadataEquals(expectedMetadata, instanceMetadataCaptor.getValue().iterator().next());
    }

    @Test
    @DisplayName("when multiple non-primary gateways are repaired and scaled we should not make any changes in the primary gateway assignments")
    public void testSetupNewHostMetadataWithMultipleNonPrimaryChange() throws CloudbreakException, CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData im2 = createInstanceMetadata("id2", InstanceMetadataType.GATEWAY, 2L, "10.0.0.2", "host2", false);
        Set<InstanceMetaData> allNewInstances = Set.of(im1, im2);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allNewInstances);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(hostOrchestrator.getMembers(any(), any())).thenReturn(Map.of("10.0.0.1", "host1", "10.0.0.2", "host2"));

        hostMetadataSetup.setupNewHostMetadata(STACK_ID, List.of("10.0.0.1", "10.0.0.2"));

        verify(instanceMetaDataService, times(0)).getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID);
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetadataCaptor.capture());
        Set<InstanceMetaData> actualIm = instanceMetadataCaptor.getValue();
        InstanceMetaData expectedIm1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData expectedIm2 = createInstanceMetadata("id2", InstanceMetadataType.GATEWAY, 2L, "10.0.0.2", "host2", false);
        assertMetadataEquals(expectedIm1, actualIm.stream().filter(im -> im.getPrivateId() == 1L).findFirst().get());
        assertMetadataEquals(expectedIm2, actualIm.stream().filter(im -> im.getPrivateId() == 2L).findFirst().get());
    }

    @Test
    @DisplayName("when multiple non-primary gateways and a single primary gateway with correct assignments are repaired and scaled "
            + "we should not make any changes in the primary gateway assignments. this has an assumption that the previous primary "
            + "gateway has the same hostname as the current one")
    public void testSetupNewHostMetadataWithMultipleNonPrimaryAndOnePrimaryChangeWithCorrectAssignment()
            throws CloudbreakException, CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData im2 = createInstanceMetadata("id2", InstanceMetadataType.GATEWAY, 2L, "10.0.0.2", "host2", false);
        InstanceMetaData im3 = createInstanceMetadata("id3", InstanceMetadataType.GATEWAY_PRIMARY, 3L, "10.0.0.3", "host3", true);
        Set<InstanceMetaData> allNewInstances = Set.of(im1, im2, im3);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allNewInstances);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(hostOrchestrator.getMembers(any(), any())).thenReturn(Map.of(
                "10.0.0.1", "host1", "10.0.0.2", "host2", "10.0.0.3", "host3"));
        InstanceMetaData lastPrimary = createInstanceMetadata("id0", InstanceMetadataType.GATEWAY_PRIMARY, 59L, "10.0.0.150", "host3", true);
        when(instanceMetaDataService.getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID)).thenReturn(Optional.of(lastPrimary));

        hostMetadataSetup.setupNewHostMetadata(STACK_ID, List.of("10.0.0.1", "10.0.0.2", "10.0.0.3"));

        verify(instanceMetaDataService, times(1)).getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID);
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetadataCaptor.capture());
        InstanceMetaData expectedIm1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData expectedIm2 = createInstanceMetadata("id2", InstanceMetadataType.GATEWAY, 2L, "10.0.0.2", "host2", false);
        InstanceMetaData expectedIm3 = createInstanceMetadata("id3", InstanceMetadataType.GATEWAY_PRIMARY, 3L, "10.0.0.3", "host3", true);
        Set<InstanceMetaData> actualIm = instanceMetadataCaptor.getValue();
        assertMetadataEquals(expectedIm1, actualIm.stream().filter(im -> im.getPrivateId() == 1L).findFirst().get());
        assertMetadataEquals(expectedIm2, actualIm.stream().filter(im -> im.getPrivateId() == 2L).findFirst().get());
        assertMetadataEquals(expectedIm3, actualIm.stream().filter(im -> im.getPrivateId() == 3L).findFirst().get());
    }

    @Test
    @DisplayName("when multiple non-primary gateways and a single primary gateway with incorrect assignments are repaired and scaled "
            + "we should change the primary gateway to match the previous one. this has an assumption that there is a previous primary gateway")
    public void testSetupNewHostMetadataWithMultipleNonPrimaryAndOnePrimaryChangeWithIncorrectAssignment()
            throws CloudbreakException, CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData im2 = createInstanceMetadata("id2", InstanceMetadataType.GATEWAY_PRIMARY, 2L, "10.0.0.2", "host2", true);
        InstanceMetaData im3 = createInstanceMetadata("id3", InstanceMetadataType.GATEWAY, 3L, "10.0.0.3", "host3", false);
        Set<InstanceMetaData> allNewInstances = Set.of(im1, im2, im3);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allNewInstances);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(hostOrchestrator.getMembers(any(), any())).thenReturn(Map.of(
                "10.0.0.1", "host1", "10.0.0.2", "host2", "10.0.0.3", "host3"));
        InstanceMetaData lastPrimary = createInstanceMetadata("id0", InstanceMetadataType.GATEWAY_PRIMARY, 59L, "10.0.0.150", "host3", true);
        when(instanceMetaDataService.getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID)).thenReturn(Optional.of(lastPrimary));

        hostMetadataSetup.setupNewHostMetadata(STACK_ID, List.of("10.0.0.1", "10.0.0.2", "10.0.0.3"));

        verify(instanceMetaDataService, times(1)).getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID);
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetadataCaptor.capture());
        InstanceMetaData expectedIm1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData expectedIm2 = createInstanceMetadata("id2", InstanceMetadataType.GATEWAY, 2L, "10.0.0.2", "host2", false);
        InstanceMetaData expectedIm3 = createInstanceMetadata("id3", InstanceMetadataType.GATEWAY_PRIMARY, 3L, "10.0.0.3", "host3", true);
        Set<InstanceMetaData> actualIm = instanceMetadataCaptor.getValue();
        assertMetadataEquals(expectedIm1, actualIm.stream().filter(im -> im.getPrivateId() == 1L).findFirst().get());
        assertMetadataEquals(expectedIm2, actualIm.stream().filter(im -> im.getPrivateId() == 2L).findFirst().get());
        assertMetadataEquals(expectedIm3, actualIm.stream().filter(im -> im.getPrivateId() == 3L).findFirst().get());
    }

    @Test
    @DisplayName("when multiple non-primary gateways and a single primary gateway with incorrect assignments are repaired and scaled "
            + "we should not make any changes in the primary gateway assignments if there are no previous primary gateways")
    public void testSetupNewHostMetadataWithMultipleNonPrimaryAndOnePrimaryChangeWithIncorrectAssignmentNoPrevious()
            throws CloudbreakException, CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData im2 = createInstanceMetadata("id2", InstanceMetadataType.GATEWAY_PRIMARY, 2L, "10.0.0.2", "host2", true);
        InstanceMetaData im3 = createInstanceMetadata("id3", InstanceMetadataType.GATEWAY, 3L, "10.0.0.3", "host3", false);
        Set<InstanceMetaData> allNewInstances = Set.of(im1, im2, im3);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allNewInstances);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(hostOrchestrator.getMembers(any(), any())).thenReturn(Map.of(
                "10.0.0.1", "host1", "10.0.0.2", "host2", "10.0.0.3", "host3"));
        when(instanceMetaDataService.getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID)).thenReturn(Optional.empty());

        hostMetadataSetup.setupNewHostMetadata(STACK_ID, List.of("10.0.0.1", "10.0.0.2", "10.0.0.3"));

        verify(instanceMetaDataService, times(1)).getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID);
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetadataCaptor.capture());
        InstanceMetaData expectedIm1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData expectedIm2 = createInstanceMetadata("id2", InstanceMetadataType.GATEWAY_PRIMARY, 2L, "10.0.0.2", "host2", true);
        InstanceMetaData expectedIm3 = createInstanceMetadata("id3", InstanceMetadataType.GATEWAY, 3L, "10.0.0.3", "host3", false);
        Set<InstanceMetaData> actualIm = instanceMetadataCaptor.getValue();
        assertMetadataEquals(expectedIm1, actualIm.stream().filter(im -> im.getPrivateId() == 1L).findFirst().get());
        assertMetadataEquals(expectedIm2, actualIm.stream().filter(im -> im.getPrivateId() == 2L).findFirst().get());
        assertMetadataEquals(expectedIm3, actualIm.stream().filter(im -> im.getPrivateId() == 3L).findFirst().get());
    }

    @Test
    @DisplayName("when multiple non-primary gateways and a single primary gateway with incorrect assignments are repaired and scaled "
            + "we should not make any changes in the primary gateway assignments if there is a previous primary gateway but the new "
            + "instances do not match the hostname")
    public void testSetupNewHostMetadataWithMultipleNonPrimaryAndOnePrimaryChangeWithIncorrectAssignmentNoMatchingPrevious()
            throws CloudbreakException, CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData im2 = createInstanceMetadata("id2", InstanceMetadataType.GATEWAY_PRIMARY, 2L, "10.0.0.2", "host2", true);
        InstanceMetaData im3 = createInstanceMetadata("id3", InstanceMetadataType.GATEWAY, 3L, "10.0.0.3", "host3", false);
        Set<InstanceMetaData> allNewInstances = Set.of(im1, im2, im3);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allNewInstances);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(hostOrchestrator.getMembers(any(), any())).thenReturn(Map.of(
                "10.0.0.1", "host1", "10.0.0.2", "host2", "10.0.0.3", "host3"));
        InstanceMetaData lastPrimary = createInstanceMetadata("id0", InstanceMetadataType.GATEWAY_PRIMARY, 59L, "10.0.0.150", "host4", true);
        when(instanceMetaDataService.getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID)).thenReturn(Optional.of(lastPrimary));

        hostMetadataSetup.setupNewHostMetadata(STACK_ID, List.of("10.0.0.1", "10.0.0.2", "10.0.0.3"));

        verify(instanceMetaDataService, times(1)).getLastTerminatedPrimaryGatewayInstanceMetadata(STACK_ID);
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetadataCaptor.capture());
        InstanceMetaData expectedIm1 = createInstanceMetadata("id1", InstanceMetadataType.GATEWAY, 1L, "10.0.0.1", "host1", false);
        InstanceMetaData expectedIm2 = createInstanceMetadata("id2", InstanceMetadataType.GATEWAY_PRIMARY, 2L, "10.0.0.2", "host2", true);
        InstanceMetaData expectedIm3 = createInstanceMetadata("id3", InstanceMetadataType.GATEWAY, 3L, "10.0.0.3", "host3", false);
        Set<InstanceMetaData> actualIm = instanceMetadataCaptor.getValue();
        assertMetadataEquals(expectedIm1, actualIm.stream().filter(im -> im.getPrivateId() == 1L).findFirst().get());
        assertMetadataEquals(expectedIm2, actualIm.stream().filter(im -> im.getPrivateId() == 2L).findFirst().get());
        assertMetadataEquals(expectedIm3, actualIm.stream().filter(im -> im.getPrivateId() == 3L).findFirst().get());
    }

    private InstanceGroup createInstanceGroup(InstanceGroupType instanceGroupType, String groupName) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(instanceGroupType);
        instanceGroup.setGroupName(groupName);
        return instanceGroup;
    }

    private InstanceMetaData createInstanceMetadata(String instanceId, InstanceMetadataType imt, long privateId,
            String privateIp, String fqdn, boolean server) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceMetaData.setInstanceId(instanceId);
        instanceMetaData.setInstanceMetadataType(imt);
        instanceMetaData.setPrivateId(privateId);
        instanceMetaData.setPrivateIp(privateIp);
        instanceMetaData.setDiscoveryFQDN(fqdn);
        instanceMetaData.setServer(server);
        return instanceMetaData;
    }

    private void assertMetadataEquals(InstanceMetaData expected, InstanceMetaData actual) {
        Assertions.assertAll(
                () -> assertEquals(expected.getInstanceStatus(), actual.getInstanceStatus()),
                () -> assertEquals(expected.getInstanceId(), actual.getInstanceId()),
                () -> assertEquals(expected.getInstanceMetadataType(), actual.getInstanceMetadataType()),
                () -> assertEquals(expected.getPrivateId(), actual.getPrivateId()),
                () -> assertEquals(expected.getPrivateIp(), actual.getPrivateIp()),
                () -> assertEquals(expected.getDiscoveryFQDN(), actual.getDiscoveryFQDN()),
                () -> assertEquals(expected.getClusterManagerServer(), actual.getClusterManagerServer()));
    }

}