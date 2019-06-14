package com.sequenceiq.redbeams.domain.stack;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class SecurityGroupTest {

    private static final Set SECURITY_GROUP_IDS = Set.of("id1", "id2", "id3");

    private SecurityGroup group;

    @Before
    public void setUp() throws Exception {
        group = new SecurityGroup();
    }

    @Test
    public void testGettersAndSetters() {
        group.setId(1L);
        assertEquals(1L, group.getId().longValue());

        group.setName("mygroup");
        assertEquals("mygroup", group.getName());

        group.setDescription("mine not yours");
        assertEquals("mine not yours", group.getDescription());

        group.setSecurityGroupIds(SECURITY_GROUP_IDS);
        assertEquals(SECURITY_GROUP_IDS, group.getSecurityGroupIds());
    }

}
