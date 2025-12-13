package com.sequenceiq.cloudbreak.orchestrator.yarn.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Artifact;

class ArtifactTest {

    private static final String TYPE = "DOCKER";

    private static final String ID = "company/foo:test";

    @Test
    void testType() {
        Artifact artifact = new Artifact();
        artifact.setType(TYPE);
        assertEquals(TYPE, artifact.getType());
    }

    @Test
    void testId() {
        Artifact artifact = new Artifact();
        artifact.setId(ID);
        assertEquals(ID, artifact.getId());
    }

}