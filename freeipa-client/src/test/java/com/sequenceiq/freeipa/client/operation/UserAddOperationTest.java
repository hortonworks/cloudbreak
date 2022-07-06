package com.sequenceiq.freeipa.client.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.client.model.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UserAddOperationTest {

    private static final String USER_NAME = "user";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvokeWhenUserProtected() {
        assertThrows(FreeIpaClientException.class, () ->
                UserAddOperation.create("admin", "admin", "admin", false, Optional.empty())
                        .invoke(freeIpaClient));
        verifyNoInteractions(freeIpaClient);
    }

    @Test
    public void testInvoke() throws FreeIpaClientException {
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new User());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        UserAddOperation.create(USER_NAME, USER_NAME, USER_NAME, false, Optional.empty()).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("user_add"), eq(List.of(USER_NAME)), any(), any());
    }

    @Test
    public void testInvokeWithTitle() throws FreeIpaClientException {
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new User());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);
        String titleString = "title string";

        UserAddOperation.create(USER_NAME, USER_NAME, USER_NAME, false, Optional.of(titleString))
                .invoke(freeIpaClient);

        ArgumentCaptor<Map<String, Object>> paramCaptor = ArgumentCaptor.forClass(Map.class);
        verify(freeIpaClient).invoke(eq("user_add"), eq(List.of(USER_NAME)), paramCaptor.capture(), any());
        Map<String, Object> params = paramCaptor.getValue();
        assertEquals(titleString, params.get("title"));
    }
}
