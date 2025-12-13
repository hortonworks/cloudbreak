package com.sequenceiq.redbeams.domain.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.Json;

class NetworkTest {

    private static final Json ATTRIBUTES = new Json("{}");

    private Network network;

    @BeforeEach
    public void setUp() throws Exception {
        network = new Network();
    }

    @Test
    void testGettersAndSetters() {
        network.setId(1L);
        assertEquals(1L, network.getId().longValue());

        network.setName("mynetwork");
        assertEquals("mynetwork", network.getName());

        network.setDescription("mine not yours");
        assertEquals("mine not yours", network.getDescription());

        network.setAttributes(ATTRIBUTES);
        assertEquals(ATTRIBUTES, network.getAttributes());
    }

}
