package com.sequenceiq.cloudbreak.domain

import java.util.Date

import com.sequenceiq.cloudbreak.common.type.CbUserRole

class CbUser(var userId: String?, val username: String, val account: String, val roles: List<CbUserRole>, val givenName: String, val familyName: String, val created: Date)
