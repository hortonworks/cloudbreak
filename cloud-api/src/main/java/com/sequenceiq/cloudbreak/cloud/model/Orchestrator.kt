package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType

class Orchestrator private constructor(value: String) : StringType(value) {
    companion object {

        fun orchestrator(value: String): Orchestrator {
            return Orchestrator(value)
        }
    }

}
