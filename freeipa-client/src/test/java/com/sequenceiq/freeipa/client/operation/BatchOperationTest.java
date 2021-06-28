package com.sequenceiq.freeipa.client.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.cloudbreak.client.RPCResponse;

@ExtendWith(MockitoExtension.class)
public class BatchOperationTest {

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvokeWhenOperationsEmpty() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();
        BatchOperation.create(Lists.newArrayList(), warnings::put, Set.of()).invoke(freeIpaClient);
        verifyNoInteractions(freeIpaClient);
    }

    @Test
    public void testInvoke() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();
        List<Object> operations = Lists.newArrayList();
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new Object());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        operations.add(UserAddOperation.create("user", "first", "last", false).getOperationParamsForBatchCall());
        BatchOperation.create(operations, warnings::put, Set.of()).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("batch"), anyList(), any(), any());
    }

    @Test
    public void testInvokeIfAcceptableErrorOccurs() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();
        List<Object> operations = Lists.newArrayList();

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(4002, "", null)));

        operations.add(UserAddOperation.create("user", "first", "last", false).getOperationParamsForBatchCall());
        BatchOperation.create(operations, warnings::put, Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("batch"), anyList(), any(), any());
        assertEquals(0, warnings.size());
    }

    @Test
    public void testInvokeIfOtherErrorOccurs() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();
        List<Object> operations = Lists.newArrayList();

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(5000, "", null)));

        operations.add(UserAddOperation.create("user", "first", "last", false).getOperationParamsForBatchCall());
        BatchOperation.create(operations, warnings::put, Set.of(FreeIpaErrorCodes.NOT_FOUND)).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("batch"), anyList(), any(), any());
        assertEquals(1, warnings.size());
    }

}
