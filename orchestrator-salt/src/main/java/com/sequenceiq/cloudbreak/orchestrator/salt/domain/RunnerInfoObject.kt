package com.sequenceiq.cloudbreak.orchestrator.salt.domain

class RunnerInfoObject {

    var comment: String? = null

    var name: String? = null

    var startTime: String? = null

    var result: Boolean = false

    var duration: String? = null

    var runNum: Int? = null

    var changes: Map<String, Any>? = null
}
