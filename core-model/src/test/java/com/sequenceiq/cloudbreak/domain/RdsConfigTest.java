package com.sequenceiq.cloudbreak.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hibernate.annotations.Where;
import org.junit.jupiter.api.Test;

class RdsConfigTest {

    @Test
    void testHasWhereAnnotation() {
        Where whereAnnotation = RDSConfig.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }
}
