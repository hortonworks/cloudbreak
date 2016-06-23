package com.sequenceiq.cloudbreak.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.ClusterTemplateEndpoint
import com.sequenceiq.cloudbreak.api.model.ClusterTemplateRequest
import com.sequenceiq.cloudbreak.api.model.ClusterTemplateResponse
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.ClusterTemplate
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.clustertemplate.ClusterTemplateService

@Component
class ClusterTemplateController : ClusterTemplateEndpoint {

    @Autowired
    private val clusterTemplateService: ClusterTemplateService? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    override fun postPrivate(clusterTemplateRequest: ClusterTemplateRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createClusterTemplateRequest(user, clusterTemplateRequest, false)
    }

    override fun postPublic(clusterTemplateRequest: ClusterTemplateRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createClusterTemplateRequest(user, clusterTemplateRequest, true)
    }

    override fun getPrivates(): Set<ClusterTemplateResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val clusterTemplates = clusterTemplateService!!.retrievePrivateClusterTemplates(user)
        return toJsonList(clusterTemplates)
    }

    override fun getPrivate(name: String): ClusterTemplateResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val clusterTemplate = clusterTemplateService!!.getPrivateClusterTemplate(name, user)
        return conversionService!!.convert<ClusterTemplateResponse>(clusterTemplate, ClusterTemplateResponse::class.java)
    }

    override fun getPublic(name: String): ClusterTemplateResponse {
        val user = authenticatedUserService!!.cbUser
        val clusterTemplate = clusterTemplateService!!.getPublicClusterTemplate(name, user)
        return conversionService!!.convert<ClusterTemplateResponse>(clusterTemplate, ClusterTemplateResponse::class.java)
    }

    override fun getPublics(): Set<ClusterTemplateResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val clusterTemplates = clusterTemplateService!!.retrieveAccountClusterTemplates(user)
        return toJsonList(clusterTemplates)
    }

    override fun get(id: Long?): ClusterTemplateResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val clusterTemplate = clusterTemplateService!!.get(id)
        return conversionService!!.convert<ClusterTemplateResponse>(clusterTemplate, ClusterTemplateResponse::class.java)
    }

    override fun delete(id: Long?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        clusterTemplateService!!.delete(id, user)
    }

    override fun deletePublic(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        clusterTemplateService!!.delete(name, user)
    }

    override fun deletePrivate(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        clusterTemplateService!!.delete(name, user)
    }

    private fun createClusterTemplateRequest(user: CbUser, clusterTemplateRequest: ClusterTemplateRequest, publicInAccount: Boolean): IdJson {
        var clusterTemplate = conversionService!!.convert<ClusterTemplate>(clusterTemplateRequest, ClusterTemplate::class.java)
        clusterTemplate.isPublicInAccount = publicInAccount
        clusterTemplate = clusterTemplateService!!.create(user, clusterTemplate)
        return IdJson(clusterTemplate.id)
    }

    private fun toJsonList(clusterTemplates: Set<ClusterTemplate>): Set<ClusterTemplateResponse> {
        return conversionService!!.convert(clusterTemplates,
                TypeDescriptor.forObject(clusterTemplates),
                TypeDescriptor.collection(Set<Any>::class.java, TypeDescriptor.valueOf(ClusterTemplateResponse::class.java))) as Set<ClusterTemplateResponse>
    }
}
