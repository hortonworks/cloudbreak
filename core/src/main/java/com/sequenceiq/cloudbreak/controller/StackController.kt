package com.sequenceiq.cloudbreak.controller

import javax.ws.rs.core.Response

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson
import com.sequenceiq.cloudbreak.api.model.CertificateResponse
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson
import com.sequenceiq.cloudbreak.api.model.StackRequest
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants
import com.sequenceiq.cloudbreak.common.type.CloudConstants
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator
import com.sequenceiq.cloudbreak.controller.validation.stack.StackValidator
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.StackValidation
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidationFailed
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidator
import com.sequenceiq.cloudbreak.service.decorator.Decorator
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService
import com.sequenceiq.cloudbreak.service.stack.StackService

@Component
class StackController : StackEndpoint {

    @Autowired
    private val stackService: StackService? = null

    @Autowired
    private val tlsSecurityService: TlsSecurityService? = null

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Autowired
    private val stackDecorator: Decorator<Stack>? = null

    @Autowired
    private val accountPreferencesValidator: AccountPreferencesValidator? = null

    @Autowired
    private val parameterService: CloudParameterService? = null

    @Autowired
    private val fileSystemValidator: FileSystemValidator? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    @Autowired
    private val stackValidator: StackValidator? = null

    override fun postPrivate(stackRequest: StackRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createStack(user, stackRequest, false)
    }

    override fun postPublic(stackRequest: StackRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createStack(user, stackRequest, true)
    }

    override fun getPrivates(): Set<StackResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return stackService!!.retrievePrivateStacks(user)
    }

    override fun getPublics(): Set<StackResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return stackService!!.retrieveAccountStacks(user)
    }

    override fun get(id: Long?): StackResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return stackService!!.getJsonById(id)
    }

    override fun getPrivate(name: String): StackResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return stackService!!.getPrivateStackJsonByName(name, user)
    }

    override fun getPublic(name: String): StackResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return stackService!!.getPublicStackJsonByName(name, user)
    }

    override fun status(id: Long?): Map<String, Any> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return conversionService!!.convert<Map<Any, Any>>(stackService!!.get(id), Map<Any, Any>::class.java)
    }

    override fun delete(id: Long?, forced: Boolean?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        if (forced!!) {
            stackService!!.forceDelete(id, user)
        } else {
            stackService!!.delete(id, user)
        }
    }

    override fun deletePrivate(name: String, forced: Boolean?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        if (forced!!) {
            stackService!!.forceDelete(name, user)
        } else {
            stackService!!.delete(name, user)
        }
    }

    override fun deletePublic(name: String, forced: Boolean?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        if (forced!!) {
            stackService!!.forceDelete(name, user)
        } else {
            stackService!!.delete(name, user)
        }
    }

    override fun put(id: Long?, updateRequest: UpdateStackJson): Response {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val stack = stackService!!.getById(id)
        MDCBuilder.buildMdcContext(stack)
        if (CloudConstants.BYOS == stack.cloudPlatform()) {
            LOGGER.warn("A 'Bring your own stack' type of infrastructure cannot be modified.")
            return Response.status(Response.Status.BAD_REQUEST).build()
        } else {
            if (updateRequest.status != null) {
                stackService.updateStatus(id, updateRequest.status)
            } else {
                val scalingAdjustment = updateRequest.instanceGroupAdjustment.scalingAdjustment
                validateAccountPreferences(id, scalingAdjustment)
                stackService.updateNodeCount(id, updateRequest.instanceGroupAdjustment)
            }
        }
        return Response.status(Response.Status.ACCEPTED).build()
    }

    override fun getCertificate(stackId: Long?): CertificateResponse {
        return CertificateResponse(tlsSecurityService!!.getCertificate(stackId))
    }

    override fun getStackForAmbari(json: AmbariAddressJson): StackResponse {
        return stackService!!.get(json.ambariAddress)
    }

    override fun validate(request: StackValidationRequest): Response {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val stackValidation = conversionService!!.convert<StackValidation>(request, StackValidation::class.java)
        stackService!!.validateStack(stackValidation)
        fileSystemValidator!!.validateFileSystem(request.platform, request.fileSystem)
        return Response.status(Response.Status.ACCEPTED).build()
    }

    override fun deleteInstance(stackId: Long?, instanceId: String): Response {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildMdcContext(user)
        stackService!!.removeInstance(user, stackId, instanceId)
        return Response.status(Response.Status.ACCEPTED).build()
    }

    override fun variants(): PlatformVariantsJson {
        val pv = parameterService!!.platformVariants
        return conversionService!!.convert<PlatformVariantsJson>(pv, PlatformVariantsJson::class.java)
    }

    private fun createStack(user: CbUser, stackRequest: StackRequest, publicInAccount: Boolean): IdJson {
        stackValidator!!.validate(stackRequest)
        var stack = conversionService!!.convert<Stack>(stackRequest, Stack::class.java)
        MDCBuilder.buildMdcContext(stack)
        stack = stackDecorator!!.decorate(stack, stackRequest.credentialId, stackRequest.networkId, stackRequest.securityGroupId)
        stack.isPublicInAccount = publicInAccount
        validateAccountPreferences(stack, user)
        if (stack.orchestrator != null && stack.orchestrator.apiEndpoint != null) {
            stackService!!.validateOrchestrator(stack.orchestrator)
        }
        stack = stackService!!.create(user, stack, stackRequest.ambariVersion, stackRequest.hdpVersion)
        return IdJson(stack.id)
    }

    private fun validateAccountPreferences(stack: Stack, user: CbUser) {
        try {
            accountPreferencesValidator!!.validate(stack, user.account, user.userId)
        } catch (e: AccountPreferencesValidationFailed) {
            throw BadRequestException(e.message, e)
        }

    }

    private fun validateAccountPreferences(stackId: Long?, scalingAdjustment: Int?) {
        try {
            accountPreferencesValidator!!.validate(stackId, scalingAdjustment)
        } catch (e: AccountPreferencesValidationFailed) {
            throw BadRequestException(e.message, e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(StackController::class.java)
    }
}
