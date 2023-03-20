package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.freeipa.InstanceMetadataProcessor;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.CommonStatus;

@ExtendWith(MockitoExtension.class)
class StackDownscaleServiceTest {

    @Mock
    private StackScalingService stackScalingService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private ResourceService resourceService;

    @Spy
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private InstanceMetadataProcessor instanceMetadataProcessor;

    @Mock
    private FreeIpaCleanupService freeIpaCleanupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private StackDownscaleService stackDownscaleService;

    @Captor
    private ArgumentCaptor<Iterable<Resource>> resourcesCaptor;

    @Captor
    private ArgumentCaptor<Resource> resourceCaptor;

    @Test
    public void finishStackDownscaleAndSaveVolumeFQDNTest() throws TransactionService.TransactionExecutionException {
        StackScalingFlowContext stackScalingFlowContext = mock(StackScalingFlowContext.class);
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stackScalingFlowContext.getStack()).thenReturn(stack);
        when(stackScalingFlowContext.isRepair()).thenReturn(true);
        InstanceMetaData master = new InstanceMetaData();
        master.setInstanceId("i-1111");
        master.setDiscoveryFQDN("master1.cloudera.site");
        master.setPrivateId(123L);
        ArrayList<Resource> volumes = new ArrayList<>();
        Resource volume1 = new Resource();
        volumes.add(volume1);
        volume1.setInstanceId("i-1111");
        doReturn(Optional.of(new VolumeSetAttributes("az1", false, "",
                new ArrayList<>(), 50, "gp2"))).when(resourceAttributeUtil).getTypedAttributes(volume1, VolumeSetAttributes.class);
        when(resourceService.findByStackIdAndType(any(), any())).thenReturn(volumes);
        when(instanceMetaDataService.getInstanceMetadataViewsByStackIdAndPrivateIds(1L, Set.of(123L))).thenReturn(List.of(master));
        stackDownscaleService.finishStackDownscale(stackScalingFlowContext, Set.of(123L));
        verify(resourceService).saveAll(resourcesCaptor.capture());
        Iterable<Resource> resourcesCaptorValue = resourcesCaptor.getValue();
        Json attributes = resourcesCaptorValue.iterator().next().getAttributes();
        assertEquals("master1.cloudera.site", attributes.getValue("discoveryFQDN"));
    }

    @Test
    public void finishStackDownscaleAndDontSaveVolumeFQDNTest() throws TransactionService.TransactionExecutionException {
        StackScalingFlowContext stackScalingFlowContext = mock(StackScalingFlowContext.class);
        StackView stack = mock(StackView.class);
        when(stackScalingFlowContext.getStack()).thenReturn(stack);
        when(stackScalingFlowContext.isRepair()).thenReturn(true);
        when(stack.getId()).thenReturn(1L);
        InstanceMetaData master = new InstanceMetaData();
        master.setInstanceId("i-1111");
        master.setPrivateId(123L);
        master.setDiscoveryFQDN("master1.cloudera.site");
        ArrayList<Resource> volumes = new ArrayList<>();
        Resource volume1 = new Resource();
        volumes.add(volume1);
        volume1.setInstanceId("i-1111");
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az1", false, "",
                new ArrayList<>(), 50, "gp2");
        volumeSetAttributes.setDiscoveryFQDN("some.fqdn");
        volume1.setAttributes(Json.silent(volumeSetAttributes));
        doReturn(Optional.of(volumeSetAttributes)).when(resourceAttributeUtil).getTypedAttributes(volume1, VolumeSetAttributes.class);
        when(resourceService.findByStackIdAndType(any(), any())).thenReturn(volumes);
        when(instanceMetaDataService.getInstanceMetadataViewsByStackIdAndPrivateIds(1L, Set.of(123L))).thenReturn(List.of(master));
        stackDownscaleService.finishStackDownscale(stackScalingFlowContext, Set.of(123L));
        verify(resourceService).saveAll(resourcesCaptor.capture());
        Iterable<Resource> resourcesCaptorValue = resourcesCaptor.getValue();
        Json attributes = resourcesCaptorValue.iterator().next().getAttributes();
        assertEquals("some.fqdn", attributes.getValue("discoveryFQDN"));
    }

    @Test
    public void finishStackDownscaleAndSaveVolumeFQDNIfInstanceIdNotProperlyFilledAndDiskIsInCreatedStateTest()
            throws TransactionService.TransactionExecutionException {
        StackScalingFlowContext stackScalingFlowContext = mock(StackScalingFlowContext.class);
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stackScalingFlowContext.getStack()).thenReturn(stack);
        when(stackScalingFlowContext.isRepair()).thenReturn(true);
        InstanceMetaData master = new InstanceMetaData();
        master.setInstanceId("i-2222");
        master.setDiscoveryFQDN("master1.cloudera.site");
        master.setPrivateId(123L);
        ArrayList<Resource> volumes = new ArrayList<>();
        Resource volume1 = new Resource();
        volumes.add(volume1);
        volume1.setInstanceId("i-1111");
        volume1.setResourceStatus(CommonStatus.CREATED);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az1", false, "",
                new ArrayList<>(), 50, "gp2");
        volumeSetAttributes.setDiscoveryFQDN("master1.cloudera.site");
        doReturn(Optional.of(volumeSetAttributes)).when(resourceAttributeUtil).getTypedAttributes(volume1, VolumeSetAttributes.class);
        when(resourceService.findByStackIdAndType(any(), any())).thenReturn(volumes);
        when(instanceMetaDataService.getInstanceMetadataViewsByStackIdAndPrivateIds(1L, Set.of(123L))).thenReturn(List.of(master));
        stackDownscaleService.finishStackDownscale(stackScalingFlowContext, Set.of(123L));
        verify(resourceService).saveAll(resourcesCaptor.capture());
        assertEquals(CommonStatus.DETACHED, volume1.getResourceStatus());
        Iterable<Resource> resourcesCaptorValue = resourcesCaptor.getValue();
        Resource diskResource = resourcesCaptorValue.iterator().next();
        assertEquals(CommonStatus.DETACHED, diskResource.getResourceStatus());
    }

    @Test
    public void finishStackDownscaleButMultipleVolumsetForTheSameFQDNSoCleanUpShouldHappen()
            throws TransactionService.TransactionExecutionException {
        StackScalingFlowContext stackScalingFlowContext = mock(StackScalingFlowContext.class);
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stackScalingFlowContext.getStack()).thenReturn(stack);
        when(stackScalingFlowContext.isRepair()).thenReturn(true);
        InstanceMetaData master = new InstanceMetaData();
        master.setInstanceId("i-2222");
        master.setDiscoveryFQDN("master1.cloudera.site");
        master.setPrivateId(123L);
        ArrayList<Resource> volumes = new ArrayList<>();
        Resource volume1 = new Resource();
        volumes.add(volume1);
        volume1.setId(1L);
        volume1.setInstanceId("i-1111");
        volume1.setResourceStatus(CommonStatus.CREATED);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az1", false, "",
                new ArrayList<>(), 50, "gp2");
        volumeSetAttributes.setDiscoveryFQDN("master1.cloudera.site");
        doReturn(Optional.of(volumeSetAttributes)).when(resourceAttributeUtil).getTypedAttributes(volume1, VolumeSetAttributes.class);
        Resource volume2 = new Resource();
        volume2.setId(2L);
        volumes.add(volume2);
        volume2.setInstanceId("i-1122");
        volume2.setResourceStatus(CommonStatus.CREATED);
        VolumeSetAttributes volumeSetAttributes2 = new VolumeSetAttributes("az1", false, "",
                new ArrayList<>(), 50, "gp2");
        volumeSetAttributes2.setDiscoveryFQDN("master1.cloudera.site");
        doReturn(Optional.of(volumeSetAttributes2)).when(resourceAttributeUtil).getTypedAttributes(volume2, VolumeSetAttributes.class);
        when(resourceService.findByStackIdAndType(any(), any())).thenReturn(volumes);
        when(instanceMetaDataService.getInstanceMetadataViewsByStackIdAndPrivateIds(1L, Set.of(123L))).thenReturn(List.of(master));
        stackDownscaleService.finishStackDownscale(stackScalingFlowContext, Set.of(123L));
        verify(resourceService).save(resourceCaptor.capture());
        assertEquals(CommonStatus.FAILED, volume1.getResourceStatus());
        Iterable<Resource> resourcesCaptorValue = resourceCaptor.getAllValues();
        Iterator<Resource> resourceIterator = resourcesCaptorValue.iterator();
        Resource diskResource1 = resourceIterator.next();
        assertEquals(CommonStatus.FAILED, diskResource1.getResourceStatus());
        assertFalse(resourceIterator.hasNext());
    }

}