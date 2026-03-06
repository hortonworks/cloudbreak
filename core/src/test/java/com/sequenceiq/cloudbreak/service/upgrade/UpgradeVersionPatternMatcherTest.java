package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpgradeVersionPatternMatcherTest {

    private final UpgradeVersionPatternMatcher underTest = new UpgradeVersionPatternMatcher();

    @Test
    void wildcardMatchesAnyVersion() {
        assertTrue(underTest.matches("*", context("7.2.18", 300)));
        assertTrue(underTest.matches("*", context("7.3.1", 0)));
    }

    @Test
    void olderThanMajorMatchesVersionsStrictlyBeforeLimit() {
        assertTrue(underTest.matches("<7.2.17.*", context("7.2.16", 0)));
        assertTrue(underTest.matches("<7.2.17.*", context("7.2.16", 9999)));
        assertFalse(underTest.matches("<7.2.17.*", context("7.2.17", 0)));
        assertFalse(underTest.matches("<7.2.17.*", context("7.2.18", 0)));
    }

    @Test
    void majorWildcardMatchesAnyPatchOfThatMajor() {
        assertTrue(underTest.matches("7.2.18.*", context("7.2.18", 0)));
        assertTrue(underTest.matches("7.2.18.*", context("7.2.18", 1200)));
        assertFalse(underTest.matches("7.2.18.*", context("7.2.17", 0)));
        assertFalse(underTest.matches("7.2.18.*", context("7.3.1", 0)));
    }

    @Test
    void exactMatchRequiresBothMajorAndPatch() {
        assertTrue(underTest.matches("7.3.2.0", context("7.3.2", 0)));
        assertFalse(underTest.matches("7.3.2.0", context("7.3.2", 1)));
        assertFalse(underTest.matches("7.3.2.0", context("7.3.1", 0)));
    }

    @Test
    void patchMinMatchesPatchAtAndAboveMin() {
        assertTrue(underTest.matches("7.2.17.600+", context("7.2.17", 600)));
        assertTrue(underTest.matches("7.2.17.600+", context("7.2.17", 1100)));
        assertFalse(underTest.matches("7.2.17.600+", context("7.2.17", 599)));
        assertFalse(underTest.matches("7.2.17.600+", context("7.2.18", 600)));
    }

    @Test
    void patchRangeMatchesPatchWithinInclusiveBounds() {
        assertTrue(underTest.matches("7.3.1.0-400", context("7.3.1", 0)));
        assertTrue(underTest.matches("7.3.1.0-400", context("7.3.1", 400)));
        assertTrue(underTest.matches("7.3.1.0-400", context("7.3.1", 200)));
        assertFalse(underTest.matches("7.3.1.0-400", context("7.3.1", 401)));
        assertFalse(underTest.matches("7.3.1.0-400", context("7.3.2", 200)));
    }

    private VersionComparisonContext context(String major, int patch) {
        return new VersionComparisonContext.Builder()
                .withMajorVersion(major)
                .withPatchVersion(patch)
                .build();
    }
}
