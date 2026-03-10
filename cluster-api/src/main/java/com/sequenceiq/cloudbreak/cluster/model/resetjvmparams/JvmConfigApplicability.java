package com.sequenceiq.cloudbreak.cluster.model.resetjvmparams;

/**
 * Mirrors {@code com.cloudera.api.swagger.model.AutoConfigApplicability}.
 * Kept in {@code cluster-api} so upstream modules are not coupled to the CM swagger library.
 */
public enum JvmConfigApplicability {

    /** Config value will change when recalculation is applied. */
    RECONFIGURABLE,

    /** Config value would not change because the calculated value equals the current value. */
    UNAFFECTED_DUE_TO_EQUAL_VALUE,

    /** Config value would not change because it was explicitly set by the user. */
    UNAFFECTED_CONFIGURED_BY_USER
}
