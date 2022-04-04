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
import com.sequenceiq.freeipa.client.model.SudoRule;

@ExtendWith(MockitoExtension.class)
public class SudoRuleAddGroupOperationTest {

    private static final String NAME = "name";

    private static final String GROUP = "group";

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvoke() throws FreeIpaClientException {
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        rpcResponse.setResult(new SudoRule());

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        SudoRuleAddGroupOperation.create(NAME, GROUP).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("sudorule_add_user"),
                argThat(argument -> argument.contains(NAME) && argument.size() == 1),
                argThat(argument -> GROUP.equals(argument.get("group")) && argument.size() == 1),
                eq(SudoRule.class));
    }
}