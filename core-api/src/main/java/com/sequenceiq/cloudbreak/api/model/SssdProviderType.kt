package com.sequenceiq.cloudbreak.api.model

enum class SssdProviderType private constructor(val type: String) {

    LDAP("ldap"),
    ACTIVE_DIRECTORY("ad"),
    IPA("ipa")
}
