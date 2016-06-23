package com.sequenceiq.cloudbreak.orchestrator.salt.client

enum class SaltClientType private constructor(val type: String) {

    LOCAL("local"),
    RUNNER("runner"),
    LOCAL_ASYNC("local_async")
}
