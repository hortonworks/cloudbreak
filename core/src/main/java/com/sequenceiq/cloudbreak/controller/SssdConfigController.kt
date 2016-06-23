package com.sequenceiq.cloudbreak.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.SssdConfigEndpoint
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.SssdConfig
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.sssdconfig.SssdConfigService

@Component
class SssdConfigController : SssdConfigEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    @Autowired
    private val sssdConfigService: SssdConfigService? = null

    override fun postPrivate(sssdConfigRequest: SssdConfigRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createConfig(user, sssdConfigRequest, false)
    }

    override fun postPublic(sssdConfigRequest: SssdConfigRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createConfig(user, sssdConfigRequest, true)
    }

    override fun getPrivates(): Set<SssdConfigResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val configs = sssdConfigService!!.retrievePrivateConfigs(user)
        return toJsonSet(configs)
    }

    override fun getPublics(): Set<SssdConfigResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val configs = sssdConfigService!!.retrieveAccountConfigs(user)
        return toJsonSet(configs)
    }

    override fun getPrivate(name: String): SssdConfigResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val config = sssdConfigService!!.getPrivateConfig(name, user)
        return conversionService!!.convert<SssdConfigResponse>(config, SssdConfigResponse::class.java)
    }

    override fun getPublic(name: String): SssdConfigResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val config = sssdConfigService!!.getPublicConfig(name, user)
        return conversionService!!.convert<SssdConfigResponse>(config, SssdConfigResponse::class.java)
    }

    override fun get(id: Long?): SssdConfigResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val config = sssdConfigService!!.get(id)
        return conversionService!!.convert<SssdConfigResponse>(config, SssdConfigResponse::class.java)
    }

    override fun delete(id: Long?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        sssdConfigService!!.delete(id, user)
    }

    override fun deletePublic(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        sssdConfigService!!.delete(name, user)
    }

    override fun deletePrivate(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        sssdConfigService!!.delete(name, user)
    }

    private fun createConfig(user: CbUser, request: SssdConfigRequest, publicInAccount: Boolean): IdJson {
        var config = conversionService!!.convert<SssdConfig>(request, SssdConfig::class.java)
        config.isPublicInAccount = publicInAccount
        config = sssdConfigService!!.create(user, config)
        return IdJson(config.id)
    }

    private fun toJsonSet(configs: Set<SssdConfig>): Set<SssdConfigResponse> {
        return conversionService!!.convert(configs, TypeDescriptor.forObject(configs),
                TypeDescriptor.collection(Set<Any>::class.java, TypeDescriptor.valueOf(SssdConfigResponse::class.java))) as Set<SssdConfigResponse>
    }
}
