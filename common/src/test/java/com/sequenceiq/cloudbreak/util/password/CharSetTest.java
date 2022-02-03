package com.sequenceiq.cloudbreak.util.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CharSetTest {

    @Test
    public void testNotContainsDuplicates() {
        assertEquals(List.of('a', 'b', 'c'), CharSet.fromString("abac").getValues());
    }

    @Test
    public void testEmptyIsNotAllowed() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CharSet.fromString(""));
        assertEquals("Character set must contain at least one value.", exception.getMessage());
    }

    @Test
    public void testAllowOneElement() {
        assertEquals(List.of('a'), CharSet.fromString("a").getValues());
    }
}