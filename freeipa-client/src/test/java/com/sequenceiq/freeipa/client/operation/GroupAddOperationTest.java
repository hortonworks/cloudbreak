package com.sequenceiq.freeipa.client.operation;

import static com.sequenceiq.freeipa.client.FreeIpaGroupType.NONPOSIX;
import static com.sequenceiq.freeipa.client.FreeIpaGroupType.POSIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Maps;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.cloudbreak.client.RPCResponse;

@ExtendWith(MockitoExtension.class)
public class GroupAddOperationTest {

    private static final String GROUP_NAME = "group";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvokeWhenGroupProtected() {
        Map warnings = Maps.newHashMap();
        assertThrows(FreeIpaClientException.class, () ->
                GroupAddOperation.create("admins", POSIX, warnings::put).invoke(freeIpaClient));
        verifyNoInteractions(freeIpaClient);
    }

    @Test
    public void testInvoke() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new Group());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        GroupAddOperation.create(GROUP_NAME, POSIX, warnings::put).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("group_add"), anyList(), any(), any());
    }

    @Test
    public void testInvokeIfDuplicateErrorOccurs() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(4002, "", null)));

        GroupAddOperation.create(GROUP_NAME, POSIX, warnings::put).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("group_add"), anyList(), any(), any());
        assertEquals(0, warnings.size());
    }

    @Test
    public void testInvokeIfOtherErrorOccurs() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(5000, "", null)));

        GroupAddOperation.create(GROUP_NAME, POSIX, warnings::put).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("group_add"), anyList(), any(), any());
        assertEquals(1, warnings.size());
    }

    @Test
    public void testInvokePosix() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new Group());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        GroupAddOperation.create(GROUP_NAME, POSIX, warnings::put).invoke(freeIpaClient);

        ArgumentCaptor<Map<String, Object>> paramCaptor = ArgumentCaptor.forClass(Map.class);
        verify(freeIpaClient).invoke(eq("group_add"), anyList(), paramCaptor.capture(), any());
        Map<String, Object> params = paramCaptor.getValue();
        assertTrue(!params.containsKey("nonposix"));
    }

    @Test
    public void testInvokeNonPosix() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new Group());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        GroupAddOperation.create(GROUP_NAME, NONPOSIX, warnings::put).invoke(freeIpaClient);

        ArgumentCaptor<Map<String, Object>> paramCaptor = ArgumentCaptor.forClass(Map.class);
        verify(freeIpaClient).invoke(eq("group_add"), anyList(), paramCaptor.capture(), any());
        Map<String, Object> params = paramCaptor.getValue();
        assertTrue(params.containsKey("nonposix"));
        assertTrue((Boolean) params.get("nonposix"));
    }
}
