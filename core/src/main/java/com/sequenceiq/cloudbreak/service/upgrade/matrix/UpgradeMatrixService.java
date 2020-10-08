package com.sequenceiq.cloudbreak.service.upgrade.matrix;

import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UpgradeMatrixService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeMatrixService.class);

    @Inject
    private UpgradeMatrixDefinition upgradeMatrixDefinition;

    public boolean permitByUpgradeMatrix(String currentVersion, String targetVersion) {
        Set<RuntimeUpgradeMatrix> upgradeMatrix = getUpgradeMatrix();
        return upgradeMatrix.stream()
                .filter(matrix -> matchesTargetVersion(targetVersion, matrix))
                .anyMatch(matchesCurrentVersion(currentVersion));
    }

    private Set<RuntimeUpgradeMatrix> getUpgradeMatrix() {
        return upgradeMatrixDefinition.getRuntimeUpgradeMatrix();
    }

    private boolean matchesTargetVersion(String targetVersion, RuntimeUpgradeMatrix matrix) {
        return matches(targetVersion, matrix.getTargetRuntime().getVersion());
    }

    private Predicate<RuntimeUpgradeMatrix> matchesCurrentVersion(String currentVersion) {
        return matrix -> matrix.getSourceRuntime().stream()
                .anyMatch(sourceRuntime -> matches(currentVersion, sourceRuntime.getVersion()));
    }

    private boolean matches(String version, String pattern) {
        try {
            return version.matches(pattern);
        } catch (PatternSyntaxException e) {
            LOGGER.warn("Failed to process regular expression {}", pattern, e);
            return false;
        }
    }
}
