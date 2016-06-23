package com.sequenceiq.cloudbreak.reactor.api.event.resource

class UpdateInstanceMetadataRequest(stackId: Long?, hostGroupName: String, val hostNames: Set<String>) : AbstractClusterScaleRequest(stackId, hostGroupName)
