package com.sequenceiq.cloudbreak.shell.model

/**
 * Holds information about the focus. Focus give you the ability to
 * provide context sensitive commands.
 */
class Focus(val value: String, val type: FocusType) {

    val prefix: String
        get() = type.prefix()

    /**
     * Checks if the current focus exists with the provided one.

     * @param type type to check with the current
     * *
     * @return true if they match false otherwise
     */
    fun isType(type: FocusType): Boolean {
        return this.type == type
    }
}
