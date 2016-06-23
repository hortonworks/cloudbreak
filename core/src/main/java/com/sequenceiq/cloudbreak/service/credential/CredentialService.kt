package com.sequenceiq.cloudbreak.service.credential

import java.util.UUID

import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.common.type.APIResourceType
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.CredentialRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException
import com.sequenceiq.cloudbreak.service.notification.NotificationSender
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter

@Service
@Transactional
class CredentialService {

    @Inject
    private val credentialRepository: CredentialRepository? = null

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val credentialAdapter: ServiceProviderCredentialAdapter? = null

    @Inject
    private val notificationSender: NotificationSender? = null

    fun retrievePrivateCredentials(user: CbUser): Set<Credential> {
        return credentialRepository!!.findForUser(user.userId)
    }

    fun retrieveAccountCredentials(user: CbUser): Set<Credential> {
        if (user.roles.contains(CbUserRole.ADMIN)) {
            return credentialRepository!!.findAllInAccount(user.account)
        } else {
            return credentialRepository!!.findPublicInAccountForUser(user.userId, user.account)
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): Credential? {
        val credential = credentialRepository!!.findOne(id)
        if (credential == null) {
            throw NotFoundException(String.format("Credential '%s' not found.", id))
        } else {
            return credential
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    fun create(user: CbUser, credential: Credential): Credential {
        var credential = credential
        LOGGER.debug("Creating credential: [User: '{}', Account: '{}']", user.username, user.account)
        credential.owner = user.userId
        credential.account = user.account
        credential = credentialAdapter!!.init(credential)
        val savedCredential: Credential
        try {
            savedCredential = credentialRepository!!.save(credential)
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateKeyValueException(APIResourceType.CREDENTIAL, credential.name, ex)
        }

        return savedCredential
    }

    fun getPublicCredential(name: String, user: CbUser): Credential {
        val credential = credentialRepository!!.findOneByName(name, user.account)
        if (credential == null) {
            throw NotFoundException(String.format("Credential '%s' not found.", name))
        } else {
            return credential
        }
    }

    fun getPrivateCredential(name: String, user: CbUser): Credential {
        val credential = credentialRepository!!.findByNameInUser(name, user.userId)
        if (credential == null) {
            throw NotFoundException(String.format("Credential '%s' not found.", name))
        } else {
            return credential
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    fun delete(id: Long?, user: CbUser) {
        val credential = credentialRepository!!.findByIdInAccount(id, user.account) ?: throw NotFoundException(String.format("Credential '%s' not found.", id))
        delete(credential, user)
    }

    @Transactional(Transactional.TxType.NEVER)
    fun delete(name: String, user: CbUser) {
        val credential = credentialRepository!!.findByNameInAccount(name, user.account, user.userId) ?: throw NotFoundException(String.format("Credential '%s' not found.", name))
        delete(credential, user)
    }

    @Transactional(Transactional.TxType.NEVER)
    @Throws(Exception::class)
    fun update(id: Long?): Credential {
        val credential = get(id)
        if (credential == null) {
            throw NotFoundException(String.format("Credential '%s' not found.", id))
        } else {
            return credentialAdapter!!.update(credential)
        }
    }

    private fun delete(credential: Credential, user: CbUser) {
        if (user.userId != credential.owner && !user.roles.contains(CbUserRole.ADMIN)) {
            throw BadRequestException("Credentials can be deleted only by account admins or owners.")
        }
        val stacks = stackRepository!!.findByCredential(credential.id)
        if (stacks.isEmpty()) {
            archiveCredential(credential)
        } else {
            throw BadRequestException(String.format("Credential '%d' is in use, cannot be deleted.", credential.id))
        }
    }

    private fun generateArchiveName(name: String): String {
        //generate new name for the archived credential to by pass unique constraint
        return StringBuilder().append(name).append("_").append(UUID.randomUUID()).toString()
    }

    private fun archiveCredential(credential: Credential) {
        credential.name = generateArchiveName(credential.name)
        credential.isArchived = true
        credential.topology = null
        credentialRepository!!.save(credential)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(CredentialService::class.java)
    }
}
