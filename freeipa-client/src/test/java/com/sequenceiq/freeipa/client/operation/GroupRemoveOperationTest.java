package com.sequenceiq.freeipa.client.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Maps;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.freeipa.client.model.RPCResponse;

@ExtendWith(MockitoExtension.class)
public class GroupRemoveOperationTest {

    private static final String GROUP_NAME = "group";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvokeWhenGroupProtected() {
        Map warnings = Maps.newHashMap();
        assertThrows(FreeIpaClientException.class, () ->
                GroupRemoveOperation.create("admins", warnings::put).invoke(freeIpaClient));
        verifyNoInteractions(freeIpaClient);
    }

    @Test
    public void testInvoke() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new Group());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        GroupRemoveOperation.create(GROUP_NAME, warnings::put).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("group_del"), anyList(), any(), any());
    }

    @Test
    public void testInvokeIfNotFoundErrorOccurs() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(4001, "", null)));

        GroupRemoveOperation.create(GROUP_NAME, warnings::put).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("group_del"), anyList(), any(), any());
        assertEquals(0, warnings.size());
    }

    @Test
    public void testInvokeIfOtherErrorOccurs() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(5000, "", null)));

        GroupRemoveOperation.create(GROUP_NAME, warnings::put).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("group_del"), anyList(), any(), any());
        assertEquals(1, warnings.size());
    }

}
