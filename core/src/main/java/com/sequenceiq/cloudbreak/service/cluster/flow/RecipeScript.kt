package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.api.model.ExecutionType

class RecipeScript(val script: String, val clusterLifecycleEvent: ClusterLifecycleEvent, val executionType: ExecutionType)
