package com.sequenceiq.cloudbreak.api.endpoint.v4.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn.NameOrCrnReader;

public class NameOrCrnTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testOfName() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("my-name");

        assertEquals("my-name", nameOrCrn.name);
    }

    @Test
    public void testOfNameWhenNull() {
        thrown.expectMessage("Name must be provided.");
        thrown.expect(IllegalArgumentException.class);

        NameOrCrn.ofName(null);
    }

    @Test
    public void testOfNameWhenZeroLength() {
        thrown.expectMessage("Name must be provided.");
        thrown.expect(IllegalArgumentException.class);

        NameOrCrn.ofName("");
    }

    @Test
    public void testOfCrn() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn("my-crn");

        assertEquals("my-crn", nameOrCrn.crn);
    }

    @Test
    public void testOfCrnWhenNull() {
        thrown.expectMessage("Crn must be provided.");
        thrown.expect(IllegalArgumentException.class);

        NameOrCrn.ofCrn(null);
    }

    @Test
    public void testOfCrnWhenZeroLength() {
        thrown.expectMessage("Crn must be provided.");
        thrown.expect(IllegalArgumentException.class);

        NameOrCrn.ofCrn("");
    }

    @Test
    public void testToStringWhenName() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("my-name");

        assertEquals("[NameOrCrn of name: 'my-name']", nameOrCrn.toString());
    }

    @Test
    public void testToStringWhenCrn() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn("my-crn");

        assertEquals("[NameOrCrn of crn: 'my-crn']", nameOrCrn.toString());
    }

    @Test
    public void testReaderHasName() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("my-name");
        NameOrCrnReader nameOrCrnReader = NameOrCrnReader.create(nameOrCrn);

        assertTrue(nameOrCrnReader.hasName());
    }

    @Test
    public void testReaderHasNameWhenEmpty() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn("my-crn");
        NameOrCrnReader nameOrCrnReader = NameOrCrnReader.create(nameOrCrn);

        assertFalse(nameOrCrnReader.hasName());
    }

    @Test
    public void testReaderHasCrn() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn("my-crn");
        NameOrCrnReader nameOrCrnReader = NameOrCrnReader.create(nameOrCrn);

        assertTrue(nameOrCrnReader.hasCrn());
    }

    @Test
    public void testReaderHasCrnWhenEmpty() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("my-name");
        NameOrCrnReader nameOrCrnReader = NameOrCrnReader.create(nameOrCrn);

        assertFalse(nameOrCrnReader.hasCrn());
    }

    @Test
    public void testReaderGetName() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("my-name");
        NameOrCrnReader nameOrCrnReader = NameOrCrnReader.create(nameOrCrn);

        assertEquals("my-name", nameOrCrnReader.getName());
    }

    @Test
    public void testReaderGetCrn() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn("my-crn");
        NameOrCrnReader nameOrCrnReader = NameOrCrnReader.create(nameOrCrn);

        assertEquals("my-crn", nameOrCrnReader.getCrn());
    }

    @Test
    public void testReaderWhenNull() {
        thrown.expectMessage("Name or crn should not be null.");
        thrown.expect(IllegalArgumentException.class);

        NameOrCrnReader.create(null);
    }
}
