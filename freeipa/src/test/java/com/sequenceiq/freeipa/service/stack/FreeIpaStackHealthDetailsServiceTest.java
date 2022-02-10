package com.sequenceiq.freeipa.service.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
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
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
public class FreeIpaStackHealthDetailsServiceTest {
    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String ACCOUNT_ID = "accountId";

    private static FreeIpaClientException ipaClientException;

    private static final String HOST1 = "host1.domain";

    private static final String HOST2 = "host2.domain";

    private static final String INSTANCE_ID1 = "i-0123456789";

    private static final String INSTANCE_ID2 = "i-9876543210";

    @Mock
    private Tracer tracer;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

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

    @BeforeAll
    public static void init() {
        ipaClientException = new FreeIpaClientException("Error during healthcheck");
    }

    @Test
    public void testNodeDeletedOnProvider() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getDeletedStack());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertFalse(response.getNodeHealthDetails().isEmpty());
        Assert.assertTrue(response.getNodeHealthDetails().stream().findFirst().get().getStatus() == InstanceStatus.TERMINATED);
    }

    @Test
    public void testHealthySingleNode() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStack());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), any())).thenReturn(getGoodDetails1());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.AVAILABLE, response.getStatus());
        Assert.assertFalse(response.getNodeHealthDetails().isEmpty());
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            Assert.assertTrue(nodeHealth.getIssues().isEmpty());
            Assert.assertEquals(InstanceStatus.CREATED, nodeHealth.getStatus());
        }
    }

    @Test
    public void testUnhealthySingleNode() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStack());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), any())).thenReturn(getUnhealthyDetails1());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertFalse(response.getNodeHealthDetails().isEmpty());
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            Assert.assertFalse(nodeHealth.getIssues().isEmpty());
            Assert.assertEquals(InstanceStatus.UNHEALTHY, nodeHealth.getStatus());
        }
    }

    @Test
    public void testUnresponsiveSingleNode() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStack());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), any())).thenThrow(ipaClientException);
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNREACHABLE, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 1);
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            Assert.assertTrue(!nodeHealth.getIssues().isEmpty());
            Assert.assertEquals(InstanceStatus.UNREACHABLE, nodeHealth.getStatus());
            Assert.assertTrue(nodeHealth.getIssues().size() == 1);
            Assert.assertTrue(nodeHealth.getIssues().get(0).equals("Error during healthcheck"));
        }
    }

    @Test
    public void testUnresponsiveSingleNodeThatThrowsRuntimeException() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStack());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), any())).thenThrow(new RuntimeException("Expected"));
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNREACHABLE, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 1);
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            Assert.assertTrue(!nodeHealth.getIssues().isEmpty());
            Assert.assertEquals(InstanceStatus.UNREACHABLE, nodeHealth.getStatus());
            Assert.assertTrue(nodeHealth.getIssues().size() == 1);
            Assert.assertTrue(nodeHealth.getIssues().get(0).equals("Expected"));
        }
    }

    @Test
    public void testUnresponsiveSecondaryNode() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im1))).thenReturn(getGoodDetails1());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im2))).thenThrow(ipaClientException);
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 2);
    }

    @Test
    public void testTwoGoodNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im1))).thenReturn(getGoodDetails1());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im2))).thenReturn(getGoodDetails2());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.AVAILABLE, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 2);
    }

    @Test
    public void testOneGoodOneUnhealthyNode() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im1))).thenReturn(getGoodDetails1());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im2))).thenReturn(getUnhealthyDetails2());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 2);
    }

    @Test
    public void testTwoUnhealthyNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im1))).thenReturn(getUnhealthyDetails1());
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im2))).thenReturn(getUnhealthyDetails2());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 2);
    }

    @Test
    public void testOneStoppedOneGoodNode() throws Exception {
        InstanceMetaData im1 = getInstance1();
        im1.setInstanceStatus(InstanceStatus.STOPPED);
        InstanceMetaData im2 = getInstance2();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), eq(im2))).thenReturn(getUnhealthyDetails2());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 2);
    }

    @Test
    public void testTwoStoppedNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        im1.setInstanceStatus(InstanceStatus.STOPPED);
        InstanceMetaData im2 = getInstance2();
        im2.setInstanceStatus(InstanceStatus.STOPPED);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.STOPPED, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 2);
    }

    @Test
    public void testTwoFailedNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        im1.setInstanceStatus(InstanceStatus.FAILED);
        InstanceMetaData im2 = getInstance2();
        im2.setInstanceStatus(InstanceStatus.FAILED);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 2);
    }

    @Test
    public void testTwoUnresponsiveNodes() throws Exception {
        InstanceMetaData im1 = getInstance1();
        InstanceMetaData im2 = getInstance2();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(getStackTwoInstances(im1, im2));
        Mockito.when(freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(any(), any())).thenThrow(ipaClientException);
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNREACHABLE, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 2);
    }

}
