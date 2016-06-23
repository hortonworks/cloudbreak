package com.sequenceiq.cloudbreak.cloud.model

import com.google.common.collect.ImmutableList

class Security(rules: List<SecurityRule>) {

    val rules: List<SecurityRule>

    init {
        this.rules = ImmutableList.copyOf(rules)
    }

    override fun toString(): String {
        val sb = StringBuilder("Security{")
        sb.append("rules=").append(rules)
        sb.append('}')
        return sb.toString()
    }
}
