package com.sequenceiq.cloudbreak.controller

import javax.ws.rs.core.Response

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.AccountPreferencesEndpoint
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.domain.AccountPreferences
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.api.model.AccountPreferencesJson
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService
import com.sequenceiq.cloudbreak.service.account.ScheduledAccountPreferencesValidator

@Component
class AccountPreferencesController : AccountPreferencesEndpoint {

    @Autowired
    private val service: AccountPreferencesService? = null

    @Autowired
    private val validator: ScheduledAccountPreferencesValidator? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    override fun get(): AccountPreferencesJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val preferences = service!!.getOneByAccount(user)
        return convert(preferences)
    }

    override fun put(updateRequest: AccountPreferencesJson) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        service!!.saveOne(user, convert(updateRequest))
    }

    override fun post(updateRequest: AccountPreferencesJson) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        service!!.saveOne(user, convert(updateRequest))
    }

    override fun validate(): Response {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        if (user.roles.contains(CbUserRole.ADMIN)) {
            validator!!.validate()
        }
        return Response.status(Response.Status.ACCEPTED).build()
    }

    private fun convert(preferences: AccountPreferences): AccountPreferencesJson {
        return conversionService!!.convert<AccountPreferencesJson>(preferences, AccountPreferencesJson::class.java)
    }

    private fun convert(preferences: AccountPreferencesJson): AccountPreferences {
        return conversionService!!.convert<AccountPreferences>(preferences, AccountPreferences::class.java)
    }
}
