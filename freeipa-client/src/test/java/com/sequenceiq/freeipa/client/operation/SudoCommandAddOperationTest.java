package com.sequenceiq.freeipa.client.operation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.SudoCommand;

@ExtendWith(MockitoExtension.class)
public class SudoCommandAddOperationTest {

    private static final String NAME = "name";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvoke() throws FreeIpaClientException {
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new SudoCommand());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        SudoCommandAddOperation.create(NAME).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("sudocmd_add"),
                argThat(argument -> argument.contains(NAME) && argument.size() == 1),
                argThat(argument -> argument.isEmpty()),
                eq(SudoCommand.class));
    }
}