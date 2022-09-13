package com.sequenceiq.freeipa.service.freeipa.flow;

import static com.dyngr.core.AttemptState.CONTINUE;
import static com.dyngr.core.AttemptState.FINISH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dyngr.core.AttemptResult;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.Privilege;

class FreeIpaPermissionReplicatedPollerTest {

    private static final String PERMISSION = "PERM";

    private static final String PRIVILEGE = "PRIV";

    private FreeIpaClient client1 = mock(FreeIpaClient.class);

    private FreeIpaClient client2 = mock(FreeIpaClient.class);

    private FreeIpaPermissionReplicatedPoller underTest;

    @BeforeEach
    public void init() {
        underTest = new FreeIpaPermissionReplicatedPoller(List.of(client1, client2), PRIVILEGE, PERMISSION);
    }

    @Test
    public void testReplicationIsMissingForAnInstance() throws Exception {
        Privilege privilege = new Privilege();
        privilege.setMemberofPermission(List.of(PERMISSION));
        when(client1.showPrivilege(PRIVILEGE)).thenReturn(privilege);
        when(client2.showPrivilege(PRIVILEGE)).thenReturn(new Privilege());

        AttemptResult<Void> result = underTest.process();

        assertEquals(CONTINUE, result.getState());
    }

    @Test
    public void testReplicationIsReady() throws Exception {
        Privilege privilege = new Privilege();
        privilege.setMemberofPermission(List.of(PERMISSION));
        when(client1.showPrivilege(PRIVILEGE)).thenReturn(privilege);
        when(client2.showPrivilege(PRIVILEGE)).thenReturn(privilege);

        AttemptResult<Void> result = underTest.process();

        assertEquals(FINISH, result.getState());
    }

}