package com.sequenceiq.cloudbreak.orchestrator.yarn.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Artifact;

public class ArtifactTest {

    private static final String TYPE = "DOCKER";

    private static final String ID = "company/foo:test";

    @Test
    public void testType() throws Exception {
        Artifact artifact = new Artifact();
        artifact.setType(TYPE);
        assertEquals(TYPE, artifact.getType());
    }

    @Test
    public void testId() throws Exception {
        Artifact artifact = new Artifact();
        artifact.setId(ID);
        assertEquals(ID, artifact.getId());
    }

}