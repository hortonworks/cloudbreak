package com.sequenceiq.environment.experience.liftie;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public enum LiftieIgnorableClusterStatuses {

    DELETED,
    DELETING;

    public boolean isNotEqualTo(String status) {
        return !isEqualTo(status);
    }

    public boolean isEqualTo(String status) {
        return name().equalsIgnoreCase(status);
    }

    public static boolean notContains(String status) {
        return !contains(status);
    }

    public static boolean contains(String status) {
        return !isEmpty(status) && stream(LiftieIgnorableClusterStatuses.values())
                .map(s -> s.name().toLowerCase())
                .collect(toSet())
                .contains(status.toLowerCase());
    }

}
