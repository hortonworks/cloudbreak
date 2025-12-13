package com.sequenceiq.cloudbreak.service.upgrade.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
class RuntimeUpgradeMatrixDefinitionProviderTest {

    private static final String UPGRADE_MATRIX_DEFINITION_FILE = "upgrade-matrix-definition";

    @InjectMocks
    private UpgradeMatrixDefinitionProvider underTest;

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Test
    void testGetUpgradeMatrixShouldReadTheUpgradeMatrixFromJson() throws IOException {
        String upgradeMatrixJson = getUpgradeMatrixJson();
        when(cloudbreakResourceReaderService.resourceDefinition(UPGRADE_MATRIX_DEFINITION_FILE)).thenReturn(upgradeMatrixJson);

        UpgradeMatrixDefinition actual = underTest.getUpgradeMatrix();

        assertNotNull(actual);
        assertEquals(9, actual.getRuntimeUpgradeMatrix().size());
        assertTrue(actual.getRuntimeUpgradeMatrix().stream().allMatch(matrix -> containsSourceVersions(matrix) && containsTargetVersions(matrix)));
    }

    private boolean containsTargetVersions(RuntimeUpgradeMatrix matrix) {
        return !matrix.getTargetRuntime().getVersion().isBlank();
    }

    private boolean containsSourceVersions(RuntimeUpgradeMatrix matrix) {
        return matrix.getSourceRuntime()
                .stream()
                .map(Runtime::getVersion)
                .noneMatch(String::isBlank);
    }

    private String getUpgradeMatrixJson() throws IOException {
        return FileReaderUtils.readFileFromPath(Path.of("src/main/resources/definitions/upgrade-matrix-definition.json"));
    }
}