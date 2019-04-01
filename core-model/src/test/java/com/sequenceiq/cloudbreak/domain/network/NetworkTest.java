package com.sequenceiq.cloudbreak.domain.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.annotations.Where;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.Network;

public class NetworkTest {

    @Test
    public void testHasWhereAnnotation() {
        Where whereAnnotation = Network.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }
}
