package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionEqualToLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderThanLimited;

/**
 * Matches a version pattern string against a {@link VersionComparisonContext}.
 *
 * <p>Supported pattern syntax (used in {@code from} and {@code to} fields of
 * {@code upgrade-path-restrictions.json}):
 * <ul>
 *   <li>{@code *}              — matches any version</li>
 *   <li>{@code <X.Y.Z.*}       — matches any version whose major is strictly older than X.Y.Z (e.g. {@code <7.2.17.*})</li>
 *   <li>{@code X.Y.Z.*}        — matches any patch of major X.Y.Z</li>
 *   <li>{@code X.Y.Z.P}        — exact patch match (e.g. {@code 7.3.2.0})</li>
 *   <li>{@code X.Y.Z.MIN-MAX}  — patch in inclusive range (e.g. {@code 7.3.1.0-400})</li>
 *   <li>{@code X.Y.Z.MIN+}     — patch >= MIN (e.g. {@code 7.2.17.600+})</li>
 * </ul>
 */
class UpgradeVersionPatternMatcher {

    /**
     * Returns {@code true} if the given version context satisfies the pattern.
     */
    boolean matches(String pattern, VersionComparisonContext context) {
        if ("*".equals(pattern)) {
            return true;
        }

        String majorVersion = context.getMajorVersion();
        int patchVersion = context.getPatchVersion().orElse(0);

        // "<X.Y.Z.*" — major must be strictly older than X.Y.Z; patch is irrelevant
        if (pattern.startsWith("<")) {
            String limitMajor = pattern.substring(1, pattern.lastIndexOf(".*"));
            return isVersionOlderThanLimited(() -> majorVersion, () -> limitMajor);
        }

        // All remaining patterns are "major.patchSpec" — split on the last dot
        int lastDot = pattern.lastIndexOf('.');
        String patternMajor = pattern.substring(0, lastDot);
        String patch = pattern.substring(lastDot + 1);

        if (!isVersionEqualToLimited(majorVersion, () -> patternMajor)) {
            return false;
        }

        // "X.Y.Z.*" — any patch of this major
        if ("*".equals(patch)) {
            return true;
        }
        // "X.Y.Z.MIN+" — patch >= MIN
        if (patch.endsWith("+")) {
            int minPatch = Integer.parseInt(patch.substring(0, patch.length() - 1));
            return patchVersion >= minPatch;
        }
        // "X.Y.Z.MIN-MAX" — patch in [MIN, MAX]
        if (patch.contains("-")) {
            String[] parts = patch.split("-");
            int minPatch = Integer.parseInt(parts[0]);
            int maxPatch = Integer.parseInt(parts[1]);
            return patchVersion >= minPatch && patchVersion <= maxPatch;
        }
        // "X.Y.Z.P" — exact patch match
        return patchVersion == Integer.parseInt(patch);
    }
}
