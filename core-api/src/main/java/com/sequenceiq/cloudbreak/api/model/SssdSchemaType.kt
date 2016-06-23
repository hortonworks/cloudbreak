package com.sequenceiq.cloudbreak.api.model

enum class SssdSchemaType private constructor(val representation: String) {

    RFC2307("rfc2307"),
    RFC2307BIS("rfc2307bis"),
    IPA("IPA"),
    AD("AD")
}
