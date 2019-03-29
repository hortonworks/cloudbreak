package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CrnTest {

    private String exampleCrn = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String exampleCrn2 = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bd@a7-be07183720d3";

    private String exampleCrn3 = "crn:altus:iam:us-west-1:default:user:lnardai@cloudera.com";

    private String invalidCrn = "crn:something:chunked21dqw";

    private String invalidCrn2 = ":::::";

    @Test
    public void whenValidCrnIsAddedShouldExtractAccountId() {
        assertEquals("9d74eee4-1cad-45d7-b645-7ccf9edbb73d", Crn.fromString(exampleCrn).getAccountId());
    }

    @Test
    public void whenInvalidCrnIsAddedShouldReturnNull() {
        assertEquals(null, Crn.fromString(invalidCrn));
    }

    @Test
    public void whenInvalidCrnIsChecked() {
        assertFalse(Crn.isCrn(invalidCrn));
        assertFalse(Crn.isCrn(invalidCrn2));
    }

    @Test
    public void isInvalid() {
        assertTrue(Crn.isCrn(exampleCrn));
        assertTrue(Crn.isCrn(exampleCrn2));
        assertTrue(Crn.isCrn(exampleCrn3));
    }
}