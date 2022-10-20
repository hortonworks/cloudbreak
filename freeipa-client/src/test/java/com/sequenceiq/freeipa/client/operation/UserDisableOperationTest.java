package com.sequenceiq.freeipa.client.operation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;

@ExtendWith(MockitoExtension.class)
public class UserDisableOperationTest {

    private static final String USER_NAME = "user";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvokeWhenUserProtected() {
        assertThrows(FreeIpaClientException.class, () ->
                UserDisableOperation.create("admin").invoke(freeIpaClient));
        verifyNoInteractions(freeIpaClient);
    }

    @Test
    public void testInvoke() throws FreeIpaClientException {
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new Object());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        UserDisableOperation operation = UserDisableOperation.create(USER_NAME);
        operation.invoke(freeIpaClient);

        assertTrue(operation.getFlags().contains(USER_NAME));
        verify(freeIpaClient).invoke(eq("user_disable"), anyList(), any(), any());
    }
}
