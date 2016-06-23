package com.sequenceiq.cloudbreak.orchestrator.model

import java.util.Date

class ContainerName(private val name: String?, private val namePrefix: String) {

    fun getName(): String {
        return name ?: String.format("%s-%s", namePrefix, Date().time.toString())
    }
}
