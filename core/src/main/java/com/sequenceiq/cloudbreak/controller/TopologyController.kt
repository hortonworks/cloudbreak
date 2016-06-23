package com.sequenceiq.cloudbreak.controller

import java.util.HashSet

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.TopologyEndpoint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Topology
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.TopologyRequest
import com.sequenceiq.cloudbreak.api.model.TopologyResponse
import com.sequenceiq.cloudbreak.repository.TopologyRepository
import com.sequenceiq.cloudbreak.service.topology.TopologyService

@Component
class TopologyController : TopologyEndpoint {

    @Autowired
    private val topologyService: TopologyService? = null
    @Autowired
    private val topologyRepository: TopologyRepository? = null
    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null
    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    override fun getPublics(): Set<TopologyResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val stacks = topologyRepository!!.findAllInAccount(user.account)

        return convert(stacks)
    }

    private fun convert(topology: Topology): TopologyResponse {
        return conversionService!!.convert<TopologyResponse>(topology, TopologyResponse::class.java)
    }

    private fun convert(topologies: Set<Topology>): Set<TopologyResponse> {
        val jsons = HashSet<TopologyResponse>()
        for (topology in topologies) {
            jsons.add(convert(topology))
        }
        return jsons
    }

    override fun postPublic(topologyRequest: TopologyRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        var topology = conversionService!!.convert<Topology>(topologyRequest, Topology::class.java)
        topology = topologyService!!.create(user, topology)
        return IdJson(topology.id)
    }

    override fun delete(id: Long?, forced: Boolean?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        topologyService!!.delete(id, user)
    }

    override fun get(id: Long?): TopologyResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val topology = topologyService!!.get(id)
        return convert(topology)
    }
}
