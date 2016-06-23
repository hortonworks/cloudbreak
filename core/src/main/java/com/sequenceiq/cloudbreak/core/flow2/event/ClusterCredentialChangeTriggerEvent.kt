package com.sequenceiq.cloudbreak.core.flow2.event

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class ClusterCredentialChangeTriggerEvent(selector: String, stackId: Long?, val user: String, val password: String) : StackEvent(selector, stackId)
