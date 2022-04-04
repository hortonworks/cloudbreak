package com.sequenceiq.freeipa.client.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.model.SudoRule;

@ExtendWith(MockitoExtension.class)
public class SudoRuleShowOperationTest {

    private static final String NAME = "name";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvoke() throws FreeIpaClientException {
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new SudoRule());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        SudoRuleShowOperation.create(NAME).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("sudorule_show"),
                argThat(argument -> argument.contains(NAME) && argument.size() == 1),
                argThat(argument -> argument.isEmpty()),
                eq(SudoRule.class));
    }

    @Test
    public void testInvokeShouldReturnEmptyInCaseOfNotFoundException() throws FreeIpaClientException {
        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(new FreeIpaClientException("",
                new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), null, null)));

        Optional<SudoRule> result = SudoRuleShowOperation.create(NAME).invoke(freeIpaClient);

        assertEquals(Optional.empty(), result);
        verify(freeIpaClient).invoke(eq("sudorule_show"),
                argThat(argument -> argument.contains(NAME) && argument.size() == 1),
                argThat(argument -> argument.isEmpty()),
                eq(SudoRule.class));
    }

    @Test
    public void testInvokeShouldFreeIpaClientException() throws FreeIpaClientException {
        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(new FreeIpaClientException(null));

        assertThrows(FreeIpaClientException.class, () -> SudoRuleShowOperation.create(NAME).invoke(freeIpaClient));
        verify(freeIpaClient).invoke(eq("sudorule_show"),
                argThat(argument -> argument.contains(NAME) && argument.size() == 1),
                argThat(argument -> argument.isEmpty()),
                eq(SudoRule.class));
    }
}