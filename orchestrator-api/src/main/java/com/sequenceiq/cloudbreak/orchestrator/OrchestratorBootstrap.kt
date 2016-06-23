package com.sequenceiq.cloudbreak.orchestrator

interface OrchestratorBootstrap {

    @Throws(Exception::class)
    fun call(): Boolean?
}
