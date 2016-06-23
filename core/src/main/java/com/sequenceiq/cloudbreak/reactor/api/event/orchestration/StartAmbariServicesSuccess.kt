package com.sequenceiq.cloudbreak.reactor.api.event.orchestration

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class StartAmbariServicesSuccess : StackEvent {
    constructor(stackId: Long?) : super(stackId) {
    }

    constructor(selector: String, stackId: Long?) : super(selector, stackId) {
    }

}
