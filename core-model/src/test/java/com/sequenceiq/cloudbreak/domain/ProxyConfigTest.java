package com.sequenceiq.cloudbreak.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.annotations.Where;
import org.junit.Test;

public class ProxyConfigTest {
    @Test
    public void testHasWhereAnnotation() {
        Where whereAnnotation = ProxyConfig.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }
}
