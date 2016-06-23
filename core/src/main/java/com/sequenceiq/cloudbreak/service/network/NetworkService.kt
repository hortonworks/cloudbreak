package com.sequenceiq.cloudbreak.service.network

import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.common.type.APIResourceType
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.domain.Network
import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.repository.NetworkRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@Service
@Transactional
class NetworkService {

    @Inject
    private val networkRepository: NetworkRepository? = null

    @Inject
    private val stackRepository: StackRepository? = null

    @Transactional(Transactional.TxType.NEVER)
    fun create(user: CbUser, network: Network): Network {
        LOGGER.info("Creating network: [User: '{}', Account: '{}']", user.username, user.account)
        network.owner = user.userId
        network.account = user.account
        try {
            return networkRepository!!.save(network)
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateKeyValueException(APIResourceType.NETWORK, network.name, ex)
        }

    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): Network? {
        val network = networkRepository!!.findOne(id) ?: throw NotFoundException(String.format("Network '%s' not found", id))
        return network
    }

    fun getById(id: Long?): Network {
        val network = networkRepository!!.findOneById(id) ?: throw NotFoundException(String.format("Network '%s' not found", id))
        return network
    }

    fun getPrivateNetwork(name: String, user: CbUser): Network {
        val network = networkRepository!!.findByNameForUser(name, user.userId) ?: throw NotFoundException(String.format("Network '%s' not found", name))
        return network
    }

    fun getPublicNetwork(name: String, user: CbUser): Network {
        val network = networkRepository!!.findByNameInAccount(name, user.account) ?: throw NotFoundException(String.format("Network '%s' not found", name))
        return network
    }

    @Transactional(Transactional.TxType.NEVER)
    fun delete(id: Long?, user: CbUser) {
        LOGGER.info("Deleting network with id: {}", id)
        val network = get(id) ?: throw NotFoundException(String.format("Network '%s' not found.", id))

        delete(user, network)
    }

    fun delete(name: String, user: CbUser) {
        LOGGER.info("Deleting network with name: {}", name)
        val network = networkRepository!!.findByNameInAccount(name, user.account) ?: throw NotFoundException(String.format("Network '%s' not found.", name))

        delete(user, network)
    }

    fun retrievePrivateNetworks(user: CbUser): Set<Network> {
        return networkRepository!!.findForUser(user.userId)
    }

    fun retrieveAccountNetworks(user: CbUser): Set<Network> {
        if (user.roles.contains(CbUserRole.ADMIN)) {
            return networkRepository!!.findAllInAccount(user.account)
        } else {
            return networkRepository!!.findPublicInAccountForUser(user.userId, user.account)
        }
    }

    private fun delete(user: CbUser, network: Network) {
        if (stackRepository!!.findAllByNetwork(network.id).isEmpty()) {
            if (user.userId != network.owner && !user.roles.contains(CbUserRole.ADMIN)) {
                throw BadRequestException("Public networks can only be deleted by owners or account admins.")
            } else {
                if (ResourceStatus.USER_MANAGED == network.status) {
                    networkRepository!!.delete(network)
                } else {
                    network.status = ResourceStatus.DEFAULT_DELETED
                    networkRepository!!.save(network)
                }
            }
        } else {
            throw BadRequestException(String.format(
                    "There are clusters associated with network '%s'(ID:'%s'). Please remove these before deleting the network.",
                    network.name, network.id))
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(NetworkService::class.java)
    }
}
