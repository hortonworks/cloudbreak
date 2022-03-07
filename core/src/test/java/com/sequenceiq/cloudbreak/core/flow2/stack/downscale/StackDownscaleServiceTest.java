package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.freeipa.InstanceMetadataProcessor;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;

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

    @InjectMocks
    private StackDownscaleService stackDownscaleService;

    @Captor
    private ArgumentCaptor<Iterable<Resource>> resourcesCaptor;

    @Test
    public void finishStackDownscaleAndSaveVolumeFQDNTest() throws TransactionService.TransactionExecutionException {
        StackScalingFlowContext stackScalingFlowContext = mock(StackScalingFlowContext.class);
        Stack stack = mock(Stack.class);
        when(stackScalingFlowContext.getStack()).thenReturn(stack);
        when(stackScalingFlowContext.isRepair()).thenReturn(true);
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setGroupName("master");
        when(stack.getInstanceGroups()).thenReturn(Set.of(masterGroup));
        InstanceMetaData master = new InstanceMetaData();
        master.setInstanceId("i-1111");
        master.setDiscoveryFQDN("master1.cloudera.site");
        master.setPrivateId(1L);
        masterGroup.setInstanceMetaData(Set.of(master));
        ArrayList<Resource> volumes = new ArrayList<>();
        Resource volume1 = new Resource();
        volumes.add(volume1);
        volume1.setInstanceId("i-1111");
        doReturn(Optional.of(new VolumeSetAttributes("az1", false, "",
                new ArrayList<>(), 50, "gp2"))).when(resourceAttributeUtil).getTypedAttributes(volume1, VolumeSetAttributes.class);
        when(resourceService.findByStackIdAndType(any(), any())).thenReturn(volumes);
        stackDownscaleService.finishStackDownscale(stackScalingFlowContext, Set.of(1L));
        verify(resourceService).saveAll(resourcesCaptor.capture());
        Iterable<Resource> resourcesCaptorValue = resourcesCaptor.getValue();
        Json attributes = resourcesCaptorValue.iterator().next().getAttributes();
        assertEquals("master1.cloudera.site", attributes.getValue("discoveryFQDN"));
    }

    @Test
    public void finishStackDownscaleAndDontSaveVolumeFQDNTest() throws TransactionService.TransactionExecutionException {
        StackScalingFlowContext stackScalingFlowContext = mock(StackScalingFlowContext.class);
        Stack stack = mock(Stack.class);
        when(stackScalingFlowContext.getStack()).thenReturn(stack);
        when(stackScalingFlowContext.isRepair()).thenReturn(true);
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setGroupName("master");
        when(stack.getInstanceGroups()).thenReturn(Set.of(masterGroup));
        InstanceMetaData master = new InstanceMetaData();
        master.setInstanceId("i-1111");
        master.setPrivateId(1L);
        master.setDiscoveryFQDN("master1.cloudera.site");
        masterGroup.setInstanceMetaData(Set.of(master));
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
        stackDownscaleService.finishStackDownscale(stackScalingFlowContext, Set.of(1L));
        verify(resourceService).saveAll(resourcesCaptor.capture());
        Iterable<Resource> resourcesCaptorValue = resourcesCaptor.getValue();
        Json attributes = resourcesCaptorValue.iterator().next().getAttributes();
        assertEquals("some.fqdn", attributes.getValue("discoveryFQDN"));
    }

}