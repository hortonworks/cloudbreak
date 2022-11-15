package com.sequenceiq.cloudbreak.auth.crn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CrnTest {

    private String exampleCrn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String exampleCrn2 = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bd@a7-be07183720d3";

    private String exampleCrn3 = "crn:cdp:iam:us-west-1:default:user:lnardai@cloudera.com";

    private String exampleCrn4 = "crn:cdp:iam:usg-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bd@a7-be07183720d3";

    private String invalidCrnPattern = "crn:something:chunked21dqw";

    private String invalidCrnEmptyParts = ":::::";

    private String invalidCrnPartition = "crn:cookie:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String invalidCrnRegion = "crn:cdp:iam:eu-north-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String invalidCrnService = "crn:cdp:cookie:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String invalidCrnResourceType = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:cookie:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String machineUserBaby = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:machineUser:qaas/b8a64902-7765-4ddd-a4f3-df81ae585e10";

    @Test
    public void testBuilder() {
        Crn crn = CrnTestUtil.getUserCrnBuilder().setAccountId("accountId").setResource("userId").build();

        assertEquals(Crn.Service.IAM, crn.getService());
        assertEquals("accountId", crn.getAccountId());
        assertEquals(Crn.ResourceType.USER, crn.getResourceType());
        assertEquals("userId", crn.getResource());
    }

    @Test
    public void testFromString() {
        Crn crn = Crn.fromString(exampleCrn);
        assertEquals(Crn.Partition.CDP, crn.getPartition());
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
        assertTrue(Crn.isCrn(exampleCrn4));
    }

    @Test
    public void testInvalidCrnDueToPartition() {
        CrnParseException exception = assertThrows(CrnParseException.class, () -> {
            Crn.fromString(invalidCrnPartition);
        });

        assertEquals("cookie is not a valid partition value", exception.getMessage());
    }

    @Test
    public void testInvalidCrnDueToRegion() {
        CrnParseException exception = assertThrows(CrnParseException.class, () -> {
            Crn.fromString(invalidCrnRegion);
        });

        assertEquals("eu-north-1 is not a valid region value", exception.getMessage());
    }

    @Test
    public void testInvalidCrnDueToService() {
        CrnParseException exception = assertThrows(CrnParseException.class, () -> {
            Crn.fromString(invalidCrnService);
        });

        assertEquals("cookie is not a valid service value", exception.getMessage());
    }

    @Test
    public void testInvalidCrnDueToResourceType() {
        CrnParseException exception = assertThrows(CrnParseException.class, () -> {
            Crn.fromString(invalidCrnResourceType);
        });

        assertEquals("cookie is not a valid resource type value", exception.getMessage());
    }

    @Test
    public void testGetUserId() {
        Crn crn = CrnTestUtil.getUserCrnBuilder().setAccountId("accountId").setResource("userId").build();
        assertEquals("userId", crn.getUserId());

        crn = CrnTestUtil.getUserCrnBuilder().setAccountId("accountId").setResource("externalId/userId").build();
        assertEquals("userId", crn.getUserId());
    }

    @Test
    public void testMachineUser() {
        Crn crn = Crn.fromString(machineUserBaby);
        assertEquals("b8a64902-7765-4ddd-a4f3-df81ae585e10", crn.getUserId());
    }
}
