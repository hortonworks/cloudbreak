package com.sequenceiq.cloudbreak.controller

import java.util.HashSet

import javax.annotation.Resource

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.CredentialEndpoint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.api.model.CredentialRequest
import com.sequenceiq.cloudbreak.api.model.CredentialResponse
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.service.credential.CredentialService

@Component
class CredentialController : CredentialEndpoint {

    @Resource
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Autowired
    private val credentialService: CredentialService? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    override fun postPrivate(credentialRequest: CredentialRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createCredential(user, credentialRequest, false)
    }

    override fun postPublic(credentialRequest: CredentialRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createCredential(user, credentialRequest, true)
    }

    override fun getPrivates(): Set<CredentialResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val credentials = credentialService!!.retrievePrivateCredentials(user)
        return convertCredentials(credentials)
    }

    override fun getPublics(): Set<CredentialResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val credentials = credentialService!!.retrieveAccountCredentials(user)
        return convertCredentials(credentials)
    }

    override fun getPrivate(name: String): CredentialResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val credentials = credentialService!!.getPrivateCredential(name, user)
        return convert(credentials)
    }

    override fun getPublic(name: String): CredentialResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val credentials = credentialService!!.getPublicCredential(name, user)
        return convert(credentials)
    }

    override fun get(id: Long?): CredentialResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val credential = credentialService!!.get(id)
        return convert(credential)
    }

    override fun delete(id: Long?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        credentialService!!.delete(id, user)
    }

    override fun deletePublic(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        credentialService!!.delete(name, user)
    }

    override fun deletePrivate(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        credentialService!!.delete(name, user)
    }

    private fun createCredential(user: CbUser, credentialRequest: CredentialRequest, publicInAccount: Boolean): IdJson {
        var credential = convert(credentialRequest, publicInAccount)
        credential = credentialService!!.create(user, credential)
        return IdJson(credential.id)
    }

    private fun convert(json: CredentialRequest, publicInAccount: Boolean): Credential {
        val converted = conversionService!!.convert<Credential>(json, Credential::class.java)
        converted.isPublicInAccount = publicInAccount
        return converted
    }

    private fun convert(credential: Credential): CredentialResponse {
        return conversionService!!.convert<CredentialResponse>(credential, CredentialResponse::class.java)
    }

    private fun convertCredentials(credentials: Set<Credential>): Set<CredentialResponse> {
        val jsonSet = HashSet<CredentialResponse>()
        for (credential in credentials) {
            jsonSet.add(convert(credential))
        }
        return jsonSet
    }
}
