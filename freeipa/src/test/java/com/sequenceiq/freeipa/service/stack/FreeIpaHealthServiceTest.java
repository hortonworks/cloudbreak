package com.sequenceiq.freeipa.service.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.ArrayList;
import java.util.List;

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
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCMessage;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@ExtendWith(MockitoExtension.class)
public class FreeIpaHealthServiceTest {
    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String ACCOUNT_ID = "accountId";

    private static FreeIpaClientException ipaClientException;

    private static final String HOST1 = "host1.domain";

    private static final String HOST2 = "host2.domain";

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @InjectMocks
    private FreeIpaHealthDetailsService underTest;

    private List<RPCMessage> getBaseMessages(String host) {
        List<RPCMessage> messages = new ArrayList<>();
        messages.add(newRPCMessage("Check connection from master to remote replica '" + host + "':"));
        messages.add(newRPCMessage("   Directory Service: Unsecure port (389): OK"));
        messages.add(newRPCMessage("   Directory Service: Secure port (636): OK"));
        messages.add(newRPCMessage("   Directory Service: Secure port (636): OK"));
        messages.add(newRPCMessage("   Kerberos KDC: TCP (88): OK"));
        messages.add(newRPCMessage("Failed to connect to port 88 udp on 10.1.1.1"));
        messages.add(newRPCMessage("   Kerberos KDC: UDP (88): WARNING"));
        messages.add(newRPCMessage("   Kerberos Kpasswd: TCP (464): OK"));
        messages.add(newRPCMessage("Failed to connect to port 464 udp on 10.1.1.1"));
        messages.add(newRPCMessage("   Kerberos Kpasswd: UDP (464): WARNING"));
        messages.add(newRPCMessage("   HTTP Server: Unsecure port (80): OK"));
        messages.add(newRPCMessage("   HTTP Server: Secure port (443): OK"));
        messages.add(newRPCMessage("The following UDP ports could not be verified as open: 88, 464"));
        messages.add(newRPCMessage("This can happen if they are already bound to an application"));
        messages.add(newRPCMessage("and ipa-replica-conncheck cannot attach own UDP responder."));
        messages.add(newRPCMessage("Connection from master to replica is OK."));
        return messages;
    }

    private RPCResponse<Boolean> getGoodPayload(String host) {
        RPCResponse<Boolean> goodResponse;
        goodResponse = new RPCResponse<>();
        goodResponse.setResult(Boolean.TRUE);
        goodResponse.setValue(host);
        goodResponse.setMessages(getBaseMessages(host));
        return goodResponse;
    }

    private RPCResponse<Boolean> getErrorPayload(String host) {
        RPCResponse<Boolean> errorResponse;
        errorResponse = new RPCResponse<>();
        errorResponse.setResult(Boolean.TRUE);
        errorResponse.setValue(host);
        errorResponse.setMessages(getBaseMessages(host));
        errorResponse.getMessages().add(newRPCMessage("Failed to connect to port 464 tcp on 10.1.1.1"));
        errorResponse.getMessages().add(newRPCMessage("   Kerberos Kpasswd: TCP (464): FAILED"));
        errorResponse.getMessages().add(newRPCMessage("Failed to connect to port 636 tcp on 10.1.1.1"));
        errorResponse.getMessages().add(newRPCMessage("   Directory Service: Secure port (636): OK"));
        return errorResponse;
    }

    private RPCMessage newRPCMessage(String message) {
        RPCMessage msg = new RPCMessage();
        msg.setMessage(message);
        msg.setName("ExternalCommandOutput");
        return msg;
    }

    private Stack getGoodStack() {
        Stack stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("i-0123456789");
        instanceMetaData.setDiscoveryFQDN(HOST1);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        return stack;
    }

    private Stack getGoodStackTwoInstances() {
        Stack stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("i-0123456789");
        instanceMetaData.setDiscoveryFQDN(HOST1);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));

        instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("i-9876543219");
        instanceMetaData.setDiscoveryFQDN(HOST2);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceGroup.getInstanceMetaData().add(instanceMetaData);
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
        instanceMetaData.setInstanceId("i-0123456789");
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        instanceMetaData.setDiscoveryFQDN(HOST1);
        return stack;
    }

    private FreeIpaClient getMockFreeIpaClient() {
        return new FreeIpaClient(null, "1.1.1.1", "testhost");
    }

    @BeforeAll
    public static void init() {
        ipaClientException = new FreeIpaClientException("failure");
    }

    @Test
    public void testHealthySingleNode() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(mockIpaClient.getHostname()).thenReturn("test.host");
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(getGoodStack());
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any(), any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), anyString())).thenReturn(getGoodPayload(HOST1));
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
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(mockIpaClient.getHostname()).thenReturn("test.host");
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(getGoodStack());
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any(), any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), anyString())).thenReturn(getErrorPayload(HOST1));
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertFalse(response.getNodeHealthDetails().isEmpty());
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            Assert.assertFalse(nodeHealth.getIssues().isEmpty());
            Assert.assertEquals(InstanceStatus.FAILED, nodeHealth.getStatus());
        }
    }

    @Test
    public void testUnresponsiveSingleNode() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(mockIpaClient.getHostname()).thenReturn("test.host");
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(getGoodStack());
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any(), any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), anyString())).thenThrow(ipaClientException);
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 1);
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            Assert.assertTrue(!nodeHealth.getIssues().isEmpty());
            Assert.assertEquals(InstanceStatus.UNREACHABLE, nodeHealth.getStatus());
            Assert.assertTrue(nodeHealth.getIssues().size() == 1);
            Assert.assertTrue(nodeHealth.getIssues().get(0).equals("failure"));
        }
    }

    @Test
    public void testUnresponsiveSecondaryNode() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(mockIpaClient.getHostname()).thenReturn("test.host");
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(getGoodStackTwoInstances());
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any(), any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), eq(HOST1))).thenReturn(getGoodPayload(HOST1));
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), eq(HOST2))).thenThrow(ipaClientException);
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.AVAILABLE, response.getStatus());
        Assert.assertTrue(response.getNodeHealthDetails().size() == 2);
    }

    @Test
    public void testNodeDeletedOnProvider() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(getDeletedStack());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertFalse(response.getNodeHealthDetails().isEmpty());
        Assert.assertTrue(response.getNodeHealthDetails().stream().findFirst().get().getStatus() == InstanceStatus.TERMINATED);
    }
}
