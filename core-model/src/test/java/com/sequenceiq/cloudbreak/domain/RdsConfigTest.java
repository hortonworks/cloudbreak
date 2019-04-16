package com.sequenceiq.cloudbreak.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.annotations.Where;
import org.junit.Test;

public class RdsConfigTest {

    @Test
    public void testHasWhereAnnotation() {
        Where whereAnnotation = RDSConfig.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }
}
