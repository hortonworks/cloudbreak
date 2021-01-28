package com.sequenceiq.freeipa.client.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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
import com.sequenceiq.cloudbreak.client.RPCResponse;

@ExtendWith(MockitoExtension.class)
public class GroupRemoveMemberOperationTest {

    private static final String GROUP_NAME = "group";

    private static final List<String> USERS = List.of("user1", "user2");

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    public void testInvokeWhenGroupNotManaged() {
        Map warnings = Maps.newHashMap();
        assertThrows(FreeIpaClientException.class, () ->
                GroupRemoveMemberOperation.create("editors", USERS, warnings::put).invoke(freeIpaClient));
        verifyNoInteractions(freeIpaClient);
    }

    @Test
    public void testInvoke() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        Group group = new Group();
        group.setMemberUser(List.of());
        rpcResponse.setResult(group);

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        GroupRemoveMemberOperation.create(GROUP_NAME, USERS, warnings::put).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("group_remove_member"), anyList(), any(), any());
        assertEquals(0, warnings.size());
    }

    @Test
    public void testInvokeWhenResultStillHasUser() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();
        RPCResponse<Object> rpcResponse = new RPCResponse<>();
        Group group = new Group();
        group.setMemberUser(List.of("user1"));
        rpcResponse.setResult(group);

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenReturn(rpcResponse);

        GroupRemoveMemberOperation.create(GROUP_NAME, USERS, warnings::put).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("group_remove_member"), anyList(), any(), any());
        assertEquals(1, warnings.size());
    }

    @Test
    public void testInvokeIfErrorOccurs() throws FreeIpaClientException {
        Map warnings = Maps.newHashMap();

        when(freeIpaClient.invoke(any(), anyList(), any(), any())).thenThrow(
                new FreeIpaClientException("error", new JsonRpcClientException(5000, "", null)));

        GroupRemoveMemberOperation.create(GROUP_NAME, USERS, warnings::put).invoke(freeIpaClient);

        verify(freeIpaClient).invoke(eq("group_remove_member"), anyList(), any(), any());
        assertEquals(1, warnings.size());
    }

}
