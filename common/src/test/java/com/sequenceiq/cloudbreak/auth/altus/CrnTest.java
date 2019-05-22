package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CrnTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private String exampleCrn = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String exampleCrn2 = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bd@a7-be07183720d3";

    private String exampleCrn3 = "crn:altus:iam:us-west-1:default:user:lnardai@cloudera.com";

    private String invalidCrnPattern = "crn:something:chunked21dqw";

    private String invalidCrnEmptyParts = ":::::";

    private String invalidCrnPartition = "crn:cookie:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String invalidCrnRegion = "crn:altus:iam:eu-north-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String invalidCrnService = "crn:altus:cookie:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String invalidCrnResourceType = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:cookie:f3b8ed82-e712-4f89-bda7-be07183720d3";

    @Test
    public void testBuilder() {
        Crn crn = Crn.builder()
            .setService(Crn.Service.IAM)
            .setAccountId("accountId")
            .setResourceType(Crn.ResourceType.USER)
            .setResource("userId")
            .build();

        assertEquals(Crn.Service.IAM, crn.getService());
        assertEquals("accountId", crn.getAccountId());
        assertEquals(Crn.ResourceType.USER, crn.getResourceType());
        assertEquals("userId", crn.getResource());
    }

    @Test
    public void testFromString() {
        Crn crn = Crn.fromString(exampleCrn);
        assertEquals(Crn.Partition.ALTUS, crn.getPartition());
        assertEquals(Crn.Service.IAM, crn.getService());
        assertEquals(Crn.Region.US_WEST_1, crn.getRegion());
        assertEquals("9d74eee4-1cad-45d7-b645-7ccf9edbb73d", crn.getAccountId());
        assertEquals(Crn.ResourceType.USER, crn.getResourceType());
        assertEquals("f3b8ed82-e712-4f89-bda7-be07183720d3", crn.getResource());
    }

    @Test
    public void testFromStringWithInvalidPattern() {
        assertNull(Crn.fromString(invalidCrnPattern));
    }

    @Test
    public void testIsCrnInvalid() {
        assertFalse(Crn.isCrn(invalidCrnPattern));
        assertFalse(Crn.isCrn(invalidCrnEmptyParts));
    }

    @Test
    public void testIsCrnValid() {
        assertTrue(Crn.isCrn(exampleCrn));
        assertTrue(Crn.isCrn(exampleCrn2));
        assertTrue(Crn.isCrn(exampleCrn3));
    }

    @Test
    public void testInvalidCrnDueToPartition() {
        thrown.expect(CrnParseException.class);
        Crn.fromString(invalidCrnPartition);
    }

    @Test
    public void testInvalidCrnDueToRegion() {
        thrown.expect(CrnParseException.class);
        Crn.fromString(invalidCrnRegion);
    }

    @Test
    public void testInvalidCrnDueToService() {
        thrown.expect(CrnParseException.class);
        Crn.fromString(invalidCrnService);
    }

    @Test
    public void testInvalidCrnDueToResourceType() {
        thrown.expect(CrnParseException.class);
        Crn.fromString(invalidCrnResourceType);
    }
}
