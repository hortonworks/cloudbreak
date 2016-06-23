package com.sequenceiq.cloudbreak.orchestrator.salt.client.target

class Glob(override val target: String) : Target<String> {

    override val type: String
        get() = "glob"

    companion object {

        val ALL = Glob("*")
    }
}