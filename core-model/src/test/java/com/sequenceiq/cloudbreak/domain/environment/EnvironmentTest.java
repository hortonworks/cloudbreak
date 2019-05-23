package com.sequenceiq.cloudbreak.domain.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.hibernate.annotations.Where;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public class EnvironmentTest {

    private static final String WORKSPACE_NAME = "workspaceName";

    @Test
    public void testHasWhereAnnotation() {
        Where whereAnnotation = Environment.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }

    @Test
    public void testUnsetRelationsToEntitiesToBeDeleted() {
        Workspace workspace = new Workspace();
        workspace.setName(WORKSPACE_NAME);
        Credential credential = new Credential();
        Environment underTest = new Environment();
        underTest.setWorkspace(workspace);
        underTest.setCredential(credential);
        underTest.setDatalakeResources(Set.of(new DatalakeResources()));

        underTest.unsetRelationsToEntitiesToBeDeleted();

        assertEquals(credential, underTest.getCredential());
        assertEquals(workspace, underTest.getWorkspace());
    }
}
