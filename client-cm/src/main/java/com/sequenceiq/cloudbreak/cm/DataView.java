package com.sequenceiq.cloudbreak.cm;

import java.util.Locale;

import com.google.common.base.Joiner;

public enum DataView {
    SUMMARY,
    FULL,
    /**
     * Entities with health test results and health test explanation.
     * Generating and transferring health check explanation for entities can be
     * very expensive.
     **/
    FULL_WITH_HEALTH_CHECK_EXPLANATION,
    EXPORT,
    /** All passwords and other sensitive fields are marked as REDACTED. */
    EXPORT_REDACTED;

    // List of supported views
    private static final String SUPPORTED_VIEWS =
            Joiner.on(", ").join(values()).toLowerCase(Locale.ROOT);

    public static DataView fromString(String s) {
        if (s == null || s.isEmpty()) {
            return SUMMARY;
        }

        return valueOf(s.toUpperCase(Locale.ROOT));
    }
}

