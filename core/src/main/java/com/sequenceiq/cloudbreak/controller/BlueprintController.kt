package com.sequenceiq.cloudbreak.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.BlueprintEndpoint
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintLoaderService
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService

@Component
class BlueprintController : BlueprintEndpoint {

    @Autowired
    private val blueprintService: BlueprintService? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Autowired
    private val blueprintLoaderService: BlueprintLoaderService? = null

    override fun postPrivate(blueprintRequest: BlueprintRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createBlueprint(user, blueprintRequest, false)
    }

    override fun postPublic(blueprintRequest: BlueprintRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createBlueprint(user, blueprintRequest, true)
    }

    override fun getPrivates(): Set<BlueprintResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        var blueprints = blueprintService!!.retrievePrivateBlueprints(user)
        if (blueprints.isEmpty()) {
            blueprints = blueprintLoaderService!!.loadBlueprints(user)
        }
        return toJsonList(blueprints)
    }

    override fun getPrivate(name: String): BlueprintResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val blueprint = blueprintService!!.getPrivateBlueprint(name, user)
        return conversionService!!.convert<BlueprintResponse>(blueprint, BlueprintResponse::class.java)
    }

    override fun getPublic(name: String): BlueprintResponse {
        val user = authenticatedUserService!!.cbUser
        val blueprint = blueprintService!!.getPublicBlueprint(name, user)
        return conversionService!!.convert<BlueprintResponse>(blueprint, BlueprintResponse::class.java)
    }

    override fun getPublics(): Set<BlueprintResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        blueprintLoaderService!!.loadBlueprints(user)
        val blueprints = blueprintService!!.retrieveAccountBlueprints(user)
        return toJsonList(blueprints)
    }

    override fun get(id: Long?): BlueprintResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val blueprint = blueprintService!!.get(id)
        return conversionService!!.convert<BlueprintResponse>(blueprint, BlueprintResponse::class.java)
    }

    override fun delete(id: Long?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        blueprintService!!.delete(id, user)
    }

    override fun deletePublic(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        blueprintService!!.delete(name, user)
    }

    override fun deletePrivate(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        blueprintService!!.delete(name, user)
    }

    private fun createBlueprint(user: CbUser, blueprintRequest: BlueprintRequest, publicInAccount: Boolean): IdJson {
        var blueprint = conversionService!!.convert<Blueprint>(blueprintRequest, Blueprint::class.java)
        blueprint.isPublicInAccount = publicInAccount
        blueprint = blueprintService!!.create(user, blueprint)
        return IdJson(blueprint.id)
    }

    private fun toJsonList(blueprints: Set<Blueprint>): Set<BlueprintResponse> {
        return conversionService!!.convert(blueprints,
                TypeDescriptor.forObject(blueprints),
                TypeDescriptor.collection(Set<Any>::class.java, TypeDescriptor.valueOf(BlueprintResponse::class.java))) as Set<BlueprintResponse>
    }

}
