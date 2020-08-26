package com.sequenceiq.freeipa.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
public class AvailabilityCheckerTest {

    private final Versioned afterVersion1 = () -> "2.19.0";

    private TestAvailbilityChecker underTest;

    @Before
    public void before() {
        underTest = new TestAvailbilityChecker();
    }

    @Test
    public void testAvailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.20.0-rc.1");
        assertTrue(underTest.isAvailable(stack, afterVersion1));

        stack.setAppVersion("2.21.0-rc.1");
        assertTrue(underTest.isAvailable(stack, afterVersion1));

        stack.setAppVersion("2.20.0");
        assertTrue(underTest.isAvailable(stack, afterVersion1));

        stack.setAppVersion("2.20.0-dev.2");
        assertTrue(underTest.isAvailable(stack, afterVersion1));
    }

    @Test
    public void testUnavailable() {
        Stack stack = new Stack();

        stack.setAppVersion("2.19.0");
        assertFalse(underTest.isAvailable(stack, afterVersion1));

        stack.setAppVersion("2.19.0-rc.23");
        assertFalse(underTest.isAvailable(stack, afterVersion1));

        stack.setAppVersion("2.19.0-rc.122");
        assertFalse(underTest.isAvailable(stack, afterVersion1));

        stack.setAppVersion("2.19.0-dev.23");
        assertFalse(underTest.isAvailable(stack, afterVersion1));

        stack.setAppVersion("2.18.0");
        assertFalse(underTest.isAvailable(stack, afterVersion1));

        stack.setAppVersion("2.18.0-rc.2");
        assertFalse(underTest.isAvailable(stack, afterVersion1));
    }

    @Test
    public void testAppVersionIsBlank() {
        Stack stack = new Stack();
        assertFalse(underTest.isAvailable(stack, afterVersion1));

        stack.setAppVersion("");
        assertFalse(underTest.isAvailable(stack, afterVersion1));

        stack.setAppVersion(" ");
        assertFalse(underTest.isAvailable(stack, afterVersion1));
    }

    static class TestAvailbilityChecker extends AvailabilityChecker {

        public boolean isAvailable(Stack stack, Versioned versioned) {
            return super.isAvailable(stack, versioned);
        }
    }
}