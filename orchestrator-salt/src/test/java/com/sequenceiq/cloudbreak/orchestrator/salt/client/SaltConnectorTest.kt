package com.sequenceiq.cloudbreak.orchestrator.salt.client

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.stream.Collectors

import org.junit.Assert
import org.junit.Before

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates

class SaltConnectorTest {

    private var client: SaltConnector? = null

    @Before
    fun setup() {
        val id = "183"
        val gatewayConfig = GatewayConfig("172.16.252.43", "10.0.0.5", "host-172-16-252-43", 9443,
                "/Users/rdoktorics/prj/certs/stack-" + id,
                "/Users/rdoktorics/prj/certs/stack-$id/ca.pem",
                "/Users/rdoktorics/prj/certs/stack-$id/cert.pem",
                "/Users/rdoktorics/prj/certs/stack-$id/key.pem")
        client = SaltConnector(gatewayConfig, true)
    }

    //@Test
    fun testJid() {
        val pingResponse = SaltStates.jidInfo(client, "20160510134036754632", Glob.ALL, StateType.SIMPLE)
    }

    //@Test
    fun testConsul() {
        val pingResponse = SaltStates.consul(client, Glob.ALL)
    }

    //@Test
    fun testHealth() {
        client!!.health()
    }

    //@Test
    fun testPillar() {
        val consulServers = Arrays.asList("10.0.0.3", "10.0.0.2", "10.0.0.5")
        val conf = HashMap<String, Any>()
        val consul = HashMap()
        consul.put("bootstrap_expect", consulServers.size)
        consul.put("retry_join", consulServers)
        conf.put("consul", consul)
        val pillar = Pillar("/consul/init.sls", conf)
        client!!.pillar(pillar)
    }

    //@Test
    fun testStart() {

        val gatewayPrivateIp = "10.0.0.5"
        val targets = Arrays.asList("10.0.0.3", "10.0.0.2", "10.0.0.5")
        val saltAction = SaltAction(SaltActionType.RUN)

        if (targets.contains(gatewayPrivateIp)) {
            saltAction.server = gatewayPrivateIp
            var roles: MutableList<String> = ArrayList()
            roles.add("ambari_server")
            roles = appendConsulRole(targets, gatewayPrivateIp, roles)
            saltAction.addMinion(createMinion(gatewayPrivateIp, gatewayPrivateIp, roles))
        }
        for (minionIp in targets) {
            if (minionIp != gatewayPrivateIp) {
                var roles: MutableList<String> = ArrayList()
                roles.add("ambari_agent")
                roles = appendConsulRole(targets, gatewayPrivateIp, roles)
                saltAction.addMinion(createMinion(gatewayPrivateIp, minionIp, roles))
            }
        }

        client!!.action(saltAction)
    }

    private fun appendConsulRole(consulServers: List<String>, minionIp: String, roles: MutableList<String>): MutableList<String> {
        if (consulServers.contains(minionIp)) {
            roles.add("consul_server")
        } else {
            roles.add("consul_agent")
        }
        return roles
    }

    private fun createMinion(gatewayPrivateIp: String, address: String, roles: List<String>): Minion {
        val minion = Minion()
        minion.address = address
        minion.roles = roles
        minion.server = gatewayPrivateIp
        return minion
    }


    //@Test
    fun testRunPing() {
        val pingResponse = SaltStates.ping(client, Glob.ALL)
        Assert.assertNotNull(pingResponse.result)
    }

    //@Test
    fun testRunNetIf() {
        val targets = Arrays.asList("10.0.0.5")
        val targetIps = "S@" + targets.stream().collect(Collectors.joining(" or S@"))
        val response = SaltStates.networkInterfaceIP(client, Compound(targetIps))
        Assert.assertNotNull(response.resultGroupByHost)
    }

    //@Test
    fun testRunDeleteMinions() {
        val targets = Arrays.asList("10.0.0.5")
        val `object` = SaltStates.removeMinions(client, targets)
        Assert.assertNotNull(`object`)
    }

    //@Test
    fun testRunConsul() {
        val `object` = SaltStates.consul(client, Glob.ALL)
        Assert.assertNotNull(`object`)
    }

    //@Test
    fun testRunHighstate() {
        val `object` = SaltStates.ambariAgent(client, Glob.ALL)
        Assert.assertNotNull(`object`)
    }

}
