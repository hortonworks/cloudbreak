package com.sequenceiq.cloudbreak.service.upgrade.matrix;

import static org.junit.Assert.assertEquals;
import static org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(Parameterized.class)
public class UpgradeMatrixServiceTest {

    @InjectMocks
    private UpgradeMatrixService underTest;

    private final String currentVersion;

    private final String targetVersion;

    private final boolean expectedResult;

    public UpgradeMatrixServiceTest(String currentVersion, String targetVersion, boolean expectedResult) {
        this.currentVersion = currentVersion;
        this.targetVersion = targetVersion;
        this.expectedResult = expectedResult;
    }

    @Before
    public void before() throws IOException {
        underTest = new UpgradeMatrixService();
        ReflectionTestUtils.setField(underTest, "upgradeMatrixDefinition", getUpgradeMatrixDefinition());
    }

    @Parameters(name = "{index}: Upgrade from {0} to {1} is permitted: {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"7a.2b.2c", "7.2.2", false},
                {"17.12.12", "7.2.2", false},
                {"a.b.c", "7.2.2", false},
                {"7a0b1", "7.2.2", false},
                {"7.1.0", "7.2.01", false},

                {"7.0.1", "7.2.2", false},
                {"7.1.0", "7.2.2", false},
                {"7.1.1", "7.2.2", false},
                {"7.2.0", "7.2.2", false},
                {"7.2.1", "7.2.2", false},
                {"7.2.2", "7.2.2", false},

                {"7.1.0", "7.2.6", false},
                {"7.1.1", "7.2.6", false},
                {"7.2.0", "7.2.6", false},
                {"7.2.1", "7.2.6", false},
                {"7.2.2", "7.2.6", false},
                {"7.2.3", "7.2.6", false},
                {"7.2.6", "7.2.6", false},

                {"7.1.0", "7.2.7", false},
                {"7.1.1", "7.2.7", false},
                {"7.2.0", "7.2.7", false},
                {"7.2.1", "7.2.7", false},
                {"7.2.2", "7.2.7", false},
                {"7.2.3", "7.2.7", false},
                {"7.2.6", "7.2.7", false},
                {"7.2.7", "7.2.7", false},

                {"7.1.0", "7.2.8", false},
                {"7.1.1", "7.2.8", false},
                {"7.2.0", "7.2.8", false},
                {"7.2.1", "7.2.8", false},
                {"7.2.2", "7.2.8", false},
                {"7.2.3", "7.2.8", false},
                {"7.2.6", "7.2.8", false},
                {"7.2.7", "7.2.8", false},
                {"7.2.8", "7.2.8", false},

                {"7.1.0", "7.2.9", false},
                {"7.1.1", "7.2.9", false},
                {"7.2.0", "7.2.9", false},
                {"7.2.1", "7.2.9", false},
                {"7.2.2", "7.2.9", false},
                {"7.2.3", "7.2.9", false},
                {"7.2.6", "7.2.9", false},
                {"7.2.7", "7.2.9", false},
                {"7.2.8", "7.2.9", false},
                {"7.2.9", "7.2.9", false},

                {"7.1.0", "7.2.10", false},
                {"7.1.1", "7.2.10", false},
                {"7.2.0", "7.2.10", false},
                {"7.2.1", "7.2.10", false},
                {"7.2.2", "7.2.10", false},
                {"7.2.3", "7.2.10", false},
                {"7.2.6", "7.2.10", false},
                {"7.2.7", "7.2.10", false},
                {"7.2.8", "7.2.10", false},
                {"7.2.9", "7.2.10", false},
                {"7.2.10", "7.2.10", false},

                {"7.1.0", "7.2.11", true},
                {"7.1.1", "7.2.11", false},
                {"7.2.0", "7.2.11", true},
                {"7.2.1", "7.2.11", true},
                {"7.2.2", "7.2.11", true},
                {"7.2.3", "7.2.11", false},
                {"7.2.6", "7.2.11", true},
                {"7.2.7", "7.2.11", true},
                {"7.2.8", "7.2.11", true},
                {"7.2.9", "7.2.11", true},
                {"7.2.10", "7.2.11", true},
                {"7.2.11", "7.2.11", false},
        });
    }

    @Test
    public void testIsPermittedByUpgradeMatrix() {
        assertEquals(expectedResult, underTest.permitByUpgradeMatrix(currentVersion, targetVersion));
    }

    private UpgradeMatrixDefinition getUpgradeMatrixDefinition() throws IOException {
        String upgradeMatrixJson = FileReaderUtils.readFileFromPath(Path.of("src/main/resources/definitions/upgrade-matrix-definition.json"));
        return JsonUtil.readValue(upgradeMatrixJson, UpgradeMatrixDefinition.class);
    }

}