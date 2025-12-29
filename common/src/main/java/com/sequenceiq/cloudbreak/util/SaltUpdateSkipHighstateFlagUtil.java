package com.sequenceiq.cloudbreak.util;

import java.util.Map;
import java.util.Optional;

public class SaltUpdateSkipHighstateFlagUtil {

    private static final String SKIP_HIGHSTATE = "skipHighstate";

    private SaltUpdateSkipHighstateFlagUtil() {

    }

    public static void putToVariables(boolean skipHighstate, Map<Object, Object> variables) {
        variables.put(SKIP_HIGHSTATE, skipHighstate);
    }

    public static Boolean getFromVariables(Map<Object, Object> variables) {
        return Optional.ofNullable(variables.get(SKIP_HIGHSTATE))
                .map(Boolean.class::cast)
                .orElse(Boolean.FALSE);
    }
}
