package com.sequenceiq.cloudbreak.service.topology

import java.util.Date

import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.common.type.APIResourceType
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.Network
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.domain.Topology
import com.sequenceiq.cloudbreak.repository.CredentialRepository
import com.sequenceiq.cloudbreak.repository.NetworkRepository
import com.sequenceiq.cloudbreak.repository.TemplateRepository
import com.sequenceiq.cloudbreak.repository.TopologyRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@Service
@Transactional
class TopologyService {

    @Inject
    private val topologyRepository: TopologyRepository? = null

    @Inject
    private val credentialRepository: CredentialRepository? = null

    @Inject
    private val templateRepository: TemplateRepository? = null

    @Inject
    private val networkRepository: NetworkRepository? = null

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): Topology {
        val topology = topologyRepository!!.findOne(id)
        if (topology == null) {
            throw NotFoundException(String.format(TOPOLOGY_NOT_FOUND_MSG, id))
        } else {
            return topology
        }
    }

    @Transactional(Transactional.TxType.NEVER)
    fun create(user: CbUser, topology: Topology): Topology {
        LOGGER.debug("Creating topology: [User: '{}', Account: '{}']", user.username, user.account)
        var savedTopology: Topology? = null
        topology.owner = user.userId
        topology.account = user.account
        try {
            savedTopology = topologyRepository!!.save(topology)
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateKeyValueException(APIResourceType.TOPOLOGY, topology.name, ex)
        }

        return savedTopology
    }

    fun delete(topologyId: Long?, user: CbUser) {
        val topology = topologyRepository!!.findByIdInAccount(topologyId, user.account) ?: throw NotFoundException(String.format(TOPOLOGY_NOT_FOUND_MSG, topologyId))
        delete(topology, user)
    }

    private fun delete(topology: Topology, user: CbUser) {
        LOGGER.debug("Deleting topology. {} - {}", *arrayOf(topology.id, topology.name))
        val credentials = credentialRepository!!.findByTopology(topology.id)
        val templates = templateRepository!!.findByTopology(topology.id)
        val networks = networkRepository!!.findByTopology(topology.id)
        if (credentials.isEmpty() && templates.isEmpty() && networks.isEmpty()) {
            val now = Date()
            val terminatedName = topology.name + DELIMITER + now.time
            topology.name = terminatedName
            topology.isDeleted = true
            topologyRepository!!.save(topology)
        } else {
            throw BadRequestException(String.format("Topology '%d' is in use, cannot be deleted.", topology.id))
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TopologyService::class.java)
        private val DELIMITER = "_"
        private val TOPOLOGY_NOT_FOUND_MSG = "Topology '%s' not found."
    }
}
