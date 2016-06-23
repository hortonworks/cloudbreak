package com.sequenceiq.cloudbreak.client


class ConfigKey(val isSecure: Boolean, val isDebug: Boolean) {

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val configKey = o as ConfigKey?

        if (isSecure != configKey.isSecure) {
            return false
        }
        return isDebug == configKey.isDebug
    }

    override fun hashCode(): Int {
        var result = if (isSecure) 1 else 0
        result = 31 * result + if (isDebug) 1 else 0
        return result
    }

    override fun toString(): String {
        return "ConfigKey{"
        +"secure=" + isSecure
        +", debug=" + isDebug
        +'}'
    }
}
