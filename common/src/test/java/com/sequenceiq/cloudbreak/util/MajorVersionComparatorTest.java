package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.util.MajorVersionComparatorTest.VersionedImpl.versioned;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.common.type.Versioned;

public class MajorVersionComparatorTest {

    private final MajorVersionComparator underTest = new MajorVersionComparator();

    @ParameterizedTest
    @MethodSource(value = "getVersions")
    void test(Versioned v1, Versioned v2, int expectedOutcome) {
        assertEquals(expectedOutcome, underTest.compare(v1, v2));
    }

    @Test
    void testCompareWhenFirstNotANumber() {
        Versioned v1 = versioned("notANumber.13");
        Versioned v2 = versioned("10.11");

        assertThrows(NumberFormatException.class, () ->
            underTest.compare(v1, v2)
        );
    }

    @Test
    void testCompareWhenSecondNotANumber() {
        Versioned v1 = versioned("10.11");
        Versioned v2 = versioned("notANumber.13");

        assertThrows(NumberFormatException.class, () ->
                underTest.compare(v1, v2)
        );
    }

    private static Object[][] getVersions() {
        return new Object[][] {
                { versioned("10"), versioned("10"), 0},
                { versioned("10"), versioned("1"), 1},
                { versioned("2"), versioned("10"), -1},
                { versioned("10.2"), versioned("10.2"), 0},
                { versioned("10.1"), versioned("10.23"), 0},
                { versioned("10.23"), versioned("10.2"), 0},
                { versioned("10.23.4"), versioned("10.45.14"), 0},
                { versioned("10.whatever"), versioned("10.foo"), 0},
                { versioned("0.1"), versioned("0.2"), 0},
                { versioned("10"), versioned("10.13"), 0},
                { versioned("10.54"), versioned("10"), 0},
        };
    }

    static class VersionedImpl implements Versioned {

        private final String version;

        VersionedImpl(String version) {
            this.version = version;
        }

        static VersionedImpl versioned(String version) {
            return new VersionedImpl(version);
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "VersionedImpl{" +
                    "version='" + version + '\'' +
                    '}';
        }
    }
}
