package com.sequenceiq.cloudbreak.shell.model

enum class FocusType private constructor(private val prefix: String) {

    MARATHON("cloudbreak-shell:marathon"),
    ROOT("");

    fun prefix(): String {
        return prefix
    }
}
