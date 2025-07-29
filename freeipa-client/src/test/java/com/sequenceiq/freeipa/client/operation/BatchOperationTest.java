package com.sequenceiq.freeipa.client.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.model.User;

@ExtendWith(MockitoExtension.class)
public class BatchOperationTest {

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvokeWhenOperationsEmpty() throws FreeIpaClientException {
        Map<String, String> warnings = Maps.newHashMap();
        BatchOperation.create(Lists.newArrayList(), warnings::put, Set.of()).invoke(freeIpaClient);
        verifyNoInteractions(freeIpaClient);
    }

    @Test
    public void testInvoke() throws FreeIpaClientException {
        Map<String, String> warnings = Maps.newHashMap();
        List<Object> operations = Lists.newArrayList();
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new Object());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        operations.add(UserAddOperation.create("user", "first", "last", false, Optional.empty())
                .getOperationParamsForBatchCall());
        BatchOperation.create(operations, warnings::put, Set.of()).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("batch"), anyList(), any(), any());
    }

    @Test
    public void testInvokeIfAcceptableErrorOccurs() throws FreeIpaClientException {
        Map<String, String> warnings = Maps.newHashMap();
        List<Object> operations = Lists.newArrayList();

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(4002, "", null)));

        operations.add(UserAddOperation.create("user", "first", "last", false, Optional.empty())
                .getOperationParamsForBatchCall());
        BatchOperation.create(operations, warnings::put, Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("batch"), anyList(), any(), any());
        assertEquals(0, warnings.size());
    }

    @Test
    public void testInvokeIfOtherErrorOccurs() throws FreeIpaClientException {
        Map<String, String> warnings = Maps.newHashMap();
        List<Object> operations = Lists.newArrayList();

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(5000, "", null)));

        operations.add(UserAddOperation.create("user", "first", "last", false, Optional.empty())
                .getOperationParamsForBatchCall());
        BatchOperation.create(operations, warnings::put, Set.of(FreeIpaErrorCodes.NOT_FOUND)).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("batch"), anyList(), any(), any());
        assertEquals(1, warnings.size());
    }

    @Test
    public void testRpcInvokeWhenOperationsEmpty() throws FreeIpaClientException {
        Map<String, String> warnings = Maps.newHashMap();
        RPCResponse<User> result = BatchOperation.<User>create(Lists.newArrayList(), warnings::put, Set.of())
                .rpcInvoke(freeIpaClient, User.class);

        assertNull(result);
        verifyNoInteractions(freeIpaClient);
    }

    @Test
    public void testRpcInvoke() throws FreeIpaClientException {
        Map<String, String> warnings = Maps.newHashMap();
        List<Object> operations = Lists.newArrayList();
        RPCResponse<User> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new User());

        when(freeIpaClient.invoke(any(), anyList(), any(), any(Type.class))).thenReturn((RPCResponse) rpcResponse);

        operations.add(UserAddOperation.create("user", "first", "last", false, Optional.empty())
                .getOperationParamsForBatchCall());
        RPCResponse<User> result = BatchOperation.<User>create(operations, warnings::put, Set.of())
                .rpcInvoke(freeIpaClient, User.class);

        assertEquals(rpcResponse, result);
        verify(freeIpaClient).invoke(eq("batch"), anyList(), any(), eq(User.class));
    }

    @Test
    public void testRpcInvokeIfAcceptableErrorOccurs() throws FreeIpaClientException {
        Map<String, String> warnings = Maps.newHashMap();
        List<Object> operations = Lists.newArrayList();

        when(freeIpaClient.invoke(any(), anyList(), any(), any(Type.class))).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(4002, "", null)));

        operations.add(UserAddOperation.create("user", "first", "last", false, Optional.empty())
                .getOperationParamsForBatchCall());
        RPCResponse<User> result = BatchOperation.<User>create(operations, warnings::put, Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY))
                .rpcInvoke(freeIpaClient, User.class);

        assertNull(result);
        verify(freeIpaClient).invoke(eq("batch"), anyList(), any(), eq(User.class));
        assertEquals(0, warnings.size());
    }

    @Test
    public void testRpcInvokeIfOtherErrorOccurs() throws FreeIpaClientException {
        Map<String, String> warnings = Maps.newHashMap();
        List<Object> operations = Lists.newArrayList();

        when(freeIpaClient.invoke(any(), anyList(), any(), any(Type.class))).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(5000, "", null)));

        operations.add(UserAddOperation.create("user", "first", "last", false, Optional.empty())
                .getOperationParamsForBatchCall());
        RPCResponse<User> result = BatchOperation.<User>create(operations, warnings::put, Set.of(FreeIpaErrorCodes.NOT_FOUND))
                .rpcInvoke(freeIpaClient, User.class);

        assertNull(result);
        verify(freeIpaClient).invoke(eq("batch"), anyList(), any(), eq(User.class));
        verify(freeIpaClient).checkIfClientStillUsable(any(FreeIpaClientException.class));
        assertEquals(1, warnings.size());
    }

}
