package com.sequenceiq.cloudbreak.cloud.model

class PlatformOrchestrators(val orchestrators: Map<Platform, Collection<Orchestrator>>, val defaults: Map<Platform, Orchestrator>)
