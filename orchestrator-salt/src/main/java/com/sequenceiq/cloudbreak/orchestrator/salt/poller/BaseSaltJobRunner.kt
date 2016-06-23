package com.sequenceiq.cloudbreak.orchestrator.salt.poller

import java.util.HashSet
import java.util.Scanner
import java.util.stream.Collectors

import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType

abstract class BaseSaltJobRunner(target: Set<String>, private val allNode: Set<Node>) : SaltJobRunner {

    override var target: Set<String> = HashSet()
    override var jid: JobId? = null
    override var jobState = JobState.NOT_STARTED

    init {
        this.target = target
    }

    override fun stateType(): StateType {
        return StateType.SIMPLE
    }

    fun collectNodes(applyResponse: ApplyResponse): Set<String> {
        val set = HashSet<String>()
        for (stringObjectMap in applyResponse.result) {
            set.addAll(stringObjectMap.entries.stream().map(Function<Entry<String, Any>, String> { it.key }).collect(Collectors.toList<String>()))
        }
        return set
    }

    fun collectMissingNodes(nodes: Set<String>): Set<String> {
        val hostNames = allNode.stream().collect(Collectors.toMap<Node, String, String>({ node -> getShortHostName(node.hostname) }, Function<Node, String> { it.getPrivateIp() }))
        val nodesTarget = nodes.stream().map({ node -> hostNames.get(getShortHostName(node)) }).collect(Collectors.toSet<String>())
        return target.stream().filter({ t -> !nodesTarget.contains(t) }).collect(Collectors.toSet<String>())
    }

    private fun getShortHostName(hostName: String): String {
        return Scanner(hostName).useDelimiter("\\.").next()
    }

    override fun toString(): String {
        return "BaseSaltJobRunner{"
        +"target=" + target
        +", jid=" + jid
        +", jobState=" + jobState
        +'}'
    }
}
