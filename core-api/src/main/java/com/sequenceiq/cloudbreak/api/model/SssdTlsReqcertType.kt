package com.sequenceiq.cloudbreak.api.model

enum class SssdTlsReqcertType {

    NEVER, ALLOW, TRY, DEMAND, HARD;

    val representation: String
        get() = name.toLowerCase()
}
