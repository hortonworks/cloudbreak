package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import java.util.Collections.singletonMap

import java.util.HashMap
import java.util.stream.Collectors

import org.springframework.util.StringUtils

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar

class PillarSave : OrchestratorBootstrap {

    private val sc: SaltConnector
    private val pillar: Pillar

    constructor(sc: SaltConnector, gateway: String) {
        this.sc = sc
        this.pillar = Pillar("/ambari/server.sls", singletonMap("ambari", singletonMap("server", gateway)))
    }

    constructor(sc: SaltConnector, hosts: Set<Node>) {
        this.sc = sc
        val fqdn = hosts.stream().collect(Collectors.toMap<Node, String, Map<String, Any>>(Function<Node, String> { it.getPrivateIp() }) { node -> discovery(node.hostname, node.publicIp) })
        this.pillar = Pillar("/nodes/hosts.sls", singletonMap<String, Map<String, Map<String, Any>>>("hosts", fqdn))
    }

    constructor(sc: SaltConnector, recipes: Map<String, List<RecipeModel>>) {
        this.sc = sc
        val scripts = HashMap<String, Map<String, List<String>>>()
        for (hostGroup in recipes.keys) {
            val pre = recipes[hostGroup].stream().filter({ h -> h.preInstall != null }).map(Function<RecipeModel, String> { it.getName() }).collect(Collectors.toList<String>())
            val post = recipes[hostGroup].stream().filter({ h -> h.postInstall != null }).map(Function<RecipeModel, String> { it.getName() }).collect(Collectors.toList<String>())
            val prePostScripts = HashMap<String, List<String>>()
            prePostScripts.put("pre", pre)
            prePostScripts.put("post", post)
            scripts.put(hostGroup, prePostScripts)
        }
        this.pillar = Pillar("/recipes/init.sls", singletonMap<String, Map<String, Map<String, List<String>>>>("recipes", scripts))
    }

    constructor(sc: SaltConnector, pillarProperties: SaltPillarProperties) {
        this.sc = sc
        this.pillar = Pillar(pillarProperties.path, pillarProperties.properties)
    }

    private fun discovery(hostname: String, publicAddress: String): Map<String, Any> {
        val map = HashMap<String, Any>()
        map.put("fqdn", hostname)
        map.put("hostname", hostname.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0])
        map.put("public_address", if (StringUtils.isEmpty(publicAddress)) java.lang.Boolean.FALSE else java.lang.Boolean.TRUE)
        return map
    }

    @Throws(Exception::class)
    override fun call(): Boolean? {
        val resp = sc.pillar(pillar)
        resp.assertError()
        return true
    }
}
