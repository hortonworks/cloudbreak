package com.sequenceiq.cloudbreak.core.bootstrap.service

enum class OrchestratorType {
    HOST, CONTAINER;

    fun hostOrchestrator(): Boolean {
        return equals(HOST)
    }

    fun containerOrchestrator(): Boolean {
        return equals(CONTAINER)
    }
}
