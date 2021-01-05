package com.sequenceiq.freeipa.service.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
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

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaHealthCheckClient;
import com.sequenceiq.freeipa.client.FreeIpaHealthCheckClientFactory;
import com.sequenceiq.freeipa.client.healthcheckmodel.CheckResult;
import com.sequenceiq.freeipa.client.model.RPCMessage;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.util.HealthCheckAvailabilityChecker;

@ExtendWith(MockitoExtension.class)
public class FreeIpaInstanceHealthDetailsServiceTest {
    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static FreeIpaClientException ipaClientException;

    private static final String HOST = "host1.domain";

    private static final String INSTANCE_ID = "i-0123456789";

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private HealthCheckAvailabilityChecker healthCheckAvailabilityChecker;

    @Mock
    private FreeIpaHealthCheckClientFactory freeIpaHealthCheckClientFactory;

    @InjectMocks
    private FreeIpaInstanceHealthDetailsService underTest;

    private List<RPCMessage> getLegacyBaseMessages(String host) {
        List<RPCMessage> messages = new ArrayList<>();
        messages.add(newLegacyRPCMessage("Check connection from master to remote replica '" + host + "':"));
        messages.add(newLegacyRPCMessage("   Directory Service: Unsecure port (389): OK"));
        messages.add(newLegacyRPCMessage("   Directory Service: Secure port (636): OK"));
        messages.add(newLegacyRPCMessage("   Directory Service: Secure port (636): OK"));
        messages.add(newLegacyRPCMessage("   Kerberos KDC: TCP (88): OK"));
        messages.add(newLegacyRPCMessage("Failed to connect to port 88 udp on 10.1.1.1"));
        messages.add(newLegacyRPCMessage("   Kerberos KDC: UDP (88): WARNING"));
        messages.add(newLegacyRPCMessage("   Kerberos Kpasswd: TCP (464): OK"));
        messages.add(newLegacyRPCMessage("Failed to connect to port 464 udp on 10.1.1.1"));
        messages.add(newLegacyRPCMessage("   Kerberos Kpasswd: UDP (464): WARNING"));
        messages.add(newLegacyRPCMessage("   HTTP Server: Unsecure port (80): OK"));
        messages.add(newLegacyRPCMessage("   HTTP Server: Secure port (443): OK"));
        messages.add(newLegacyRPCMessage("The following UDP ports could not be verified as open: 88, 464"));
        messages.add(newLegacyRPCMessage("This can happen if they are already bound to an application"));
        messages.add(newLegacyRPCMessage("and ipa-replica-conncheck cannot attach own UDP responder."));
        messages.add(newLegacyRPCMessage("Connection from master to replica is OK."));
        return messages;
    }

    private RPCResponse<Boolean> getLegacyGoodPayload(String host) {
        RPCResponse<Boolean> goodResponse;
        goodResponse = new RPCResponse<>();
        goodResponse.setResult(Boolean.TRUE);
        goodResponse.setValue(host);
        goodResponse.setMessages(getLegacyBaseMessages(host));
        return goodResponse;
    }

    private RPCResponse<Boolean> getLegacyErrorPayload(String host) {
        RPCResponse<Boolean> errorResponse;
        errorResponse = new RPCResponse<>();
        errorResponse.setResult(Boolean.TRUE);
        errorResponse.setValue(host);
        errorResponse.setMessages(getLegacyBaseMessages(host));
        errorResponse.getMessages().add(newLegacyRPCMessage("Failed to connect to port 464 tcp on 10.1.1.1"));
        errorResponse.getMessages().add(newLegacyRPCMessage("   Kerberos Kpasswd: TCP (464): FAILED"));
        errorResponse.getMessages().add(newLegacyRPCMessage("Failed to connect to port 636 tcp on 10.1.1.1"));
        errorResponse.getMessages().add(newLegacyRPCMessage("   Directory Service: Secure port (636): OK"));
        return errorResponse;
    }

    private RPCMessage newLegacyRPCMessage(String message) {
        RPCMessage msg = new RPCMessage();
        msg.setMessage(message);
        msg.setName("ExternalCommandOutput");
        return msg;
    }

    private RPCResponse<CheckResult> getGoodPayload(String host) {
        CheckResult checkResult = new CheckResult();
        checkResult.setHost(host);
        RPCResponse<CheckResult> goodResponse;
        goodResponse = new RPCResponse<>();
        goodResponse.setResult(checkResult);
        RPCMessage message = new RPCMessage();
        message.setCode(200);
        message.setMessage("success");
        goodResponse.setMessages(List.of(message));
        return goodResponse;
    }

    private RPCResponse<CheckResult> getErrorPayload(String host) {
        CheckResult checkResult = new CheckResult();
        checkResult.setHost(host);
        RPCResponse<CheckResult> badResponse;
        badResponse = new RPCResponse<>();
        badResponse.setResult(checkResult);
        RPCMessage message = new RPCMessage();
        message.setCode(503);
        message.setMessage("failure");
        badResponse.setMessages(List.of(message));
        return badResponse;
    }

