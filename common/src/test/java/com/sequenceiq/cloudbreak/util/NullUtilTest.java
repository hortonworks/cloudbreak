package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

}