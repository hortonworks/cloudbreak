package com.sequenceiq.cloudbreak.domain.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hibernate.annotations.Where;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.domain.Network;

class NetworkTest {

    @Test
    void testHasWhereAnnotation() {
        Where whereAnnotation = Network.class.getAnnotation(Where.class);

        assertNotNull(whereAnnotation);
        assertEquals("archived = false", whereAnnotation.clause());
    }
}
