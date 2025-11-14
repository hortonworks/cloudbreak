package com.sequenceiq.freeipa.service.stack;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.sync.FreeipaChecker;
import com.sequenceiq.freeipa.sync.SyncResult;

@ExtendWith(MockitoExtension.class)
public class FreeipaCheckerTest {

    private static final int SUCCESS_STATUS = 200;

    @InjectMocks
    private FreeipaChecker underTest;

    @Mock
    private FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    @BeforeEach
    public void setUp() {
        underTest = new FreeipaChecker();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetStatus() throws FreeIpaClientException {
        // GIVEN
        Stack stack = new Stack();
        Set<InstanceMetaData> instanceMetaDataSet = createInstanceMetaDataSet();
        given(freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(any(), any()))
                .willReturn(createHealthResponse());
        // WHEN
        underTest.getStatus(stack, instanceMetaDataSet, Set.of());
        // THEN
        verify(freeIpaInstanceHealthDetailsService, times(2)).checkFreeIpaHealth(any(), any());
    }

    @Test
    public void testGetStatusWhenSaltCheckFailed() throws FreeIpaClientException {
        // GIVEN
        Stack stack = new Stack();
        Set<InstanceMetaData> instanceMetaDataSet = createInstanceMetaDataSet();
        given(freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(any(), any()))
                .willReturn(createHealthResponse());
        // WHEN
        SyncResult status = underTest.getStatus(stack, instanceMetaDataSet, Set.of("host2"));
        // THEN
        verify(freeIpaInstanceHealthDetailsService, times(2)).checkFreeIpaHealth(any(), any());
        assertEquals(DetailedStackStatus.UNHEALTHY, status.getStatus());
        Map<InstanceMetaData, DetailedStackStatus> unhealthyNodes = status.getInstanceStatusMap().entrySet().stream().
                filter(entry -> entry.getValue().equals(DetailedStackStatus.UNHEALTHY))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(1, unhealthyNodes.size());
        assertEquals("host2", unhealthyNodes.keySet().iterator().next().getDiscoveryFQDN());
    }

    private RPCResponse<Boolean> createHealthResponse() {
        RPCResponse<Boolean> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(true);
        rpcResponse.setMessages(createRpcMessage());
        return rpcResponse;
    }

    private List<RPCMessage> createRpcMessage() {
        RPCMessage rpcMessage = new RPCMessage();
        rpcMessage.setMessage("{'empty': 'json'}");
        rpcMessage.setCode(SUCCESS_STATUS);
        return List.of(rpcMessage);
    }

    private Set<InstanceMetaData> createInstanceMetaDataSet() {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        InstanceMetaData instance1 = new InstanceMetaData();
        instance1.setInstanceId("id1");
        instance1.setDiscoveryFQDN("host1");
        InstanceMetaData instance2 = new InstanceMetaData();
        instance2.setInstanceId("id2");
        instance2.setDiscoveryFQDN("host2");
        instanceMetaDataSet.add(instance1);
        instanceMetaDataSet.add(instance2);
        return instanceMetaDataSet;
    }

}
