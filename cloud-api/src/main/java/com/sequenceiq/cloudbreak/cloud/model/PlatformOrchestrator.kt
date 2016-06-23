package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes

class PlatformOrchestrator(types: Collection<Orchestrator>, defaultType: Orchestrator) : CloudTypes<Orchestrator>(types, defaultType)
