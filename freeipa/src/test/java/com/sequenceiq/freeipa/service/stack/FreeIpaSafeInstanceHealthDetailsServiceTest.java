package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class FreeIpaSafeInstanceHealthDetailsServiceTest {

    @Mock
    private FreeIpaInstanceHealthDetailsService healthDetailsService;

    @InjectMocks
    private FreeIpaSafeInstanceHealthDetailsService underTest;

    @Test
    void safeGetInstanceHealthDetailsSuccess() throws Exception {
        InstanceMetaData instance = getInstance();
        Stack stack = getStack(Set.of(instance));
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        when(healthDetailsService.getInstanceHealthDetails(stack, instance)).thenReturn(nodeHealthDetails);

        NodeHealthDetails result = underTest.getInstanceHealthDetails(stack, instance);

        assertEquals(nodeHealthDetails, result);
        verify(healthDetailsService).getInstanceHealthDetails(stack, instance);
    }

    @Test
    void safeGetInstanceHealthDetailsFailure() throws Exception {
        InstanceMetaData instance = getInstance();
        Stack stack = getStack(Set.of(instance));
        FreeIpaClientException cause = new FreeIpaClientException("cause");
        doThrow(cause).when(healthDetailsService).getInstanceHealthDetails(stack, instance);

        NodeHealthDetails result = underTest.getInstanceHealthDetails(stack, instance);

        assertEquals(InstanceStatus.UNREACHABLE, result.getStatus());
        assertEquals(List.of(cause.getMessage()), result.getIssues());
        verify(healthDetailsService).getInstanceHealthDetails(stack, instance);
    }

    private InstanceMetaData getInstance() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("i-1");
        instanceMetaData.setDiscoveryFQDN("host-1");
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        return instanceMetaData;
    }

    private Stack getStack(Set<InstanceMetaData> instanceMetaData) {
        Stack stack = new Stack();
        stack.setResourceCrn("crn");
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        instanceGroup.setInstanceMetaData(instanceMetaData);
        return stack;
    }

}
