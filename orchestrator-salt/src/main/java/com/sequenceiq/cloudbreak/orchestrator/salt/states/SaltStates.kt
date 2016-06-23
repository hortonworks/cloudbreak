package com.sequenceiq.cloudbreak.orchestrator.salt.states

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.RUNNER
import java.util.stream.Collectors

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfoObject
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType

object SaltStates {

    private val LOGGER = LoggerFactory.getLogger(SaltStates::class.java)

    fun ping(sc: SaltConnector, target: Target<String>): PingResponse {
        return sc.run<PingResponse>(target, "test.ping", LOCAL, PingResponse::class.java)
    }

    fun ambariServer(sc: SaltConnector, target: Target<String>): String {
        return applyState(sc, "ambari.server", target).jid
    }

    fun ambariAgent(sc: SaltConnector, target: Target<String>): String {
        return applyState(sc, "ambari.agent", target).jid
    }

    fun ambariReset(sc: SaltConnector, target: Target<String>): String {
        return applyState(sc, "ambari.reset", target).jid
    }

    fun kerberos(sc: SaltConnector, target: Target<String>): String {
        return applyState(sc, "kerberos.server", target).jid
    }

    fun addGrain(sc: SaltConnector, target: Target<String>, key: String, value: String): ApplyResponse {
        return sc.run<ApplyResponse>(target, "grains.append", LOCAL, ApplyResponse::class.java, key, value)
    }

    fun removeGrain(sc: SaltConnector, target: Target<String>, key: String, value: String): ApplyResponse {
        return sc.run<ApplyResponse>(target, "grains.remove", LOCAL, ApplyResponse::class.java, key, value)
    }

    fun syncGrains(sc: SaltConnector, target: Target<String>): ApplyResponse {
        return sc.run<ApplyResponse>(Glob.ALL, "saltutil.sync_grains", LOCAL, ApplyResponse::class.java)
    }

    fun highstate(sc: SaltConnector): String {
        return sc.run<ApplyResponse>(Glob.ALL, "state.highstate", LOCAL_ASYNC, ApplyResponse::class.java).jid
    }

    fun consul(sc: SaltConnector, target: Target<String>): String {
        return applyState(sc, "consul", target).jid
    }

    fun jidInfo(sc: SaltConnector, jid: String, target: Target<String>, stateType: StateType): Multimap<String, String> {
        if (StateType.HIGH == stateType) {
            return highStateJidInfo(sc, jid, target)
        } else if (StateType.SIMPLE == stateType) {
            return applyStateJidInfo(sc, jid, target)
        }
        return ArrayListMultimap.create<String, String>()
    }

    private fun applyStateJidInfo(sc: SaltConnector, jid: String, target: Target<String>): Multimap<String, String> {
        val jidInfo = sc.run<Map<Any, Any>>(target, "jobs.lookup_jid", RUNNER, Map<Any, Any>::class.java, "jid", jid)
        LOGGER.info("Salt apply state jid info: {}", jidInfo)
        val states = JidInfoResponseTransformer.getSimpleStates(jidInfo)
        return collectMissingTargets(states)
    }

    private fun highStateJidInfo(sc: SaltConnector, jid: String, target: Target<String>): Multimap<String, String> {
        val jidInfo = sc.run<Map<Any, Any>>(target, "jobs.lookup_jid", RUNNER, Map<Any, Any>::class.java, "jid", jid)
        LOGGER.info("Salt high state jid info: {}", jidInfo)
        val states = JidInfoResponseTransformer.getHighStates(jidInfo)
        return collectMissingTargets(states)
    }

    private fun collectMissingTargets(stringRunnerInfoObjectMap: Map<String, Map<String, RunnerInfoObject>>): Multimap<String, String> {
        val missingTargetsWithErrors = ArrayListMultimap.create<String, String>()
        for (stringMapEntry in stringRunnerInfoObjectMap.entries) {
            LOGGER.info("Collect missing targets from host: {}", stringMapEntry.key)
            logRunnerInfos(stringMapEntry)
            for (targetObject in stringMapEntry.value.entries) {
                if (targetObject.value.result) {
                    LOGGER.info("{} finished in {} ms.", targetObject.value.comment, targetObject.value.duration)
                } else {
                    LOGGER.info("{} job state is {}.", targetObject.value.comment, targetObject.value.result)
                    missingTargetsWithErrors.put(stringMapEntry.key, targetObject.value.comment)
                }
            }
        }
        return missingTargetsWithErrors
    }

    private fun logRunnerInfos(stringMapEntry: Entry<String, Map<String, RunnerInfoObject>>) {
        stringMapEntry.value.entries.stream().sorted({ o1, o2 -> Integer.compare(o1.value.runNum!!, o2.value.runNum!!) }).forEach({ stringRunnerInfoObjectEntry -> LOGGER.info("Runner info: {} --- {}", stringRunnerInfoObjectEntry.key, stringRunnerInfoObjectEntry.value.comment) })
    }

    fun jobIsRunning(sc: SaltConnector, jid: String, target: Target<String>): Boolean {
        val runningInfo = sc.run<RunningJobsResponse>(target, "jobs.active", RUNNER, RunningJobsResponse::class.java, "jid", jid)
        LOGGER.info("Active salt jobs: {}", runningInfo)
        for (results in runningInfo.result) {
            for (stringMapEntry in results.entries) {
                if (stringMapEntry.key == jid) {
                    return true
                }
            }
        }
        return false
    }

    fun networkInterfaceIP(sc: SaltConnector, target: Target<String>): NetworkInterfaceResponse {
        return sc.run<NetworkInterfaceResponse>(target, "network.interface_ip", LOCAL, NetworkInterfaceResponse::class.java, "eth0")
    }

    fun removeMinions(sc: SaltConnector, hostnames: List<String>): Any {
        // This is slow
        // String targetIps = "S@" + hostnames.stream().collect(Collectors.joining(" or S@"));
        //Map<String, String> ipToMinionId = SaltStates.networkInterfaceIP(sc, new Compound(targetIps)).getResultGroupByHost();

        val saltHostnames = SaltStates.networkInterfaceIP(sc, Glob.ALL).resultGroupByHost
        val hostnamesWithoutDomain = hostnames.stream().map({ host -> host.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0] }).collect(Collectors.toList<String>())
        val minionIds = saltHostnames.entries.stream().filter({ entry -> hostnamesWithoutDomain.contains(entry.key.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]) }).map(Function<Entry<String, String>, String> { it.key }).collect(Collectors.toList<String>())
        val saltAction = SaltAction(SaltActionType.STOP)
        for (hostname in minionIds) {
            val minion = Minion()
            minion.address = saltHostnames[hostname]
            saltAction.addMinion(minion)
        }
        sc.action(saltAction)

        return sc.wheel<Any>("key.delete", minionIds, Any::class.java)
    }

    private fun applyState(sc: SaltConnector, service: String, target: Target<String>): ApplyResponse {
        return sc.run<ApplyResponse>(target, "state.apply", LOCAL_ASYNC, ApplyResponse::class.java, service)
    }

    fun resolveHostNameToMinionHostName(sc: SaltConnector, minionName: String): String {
        val saltHostnames = SaltStates.networkInterfaceIP(sc, Glob.ALL).resultGroupByHost
        return saltHostnames[minionName]
    }

}
