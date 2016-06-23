package com.sequenceiq.cloudbreak.controller

import java.util.HashSet

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PathVariable

import com.sequenceiq.cloudbreak.api.endpoint.NetworkEndpoint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Network
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.NetworkJson
import com.sequenceiq.cloudbreak.service.network.DefaultNetworkCreator
import com.sequenceiq.cloudbreak.service.network.NetworkService

@Component
class NetworkController : NetworkEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Autowired
    private val networkService: NetworkService? = null

    @Autowired
    private val networkCreator: DefaultNetworkCreator? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    override fun postPrivate(networkJson: NetworkJson): IdJson {
        val user = authenticatedUserService!!.cbUser
        return createNetwork(user, networkJson, false)
    }

    override fun postPublic(networkJson: NetworkJson): IdJson {
        val user = authenticatedUserService!!.cbUser
        return createNetwork(user, networkJson, true)
    }

    override fun getPrivates(): Set<NetworkJson> {
        val user = authenticatedUserService!!.cbUser
        networkCreator!!.createDefaultNetworks(user)
        val networks = networkService!!.retrievePrivateNetworks(user)
        return convert(networks)
    }

    override fun getPublics(): Set<NetworkJson> {
        val user = authenticatedUserService!!.cbUser
        networkCreator!!.createDefaultNetworks(user)
        val networks = networkService!!.retrieveAccountNetworks(user)
        return convert(networks)
    }

    override fun get(id: Long?): NetworkJson {
        val network = networkService!!.getById(id)
        return convert(network)
    }

    override fun getPrivate(name: String): NetworkJson {
        val user = authenticatedUserService!!.cbUser
        val network = networkService!!.getPrivateNetwork(name, user)
        return convert(network)
    }

    override fun getPublic(name: String): NetworkJson {
        val user = authenticatedUserService!!.cbUser
        val network = networkService!!.getPublicNetwork(name, user)
        return convert(network)
    }

    override fun delete(id: Long?) {
        val user = authenticatedUserService!!.cbUser
        networkService!!.delete(id, user)
    }

    override fun deletePublic(name: String) {
        val user = authenticatedUserService!!.cbUser
        networkService!!.delete(name, user)
    }

    override fun deletePrivate(@PathVariable name: String) {
        val user = authenticatedUserService!!.cbUser
        networkService!!.delete(name, user)
    }

    private fun createNetwork(user: CbUser, networkRequest: NetworkJson, publicInAccount: Boolean): IdJson {
        var network = convert(networkRequest, publicInAccount)
        network = networkService!!.create(user, network)
        return IdJson(network.id)
    }

    private fun convert(networkRequest: NetworkJson, publicInAccount: Boolean): Network {
        val network = conversionService!!.convert<Network>(networkRequest, Network::class.java)
        network.isPublicInAccount = publicInAccount
        return network
    }

    private fun convert(network: Network): NetworkJson {
        return conversionService!!.convert<NetworkJson>(network, NetworkJson::class.java)
    }

    private fun convert(networks: Set<Network>): Set<NetworkJson> {
        val jsons = HashSet<NetworkJson>()
        for (network in networks) {
            jsons.add(convert(network))
        }
        return jsons
    }
}
