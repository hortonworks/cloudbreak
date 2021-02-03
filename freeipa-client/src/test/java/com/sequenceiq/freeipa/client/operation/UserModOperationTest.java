package com.sequenceiq.freeipa.client.operation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.client.model.User;

@ExtendWith(MockitoExtension.class)
public class UserModOperationTest {

    private static final String USER_NAME = "user";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvoke() throws FreeIpaClientException {
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new User());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        UserModOperation.create("key", new Object(), USER_NAME).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("user_mod"), anyList(), any(), any());
    }

    @Test
    public void testInvokeIfEmptyModListErrorOccurs() throws FreeIpaClientException {
        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(4202, "", null)));

        UserModOperation.create("key", new Object(), USER_NAME).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("user_mod"), anyList(), any(), any());
    }

    @Test
    public void testInvokeIfOtherErrorOccurs() throws FreeIpaClientException {
        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(5000, "", null)));

        assertThrows(FreeIpaClientException.class, () ->
                UserModOperation.create("key", new Object(), USER_NAME).invoke(freeIpaClient));

        verify(freeIpaClient).invoke(eq("user_mod"), anyList(), any(), any());
    }

}