    private InstanceMetaData getInstance() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID);
        instanceMetaData.setDiscoveryFQDN(HOST);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        return instanceMetaData;
    }

    private Stack getStack(Set<InstanceMetaData> instanceMetaData) {
        Stack stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        instanceGroup.setInstanceMetaData(instanceMetaData);
        return stack;
    }

    @BeforeAll
    public static void init() {
        ipaClientException = new FreeIpaClientException("failure");
    }

    @Test
    public void testGetInstanceHealthDetailsLegacyHealthyNode() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(any())).thenReturn(false);
        Mockito.when(mockIpaClient.getHostname()).thenReturn("test.host");
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStackForLegacyHealthCheck(any(), any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), anyString())).thenReturn(getLegacyGoodPayload(HOST));

        InstanceMetaData instanceMetaData = getInstance();
        Stack stack = getStack(Set.of(instanceMetaData));

        NodeHealthDetails response = underTest.getInstanceHealthDetails(stack, instanceMetaData);
        Assert.assertEquals(InstanceStatus.CREATED, response.getStatus());
        Assert.assertEquals(INSTANCE_ID, response.getInstanceId());
        Assert.assertEquals(HOST, response.getName());
        Assert.assertTrue(response.getIssues().isEmpty());
    }

    @Test
    public void testGetInstanceHealthDetailsLegacyUnhealthyNode() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(any())).thenReturn(false);
        Mockito.when(mockIpaClient.getHostname()).thenReturn(HOST);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStackForLegacyHealthCheck(any(), any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), anyString())).thenReturn(getLegacyErrorPayload(HOST));

        InstanceMetaData instanceMetaData = getInstance();
        Stack stack = getStack(Set.of(instanceMetaData));

        NodeHealthDetails response = underTest.getInstanceHealthDetails(stack, instanceMetaData);
        Assert.assertEquals(InstanceStatus.UNHEALTHY, response.getStatus());
        Assert.assertEquals(INSTANCE_ID, response.getInstanceId());
        Assert.assertEquals(HOST, response.getName());
        Assert.assertFalse(response.getIssues().isEmpty());
    }

    @Test
    public void testGetInstanceHealthDetailsLegacyUnresponsiveNodeThrows() throws Exception {
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        Mockito.when(healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(any())).thenReturn(false);
        Mockito.when(mockIpaClient.getHostname()).thenReturn(HOST);
        Mockito.when(freeIpaClientFactory.getFreeIpaClientForStackForLegacyHealthCheck(any(), any())).thenReturn(mockIpaClient);
        Mockito.when(mockIpaClient.serverConnCheck(anyString(), anyString())).thenThrow(ipaClientException);

        InstanceMetaData instanceMetaData = getInstance();
        Stack stack = getStack(Set.of(instanceMetaData));

        Assert.assertThrows(FreeIpaClientException.class, () -> underTest.getInstanceHealthDetails(stack, instanceMetaData));
    }

    @Test
    public void testGetInstanceHealthDetailsHealthyNode() throws Exception {
        FreeIpaHealthCheckClient mockIpaHealthClient = Mockito.mock(FreeIpaHealthCheckClient.class);
        Mockito.when(healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(any())).thenReturn(true);
        Mockito.when(freeIpaHealthCheckClientFactory.getClient(any(), any())).thenReturn(mockIpaHealthClient);
        Mockito.when(mockIpaHealthClient.nodeHealth()).thenReturn(getGoodPayload(HOST));

        InstanceMetaData instanceMetaData = getInstance();
        Stack stack = getStack(Set.of(instanceMetaData));

        NodeHealthDetails response = underTest.getInstanceHealthDetails(stack, instanceMetaData);
        Assert.assertEquals(InstanceStatus.CREATED, response.getStatus());
        Assert.assertEquals(INSTANCE_ID, response.getInstanceId());
        Assert.assertEquals(HOST, response.getName());
        Assert.assertTrue(response.getIssues().isEmpty());
    }

    @Test
    public void testGetInstanceHealthDetailsUnhealthyNode() throws Exception {
        FreeIpaHealthCheckClient mockIpaHealthClient = Mockito.mock(FreeIpaHealthCheckClient.class);
        Mockito.when(healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(any())).thenReturn(true);
        Mockito.when(freeIpaHealthCheckClientFactory.getClient(any(), any())).thenReturn(mockIpaHealthClient);
        Mockito.when(mockIpaHealthClient.nodeHealth()).thenReturn(getErrorPayload(HOST));

        InstanceMetaData instanceMetaData = getInstance();
        Stack stack = getStack(Set.of(instanceMetaData));

        NodeHealthDetails response = underTest.getInstanceHealthDetails(stack, instanceMetaData);
        Assert.assertEquals(InstanceStatus.UNHEALTHY, response.getStatus());
        Assert.assertEquals(INSTANCE_ID, response.getInstanceId());
        Assert.assertEquals(HOST, response.getName());
        Assert.assertFalse(response.getIssues().isEmpty());
    }

    @Test
    public void testGetInstanceHealthDetailsUnresponsiveNodeThrows() throws Exception {
        FreeIpaHealthCheckClient mockIpaHealthClient = Mockito.mock(FreeIpaHealthCheckClient.class);
        Mockito.when(healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(any())).thenReturn(true);
        Mockito.when(freeIpaHealthCheckClientFactory.getClient(any(), any())).thenReturn(mockIpaHealthClient);
        Mockito.when(mockIpaHealthClient.nodeHealth()).thenThrow(ipaClientException);

        InstanceMetaData instanceMetaData = getInstance();
        Stack stack = getStack(Set.of(instanceMetaData));

        Assert.assertThrows(FreeIpaClientException.class, () -> underTest.getInstanceHealthDetails(stack, instanceMetaData));
    }

}
