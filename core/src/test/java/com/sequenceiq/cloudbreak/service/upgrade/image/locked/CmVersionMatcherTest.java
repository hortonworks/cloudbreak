package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

class CmVersionMatcherTest {

    private final Image current = mock(Image.class);

    private final Image candidate = mock(Image.class);

    private final CmVersionMatcher underTest = new CmVersionMatcher();

    static Object[][] parameters() {
        return new Object[][]{
                {"versions equal", Boolean.TRUE, "1234", "1234"},
                {"versions different", Boolean.FALSE, "1234", "4321"},
                {"candidate version null", Boolean.FALSE, "1234", null},
                {"candidate version empty", Boolean.FALSE, "1234", ""},
                {"current version null", Boolean.FALSE, null, "1234"},
                {"current version empty", Boolean.FALSE, "", "1234"},
                {"both version null", Boolean.FALSE, null, null},
                {"both version empty", Boolean.FALSE, "", ""},
                {"both version blank", Boolean.FALSE, " ", " "},
        };
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testVersionMatcher(String name, Boolean expectedResult, String currentVersion, String candidateVersion) {
        when(current.getCmBuildNumber()).thenReturn(currentVersion);
        when(candidate.getCmBuildNumber()).thenReturn(candidateVersion);

        boolean result = underTest.isCmVersionMatching(current, candidate);

        assertEquals(expectedResult, result);
    }
}