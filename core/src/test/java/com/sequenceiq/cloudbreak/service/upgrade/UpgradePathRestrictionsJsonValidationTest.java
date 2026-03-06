package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validates that every {@code from} and {@code to} value in
 * {@code upgrade-path-restrictions.json} conforms to the pattern grammar
 * supported by {@link UpgradeVersionPatternMatcher}.
 *
 * <p>Valid forms:
 * <ul>
 *   <li>{@code *}</li>
 *   <li>{@code <X.Y.Z.*}</li>
 *   <li>{@code X.Y.Z.*}</li>
 *   <li>{@code X.Y.Z.P}</li>
 *   <li>{@code X.Y.Z.MIN-MAX}</li>
 *   <li>{@code X.Y.Z.MIN+}</li>
 * </ul>
 */
class UpgradePathRestrictionsJsonValidationTest {
    private static final String NUMBER = "\\d+";

    private static final String VERSION = NUMBER + "\\." + NUMBER + "\\." + NUMBER;

    private static final String ANY = "^\\*$";

    private static final String LESS_THAN = "^<" + VERSION + "\\.\\*$";

    private static final String PREFIX = "^" + VERSION + "\\.";

    private static final String SUFFIX = "(\\*|" + NUMBER + "|" + NUMBER + "-" + NUMBER + "|" + NUMBER + "\\+)$";

    private static final Pattern VALID_PATTERN = Pattern.compile(
            String.join("|",
                    ANY,
                    LESS_THAN,
                    PREFIX + SUFFIX
            )
    );

    @Test
    void allPatternsInJsonConformToGrammar() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("definitions/upgrade-path-restrictions.json")) {
            List<BlockedUpgradePath> rules = new ObjectMapper().readValue(is, new TypeReference<>() { });
            for (BlockedUpgradePath rule : rules) {
                assertValidPattern(rule.from());
                assertValidPattern(rule.to());
            }
        }
    }

    private void assertValidPattern(String pattern) {
        assertTrue(VALID_PATTERN.matcher(pattern).matches(),
                "Invalid pattern in upgrade-path-restrictions.json: \"" + pattern + "\"");
    }
}
