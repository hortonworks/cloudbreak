package com.sequenceiq.cloudbreak.api.endpoint.v4.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NameOrCrnTest {

    private static final String CRN = "my-crn";

    private static final String NAME = "my-name";

    @Test
    public void testOfName() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(NAME);

        assertEquals(NAME, nameOrCrn.getName());
    }

    @Test
    public void testOfNameWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> NameOrCrn.ofName(null), "Name must be provided.");
    }

    @Test
    public void testOfNameWhenZeroLength() {
        assertThrows(IllegalArgumentException.class, () -> NameOrCrn.ofName(""), "Name must be provided.");
    }

    @Test
    public void testOfCrn() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(CRN);

        assertEquals(CRN, nameOrCrn.getCrn());
    }

    @Test
    public void testOfCrnWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> NameOrCrn.ofCrn(null), "Crn must be provided.");
    }

    @Test
    public void testOfCrnWhenZeroLength() {
        assertThrows(IllegalArgumentException.class, () -> NameOrCrn.ofCrn(""), "Crn must be provided.");
    }

    @Test
    public void testToStringWhenName() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(NAME);

        assertEquals("[NameOrCrn of name: 'my-name']", nameOrCrn.toString());
    }

    @Test
    public void testToStringWhenCrn() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(CRN);

        assertEquals("[NameOrCrn of crn: 'my-crn']", nameOrCrn.toString());
    }

    @Test
    public void testHasName() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(NAME);

        assertTrue(nameOrCrn.hasName());
    }

    @Test
    public void testHasNameWhenEmpty() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(CRN);

        assertFalse(nameOrCrn.hasName());
    }

    @Test
    public void testHasCrn() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(CRN);

        assertTrue(nameOrCrn.hasCrn());
    }

    @Test
    public void testHasCrnWhenEmpty() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(NAME);

        assertFalse(nameOrCrn.hasCrn());
    }

    @Test
    public void testGetNameWhenCrnProvided() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(CRN);

        assertThrows(IllegalArgumentException.class, nameOrCrn::getName, "Request to get name when crn was provided on [NameOrCrn of crn: 'my-crn']");
    }

    @Test
    public void testGetCrnWhenCrnProvided() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(NAME);

        assertThrows(IllegalArgumentException.class, nameOrCrn::getCrn, "Request to get crn when name was provided on [NameOrCrn of name: 'my-name']");
    }
}
