package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
class FreeIpaStackHealthDetailsServiceTest {
    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String ACCOUNT_ID = "accountId";

    private static final String HOST1 = "host1.domain";

    private static final String HOST2 = "host2.domain";

    private static final String INSTANCE_ID1 = "i-0123456789";

    private static final String INSTANCE_ID2 = "i-9876543210";

    @Mock
    private Tracer tracer;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaSafeInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    @InjectMocks
    private FreeIpaStackHealthDetailsService underTest;

    private NodeHealthDetails getGoodDetails1() {
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId(INSTANCE_ID1);
        nodeHealthDetails.setName(HOST1);
        nodeHealthDetails.setStatus(InstanceStatus.CREATED);
        return nodeHealthDetails;
    }

    private NodeHealthDetails getGoodDetails2() {
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId(INSTANCE_ID2);
        nodeHealthDetails.setName(HOST2);
        nodeHealthDetails.setStatus(InstanceStatus.CREATED);
        return nodeHealthDetails;
    }

    private NodeHealthDetails getUnhealthyDetails1() {
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId(INSTANCE_ID1);
        nodeHealthDetails.setName(HOST1);
        nodeHealthDetails.setStatus(InstanceStatus.UNHEALTHY);
        nodeHealthDetails.setIssues(List.of("failed"));
        return nodeHealthDetails;
    }

    private NodeHealthDetails getUnhealthyDetails2() {
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId(INSTANCE_ID2);
        nodeHealthDetails.setName(HOST2);
        nodeHealthDetails.setStatus(InstanceStatus.UNHEALTHY);
        nodeHealthDetails.setIssues(List.of("failed"));
        return nodeHealthDetails;
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID1);
        instanceMetaData.setDiscoveryFQDN(HOST1);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        return stack;
    }

    private Stack getStackTwoInstances(InstanceMetaData im1, InstanceMetaData im2) {
        Stack stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        instanceGroup.setInstanceMetaData(Set.of(im1, im2));
        return stack;
    }

    private Stack getDeletedStack() {
        Stack stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
        instanceMetaData.setInstanceId(INSTANCE_ID1);
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        instanceMetaData.setDiscoveryFQDN(HOST1);
        return stack;
    }

    private InstanceMetaData getInstance1() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID1);
        instanceMetaData.setDiscoveryFQDN(HOST1);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        return instanceMetaData;
    }

    private InstanceMetaData getInstance2() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID2);
        instanceMetaData.setDiscoveryFQDN(HOST2);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        return instanceMetaData;
    }

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(freeIpaInstanceHealthDetailsService.createNodeResponseWithStatusAndIssue(any(), any(), anyString())).thenCallRealMethod();
    }

    @Test
    void testNodeDeletedOnProvider() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getDeletedStack());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertFalse(response.getNodeHealthDetails().isEmpty());
        assertSame(response.getNodeHealthDetails().stream().findFirst().get().getStatus(), InstanceStatus.TERMINATED);
    }

    @Test
    void testHealthySingleNode() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStack());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), any())).thenReturn(getGoodDetails1());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        assertEquals(Status.AVAILABLE, response.getStatus());
        assertFalse(response.getNodeHealthDetails().isEmpty());
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            assertTrue(nodeHealth.getIssues().isEmpty());
            assertEquals(InstanceStatus.CREATED, nodeHealth.getStatus());
        }
    }

    @Test
    void testUnhealthySingleNode() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStack());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), any())).thenReturn(getUnhealthyDetails1());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertFalse(response.getNodeHealthDetails().isEmpty());
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            assertFalse(nodeHealth.getIssues().isEmpty());
            assertEquals(InstanceStatus.UNHEALTHY, nodeHealth.getStatus());
        }
    }

    @Test
    void testTwoGoodNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im1))).thenReturn(getGoodDetails1());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im2))).thenReturn(getGoodDetails2());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        assertEquals(Status.AVAILABLE, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());
    }

    @Test
    void testOneGoodOneUnhealthyNode() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im1))).thenReturn(getGoodDetails1());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im2))).thenReturn(getUnhealthyDetails2());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());
    }

    @Test
    void testTwoUnhealthyNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im1))).thenReturn(getUnhealthyDetails1());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im2))).thenReturn(getUnhealthyDetails2());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());
    }

    @Test
    void testOneStoppedOneGoodNode() throws Exception {
        InstanceMetaData im1 = getInstance1();
        im1.setInstanceStatus(InstanceStatus.STOPPED);
        InstanceMetaData im2 = getInstance2();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im2))).thenReturn(getUnhealthyDetails2());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());
    }

    @Test
    void testTwoStoppedNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        im1.setInstanceStatus(InstanceStatus.STOPPED);
        InstanceMetaData im2 = getInstance2();
        im2.setInstanceStatus(InstanceStatus.STOPPED);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        assertEquals(Status.STOPPED, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());
    }

    @Test
    void testTwoFailedNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        im1.setInstanceStatus(InstanceStatus.FAILED);
        InstanceMetaData im2 = getInstance2();
        im2.setInstanceStatus(InstanceStatus.FAILED);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        assertEquals(Status.UNHEALTHY, response.getStatus());
        assertEquals(2, response.getNodeHealthDetails().size());
    }

}
