package com.sequenceiq.freeipa.client.operation;

import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StageUserActivateOperationTest {

    private static final String USER_NAME = "user";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvokeWhenUserProtected() {
        assertThrows(FreeIpaClientException.class, () ->
                StageUserActivateOperation.create("admin").invoke(freeIpaClient));
        verifyNoInteractions(freeIpaClient);
    }

    @Test
    public void testInvoke() throws FreeIpaClientException {
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new User());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        StageUserActivateOperation.create(USER_NAME).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("stageuser_activate"), anyList(), any(), any());
    }
}
