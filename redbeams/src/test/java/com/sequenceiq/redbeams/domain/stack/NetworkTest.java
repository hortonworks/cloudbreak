package com.sequenceiq.redbeams.domain.stack;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.common.json.Json;

public class NetworkTest {

    private static final Json ATTRIBUTES = new Json("{}");

    private Network network;

    @Before
    public void setUp() throws Exception {
        network = new Network();
    }

    @Test
    public void testGettersAndSetters() {
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
