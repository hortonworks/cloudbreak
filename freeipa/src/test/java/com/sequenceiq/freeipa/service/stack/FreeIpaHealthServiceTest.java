package com.sequenceiq.freeipa.service.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
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

    private static RPCResponse<Boolean> goodResponse;

    private static RPCResponse<Boolean> doubleResponseOneBad;

    private static RPCResponse<Boolean> errorResponse;

    private static Stack stack;

    private static FreeIpaClientException ipaClientException;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @InjectMocks
    private FreeIpaHealthDetailsService underTest;

    private List<RPCMessage> getBaseMessages() {
        List<RPCMessage> messages = new ArrayList<>();
        messages.add(newRPCMessage("Check connection from master to remote replica 'freeipa.host.com':"));
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

    private RPCResponse<Boolean> getGoodPayload() {
        if (goodResponse == null) {
            goodResponse = new RPCResponse<>();
            goodResponse.setResult(Boolean.TRUE);
            goodResponse.setValue("test.host.name");
            goodResponse.setMessages(getBaseMessages());
        }
        return goodResponse;
    }

    private RPCResponse<Boolean> getErrorPayload() {
        if (errorResponse == null) {
            errorResponse = new RPCResponse<>();
            errorResponse.setResult(Boolean.TRUE);
            errorResponse.setValue("test.host.name");
            errorResponse.setMessages(getBaseMessages());
            errorResponse.getMessages().add(newRPCMessage("Failed to connect to port 464 tcp on 10.1.1.1"));
            errorResponse.getMessages().add(newRPCMessage("   Kerberos Kpasswd: TCP (464): FAILED"));
            errorResponse.getMessages().add(newRPCMessage("Failed to connect to port 636 tcp on 10.1.1.1"));
            errorResponse.getMessages().add(newRPCMessage("   Directory Service: Secure port (636): OK"));
        }
        return errorResponse;
    }

    private RPCResponse<Boolean> getDoublePayload() {
        if (doubleResponseOneBad == null) {
            doubleResponseOneBad = new RPCResponse<>();
            doubleResponseOneBad.setResult(Boolean.TRUE);
            doubleResponseOneBad.setValue("test.host.name");
            doubleResponseOneBad.setMessages(getBaseMessages());
            doubleResponseOneBad.getMessages().add(newRPCMessage("Check connection from master to remote replica 'freeipa2.host.com':"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("   Directory Service: Unsecure port (389): OK"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("   Directory Service: Secure port (636): OK"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("   Directory Service: Secure port (636): OK"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("   Kerberos KDC: TCP (88): OK"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("Failed to connect to port 88 udp on 10.1.1.1"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("   Kerberos KDC: UDP (88): WARNING"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("Failed to connect to port 464 tcp on 10.1.1.1"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("   Kerberos Kpasswd: TCP (464): FAILED"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("Failed to connect to port 464 udp on 10.1.1.1"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("   Kerberos Kpasswd: UDP (464): WARNING"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("   HTTP Server: Unsecure port (80): OK"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("   HTTP Server: Secure port (443): OK"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("The following UDP ports could not be verified as open: 88, 464"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("This can happen if they are already bound to an application"));
            doubleResponseOneBad.getMessages().add(newRPCMessage("and ipa-replica-conncheck cannot attach own UDP responder."));
            doubleResponseOneBad.getMessages().add(newRPCMessage("Connection from master to replica is OK."));
        }
        return doubleResponseOneBad;
    }

    private RPCMessage newRPCMessage(String message) {
        RPCMessage msg = new RPCMessage();
        msg.setMessage(message);
        msg.setName("ExternalCommandOutput");
        return msg;
    }

    @BeforeAll
    public static void init() {
        stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("localhost");
        instanceMetaData.setInstanceId("i-0123456789");
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        instanceMetaData.setDiscoveryFQDN("host.domain");
        ipaClientException = new FreeIpaClientException("failure");
    }

    @Test
    public void testHealthySingleNode() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), anyString())).thenReturn(getGoodPayload());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.AVAILABLE, response.getStatus());
        Assert.assertFalse(response.getNodeHealthDetails().isEmpty());
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            Assert.assertTrue(nodeHealth.getIssues().isEmpty());
            Assert.assertEquals(Status.AVAILABLE, nodeHealth.getStatus());
        }
    }

    @Test
    public void testUnhealthySingleNode() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), anyString())).thenReturn(getErrorPayload());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNHEALTHY, response.getStatus());
        Assert.assertFalse(response.getNodeHealthDetails().isEmpty());
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            Assert.assertFalse(nodeHealth.getIssues().isEmpty());
            Assert.assertEquals(Status.UNHEALTHY, nodeHealth.getStatus());
        }
    }

    @Test
    public void testUnresponsiveSingleNode() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), anyString())).thenThrow(ipaClientException);
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.UNREACHABLE, response.getStatus());
        Assert.assertFalse(response.getNodeHealthDetails().isEmpty());
        for (NodeHealthDetails nodeHealth:response.getNodeHealthDetails()) {
            Assert.assertTrue(nodeHealth.getIssues().isEmpty());
            Assert.assertEquals(Status.UNREACHABLE, nodeHealth.getStatus());
        }
    }

    @Test
    public void testUnresponsiveSecondaryNode() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), anyString())).thenReturn(getDoublePayload());
        HealthDetailsFreeIpaResponse response = underTest.getHealthDetails(ENVIRONMENT_ID, ACCOUNT_ID);
        Assert.assertEquals(Status.AVAILABLE, response.getStatus());
        Assert.assertFalse(response.getNodeHealthDetails().isEmpty());
    }

}
