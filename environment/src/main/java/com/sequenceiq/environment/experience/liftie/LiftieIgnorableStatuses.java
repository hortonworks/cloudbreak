package com.sequenceiq.environment.experience.liftie;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum LiftieIgnorableStatuses {

    DELETED;

    static boolean notContains(String status) {
        return !contains(status);
    }

    static boolean contains(String status) {
        if (status == null) {
            return false;
        }
        return Arrays.asList(LiftieIgnorableStatuses.values())
                .stream()
                .map(s -> s.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet()).contains(status.toLowerCase(Locale.ROOT));
    }

}
