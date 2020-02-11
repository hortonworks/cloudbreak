package com.sequenceiq.freeipa.service.instance;

import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_EVENT;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.instance.InstanceEvent;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.FreeIpaHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.RebootInstancesService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
public class RebootInstanceServiceTest {

    private static final String ENVIRONMENT_ID1 = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String ENVIRONMENT_ID2 = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:98765-4321";

    private static final String ACCOUNT_ID = "accountId";

    private static FreeIpaClientException ipaClientException;

    private static Stack stack1;

    private static Stack stack2;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private FreeIpaHealthDetailsService healthDetailsService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private RebootInstancesService underTest;

    @Mock
    private FreeIpaFlowManager flowManager;

    @BeforeAll
    public static void init() {
        ipaClientException = new FreeIpaClientException("failure");

        stack1 = new Stack();
        stack1.setResourceCrn(ENVIRONMENT_ID1);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack1.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        instanceMetaData.setDiscoveryFQDN("host.domain");
        instanceMetaData.setInstanceId("instance_1");

        stack2 = new Stack();
        stack2.setResourceCrn(ENVIRONMENT_ID2);
        instanceGroup = new InstanceGroup();
        stack2.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        instanceMetaData = new InstanceMetaData();
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        instanceMetaData.setDiscoveryFQDN("host1.domain");
        instanceMetaData.setInstanceId("instance_1");
        instanceMetaData = new InstanceMetaData();
        instanceGroup.getInstanceMetaData().add(instanceMetaData);
        instanceMetaData.setDiscoveryFQDN("host2.domain");
        instanceMetaData.setInstanceId("instance_2");
    }

    private HealthDetailsFreeIpaResponse getMockDetails1() {
        HealthDetailsFreeIpaResponse healthDetailsFreeIpaResponse = new HealthDetailsFreeIpaResponse();
        healthDetailsFreeIpaResponse.setCrn(ENVIRONMENT_ID1);
        healthDetailsFreeIpaResponse.setName("test");
        healthDetailsFreeIpaResponse.setStatus(Status.AVAILABLE);
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId("instance_1");
        nodeHealthDetails.setStatus(InstanceStatus.TERMINATED);
        healthDetailsFreeIpaResponse.addNodeHealthDetailsFreeIpaResponses(nodeHealthDetails);
        return healthDetailsFreeIpaResponse;
    }

    private HealthDetailsFreeIpaResponse getMockDetails2() {
        HealthDetailsFreeIpaResponse healthDetailsFreeIpaResponse = new HealthDetailsFreeIpaResponse();
        healthDetailsFreeIpaResponse.setCrn(ENVIRONMENT_ID2);
        healthDetailsFreeIpaResponse.setName("test");
        healthDetailsFreeIpaResponse.setStatus(Status.AVAILABLE);
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId("instance_1");
        nodeHealthDetails.setStatus(InstanceStatus.CREATED);
        healthDetailsFreeIpaResponse.addNodeHealthDetailsFreeIpaResponses(nodeHealthDetails);
        nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId("instance_2");
        nodeHealthDetails.setStatus(InstanceStatus.UNREACHABLE);
        healthDetailsFreeIpaResponse.addNodeHealthDetailsFreeIpaResponses(nodeHealthDetails);
        return healthDetailsFreeIpaResponse;
    }

    @Test
    public void testBasicSuccessReboot() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack1);
        Mockito.when(healthDetailsService.getHealthDetails(any(), any())).thenReturn(getMockDetails1());
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest);

        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(1)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testInstancesSuccessReboot() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack1);
        Mockito.when(healthDetailsService.getHealthDetails(any(), any())).thenReturn(getMockDetails1());
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        rebootInstancesRequest.setInstanceIds(Arrays.asList("instance_1"));
        underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest);

        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(1)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testInvalidInstancesReboot() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack1);
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        rebootInstancesRequest.setInstanceIds(Arrays.asList("bad_instance"));
        Exception expected = assertThrows(BadRequestException.class, () -> {
            underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest);
                });
        assertTrue(expected.getLocalizedMessage().equals("Invalid instanceIds in request bad_instance."));
    }

    @Test
    public void testForceInstancesSuccessReboot() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack1);
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        rebootInstancesRequest.setInstanceIds(Arrays.asList("instance_1"));
        rebootInstancesRequest.setForceReboot(true);
        underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest);

        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(1)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testNonForceAvailableInstanceReboot() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack2);
        Mockito.when(healthDetailsService.getHealthDetails(any(), any())).thenReturn(getMockDetails2());
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID2);
        rebootInstancesRequest.setInstanceIds(Arrays.asList("instance_1"));
        Exception expected = assertThrows(NotFoundException.class, () -> {
                    underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest);
                });
        assertTrue(expected.getLocalizedMessage().equals("No unhealthy instances to reboot.  Maybe use the force option."));
        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(0)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testNonForceMultiInstanceReboot() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack2);
        Mockito.when(healthDetailsService.getHealthDetails(any(), any())).thenReturn(getMockDetails2());
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID2);
        underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest);
        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(1)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testForceMultiInstanceReboot() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack2);
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID2);
        rebootInstancesRequest.setForceReboot(true);
        underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest);
        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(1)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

}
