package com.sequenceiq.cloudbreak.structuredevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public class CloudbreakRestRequestThreadLocalServiceTest {

    private static final Long PREVIOUS_WORKSPACE_ID = 321L;

    private static final Long WORKSPACE_ID = 123L;

    private CloudbreakRestRequestThreadLocalService victim;

    @BeforeEach
    public void setUp() {
        victim = new CloudbreakRestRequestThreadLocalService();
    }

    @Test
    public void shouldOverwritePreviousWorkspaceIdWithTheCurrentOne() {
        victim.setRequestedWorkspaceId(PREVIOUS_WORKSPACE_ID);
        victim.setWorkspaceId(aWorkspace().getId());

        assertEquals(WORKSPACE_ID, victim.getRequestedWorkspaceId());
    }

    @Test
    public void shouldClearPreviousWorkspaceIdInCaseOfMissingWorkspace() {
        victim.setRequestedWorkspaceId(PREVIOUS_WORKSPACE_ID);
        victim.setWorkspaceId(null);

        assertNull(victim.getRequestedWorkspaceId());
    }

    private Workspace aWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);

        return workspace;
    }
}