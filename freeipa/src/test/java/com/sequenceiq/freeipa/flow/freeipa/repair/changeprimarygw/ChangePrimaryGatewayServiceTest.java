package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;

@ExtendWith(MockitoExtension.class)
class ChangePrimaryGatewayServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_ID_1 = "i-1";

    private static final String INSTANCE_ID_2 = "i-2";

    private static final String INSTANCE_ID_3 = "i-3";

    private static final String HOSTNAME_1 = "example1.com";

    private static final String HOSTNAME_2 = "example2.com";

    private static final String HOSTNAME_3 = "example3.com";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @InjectMocks
    private ChangePrimaryGatewayService underTest;

    @Test
    void testGetPrimaryGatewayInstanceIdWhenPresent() {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);

        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(gatewayConfig.getInstanceId()).thenReturn(INSTANCE_ID_1);

        assertEquals(Optional.of(INSTANCE_ID_1), underTest.getPrimaryGatewayInstanceId(stack));

        verify(gatewayConfigService).getPrimaryGatewayConfig(eq(stack));
    }

    @Test
    void testGetPrimaryGatewayInstanceIdWhenNotPresent() {
        Stack stack = mock(Stack.class);

        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenThrow(NotFoundException.class);

        assertEquals(Optional.empty(), underTest.getPrimaryGatewayInstanceId(stack));

        verify(gatewayConfigService).getPrimaryGatewayConfig(eq(stack));
    }

    @Test
    void testGetNewPrimaryGatewayInstanceIdWhenExistingPrimaryGwIsGood() throws Exception {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);

        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(any())).thenReturn(gatewayConfig);
        when(hostOrchestrator.getFreeIpaMasterHostname(any(), any())).thenReturn(Optional.of(HOSTNAME_1));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(im1, im2));
        when(im1.getDiscoveryFQDN()).thenReturn(HOSTNAME_1);
        lenient().when(im2.getDiscoveryFQDN()).thenReturn(HOSTNAME_2);
        when(im1.getInstanceId()).thenReturn(INSTANCE_ID_1);
        lenient().when(im2.getInstanceId()).thenReturn(INSTANCE_ID_2);

        assertEquals(INSTANCE_ID_1, underTest.selectNewPrimaryGatewayInstanceId(stack, List.of()));

        verify(gatewayConfigService).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator).getFreeIpaMasterHostname(eq(gatewayConfig), any());
    }

    @Test
    void testGetNewPrimaryGatewayInstanceIdWhenPrimaryGwDoesNotExist() throws Exception {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);

        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenThrow(NotFoundException.class);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(im1, im2));
        when(im1.getInstanceId()).thenReturn(INSTANCE_ID_1);
        lenient().when(im2.getInstanceId()).thenReturn(INSTANCE_ID_2);

        assertEquals(INSTANCE_ID_1, underTest.selectNewPrimaryGatewayInstanceId(stack, List.of(INSTANCE_ID_2)));

        verify(gatewayConfigService).getPrimaryGatewayConfig(eq(stack));
    }

    @Test
    void testGetNewPrimaryGatewayInstanceIdWhenPrimaryGwDoesNotRespond() throws Exception {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);

        when(hostOrchestrator.getFreeIpaMasterHostname(any(), any())).thenThrow(CloudbreakOrchestratorFailedException.class);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(im1, im2));
        when(im1.getInstanceId()).thenReturn(INSTANCE_ID_1);
        lenient().when(im2.getInstanceId()).thenReturn(INSTANCE_ID_2);

        assertEquals(INSTANCE_ID_1, underTest.selectNewPrimaryGatewayInstanceId(stack, List.of(INSTANCE_ID_2)));

        verify(gatewayConfigService).getPrimaryGatewayConfig(eq(stack));
    }

    @Test
    void testGetNewPrimaryGatewayInstanceIdWhenExistingPrimaryGwIsOnListToAvoid() throws Exception {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);
        Node node = mock(Node.class);
        Set<Node> allNodes = Set.of(node);

        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(any())).thenReturn(gatewayConfig);
        when(gatewayConfig.getInstanceId()).thenReturn(INSTANCE_ID_1);
        when(freeIpaNodeUtilService.mapInstancesToNodes(any())).thenReturn(allNodes);
        when(hostOrchestrator.getFreeIpaMasterHostname(any(), any())).thenReturn(Optional.of(HOSTNAME_1));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(im1, im2));
        when(im1.getDiscoveryFQDN()).thenReturn(HOSTNAME_1);
        lenient().when(im2.getDiscoveryFQDN()).thenReturn(HOSTNAME_2);
        when(im1.getInstanceId()).thenReturn(INSTANCE_ID_1);
        lenient().when(im2.getInstanceId()).thenReturn(INSTANCE_ID_2);

        assertEquals(INSTANCE_ID_2, underTest.selectNewPrimaryGatewayInstanceId(stack, List.of(INSTANCE_ID_1)));

        verify(gatewayConfigService).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator).getFreeIpaMasterHostname(eq(gatewayConfig), eq(allNodes));
    }

    @Test
    void testGetNewPrimaryGatewayInstanceIdWhenExistingPrimaryGwAndMasterIsOnListToAvoid() throws Exception {
        Stack stack = mock(Stack.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);
        InstanceMetaData im3 = mock(InstanceMetaData.class);
        Node node = mock(Node.class);
        Set<Node> allNodes = Set.of(node);

        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(any())).thenReturn(gatewayConfig);
        when(gatewayConfig.getInstanceId()).thenReturn(INSTANCE_ID_2);
        when(freeIpaNodeUtilService.mapInstancesToNodes(any())).thenReturn(allNodes);
        when(hostOrchestrator.getFreeIpaMasterHostname(any(), any())).thenReturn(Optional.of(HOSTNAME_1));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(im1, im2, im3));
        when(im1.getDiscoveryFQDN()).thenReturn(HOSTNAME_1);
        lenient().when(im2.getDiscoveryFQDN()).thenReturn(HOSTNAME_2);
        lenient().when(im3.getDiscoveryFQDN()).thenReturn(HOSTNAME_3);
        when(im1.getInstanceId()).thenReturn(INSTANCE_ID_1);
        lenient().when(im2.getInstanceId()).thenReturn(INSTANCE_ID_2);
        when(im3.getInstanceId()).thenReturn(INSTANCE_ID_3);

        assertEquals(INSTANCE_ID_3, underTest.selectNewPrimaryGatewayInstanceId(stack, List.of(INSTANCE_ID_1, INSTANCE_ID_2)));

        verify(gatewayConfigService).getPrimaryGatewayConfig(eq(stack));
        verify(hostOrchestrator).getFreeIpaMasterHostname(eq(gatewayConfig), eq(allNodes));
    }

    @Test
    void testChangePrimaryGatewayMetadataWhenFormerPgwIsNewPgwThenItDoesNotSaveMetadata() {
        Stack stack = mock(Stack.class);

        underTest.changePrimaryGatewayMetadata(stack, Optional.of(INSTANCE_ID_1), INSTANCE_ID_1);

        verify(instanceMetaDataRepository, never()).save(any());
    }

    @Test
    void testChangePrimaryGatewayMetadataWhenFormerPgwIsEmptyThenItSavesMetadata() {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);

        when(stack.getId()).thenReturn(STACK_ID);
        when(instanceMetaDataRepository.findNotTerminatedForStack(anyLong())).thenReturn(Set.of(im1, im2));
        when(im1.getInstanceId()).thenReturn(INSTANCE_ID_1);
        lenient().when(im2.getInstanceId()).thenReturn(INSTANCE_ID_2);

        underTest.changePrimaryGatewayMetadata(stack, Optional.empty(), INSTANCE_ID_1);

        verify(im1).setInstanceMetadataType(eq(InstanceMetadataType.GATEWAY_PRIMARY));
        verify(instanceMetaDataRepository).save(eq(im1));
    }

    @Test
    void testChangePrimaryGatewayMetadataWhenFormerPgwExiststThenItSavesMetadata() {
        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);
        InstanceMetaData im3 = mock(InstanceMetaData.class);

        when(stack.getId()).thenReturn(STACK_ID);
        when(instanceMetaDataRepository.findNotTerminatedForStack(anyLong())).thenReturn(Set.of(im1, im2, im3));
        when(im1.getInstanceId()).thenReturn(INSTANCE_ID_1);
        when(im2.getInstanceId()).thenReturn(INSTANCE_ID_2);
        lenient().when(im3.getInstanceId()).thenReturn(INSTANCE_ID_3);

        underTest.changePrimaryGatewayMetadata(stack, Optional.of(INSTANCE_ID_1), INSTANCE_ID_2);

        verify(im1).setInstanceMetadataType(eq(InstanceMetadataType.GATEWAY));
        verify(im2).setInstanceMetadataType(eq(InstanceMetadataType.GATEWAY_PRIMARY));
        verify(instanceMetaDataRepository).save(eq(im1));
        verify(instanceMetaDataRepository).save(eq(im2));
    }
}