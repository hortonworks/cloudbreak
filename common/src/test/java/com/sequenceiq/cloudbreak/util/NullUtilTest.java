package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NullUtilTest {

    @Test
    void throwIfNullTestWhenNull() {
        assertThrows(UnsupportedOperationException.class, () -> NullUtil.throwIfNull(null, UnsupportedOperationException::new));
    }

    @Test
    void throwIfNullTestWhenNonNull() {
        NullUtil.throwIfNull(12, UnsupportedOperationException::new);
    }

    @Test
    void testAllNullWhenInputArrayIsNullThenTrueShouldReturn() {
        assertTrue(NullUtil.allNull(null));
    }

    @Test
    void testAllNullWhenOneInputIsNotNullThenFalseShouldReturn() {
        assertFalse(NullUtil.allNull("someStuff", null));
    }

    @Test
    void testAllNullWhenAllTheInputsAreNotNullThenFalseShouldReturn() {
        assertFalse(NullUtil.allNull("someStuff", "someOtherStuff"));
    }

    @Test
    void testAllNullWhenAllTheMultipleInputsAreNullThenTrueShouldReturn() {
        assertTrue(NullUtil.allNull(null, null));
    }

}