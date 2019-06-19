package com.sequenceiq.redbeams.dto;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class CredentialTest {

    private Credential underTest;

    @Before
    public void setUp() {
        underTest = new Credential("name", "attributes", "crn");
    }

    @Test
    public void testGetters() {
        assertEquals("name", underTest.getName());
        assertEquals("attributes", underTest.getAttributes());
        assertEquals("crn", underTest.getCrn());
    }

}
