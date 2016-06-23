package com.sequenceiq.cloudbreak.orchestrator.salt.client

import com.fasterxml.jackson.annotation.JsonValue

enum class SaltActionType private constructor(val action: String) {

    RUN("run"),
    STOP("stop")


}
