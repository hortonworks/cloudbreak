package com.sequenceiq.cloudbreak.service.upgrade.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class UpgradeMatrixServiceTest {

    private UpgradeMatrixService underTest = new UpgradeMatrixService();

    @BeforeEach
    public void before() throws IOException {
        ReflectionTestUtils.setField(underTest, "upgradeMatrixDefinition", getUpgradeMatrixDefinition());
    }

    static Collection<Object[]> data() {
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

    @MethodSource("data")
    @ParameterizedTest
    void testIsPermittedByUpgradeMatrix(String currentVersion, String targetVersion, boolean expectedResult) {
        assertEquals(expectedResult, underTest.permitByUpgradeMatrix(currentVersion, targetVersion));
    }

    private UpgradeMatrixDefinition getUpgradeMatrixDefinition() throws IOException {
        String upgradeMatrixJson = FileReaderUtils.readFileFromPath(Path.of("src/main/resources/definitions/upgrade-matrix-definition.json"));
        return JsonUtil.readValue(upgradeMatrixJson, UpgradeMatrixDefinition.class);
    }

}