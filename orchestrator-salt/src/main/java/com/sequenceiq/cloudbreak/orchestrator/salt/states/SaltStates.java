package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.RUNNER;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfoObject;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;

public class SaltStates {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltStates.class);

    private SaltStates() {
    }

    public static PingResponse ping(SaltConnector sc, Target<String> target) {
        return sc.run(target, "test.ping", LOCAL, PingResponse.class);
    }

    public static String ambariServer(SaltConnector sc, Target<String> target) {
        return applyState(sc, "ambari.server", target).getJid();
    }

    public static String ambariAgent(SaltConnector sc, Target<String> target) {
        return applyState(sc, "ambari.agent", target).getJid();
    }

    public static String kerberos(SaltConnector sc, Target<String> target) {
        return applyState(sc, "kerberos.server", target).getJid();
    }

    public static ApplyResponse addRole(SaltConnector sc, Target<String> target, String role) {
        return sc.run(target, "grains.append", LOCAL, ApplyResponse.class, "roles", role);
    }

    public static ApplyResponse syncGrains(SaltConnector sc, Target<String> target) {
        return sc.run(Glob.ALL, "saltutil.sync_grains", LOCAL, ApplyResponse.class);
    }

    public static String highstate(SaltConnector sc) {
        return sc.run(Glob.ALL, "state.highstate", LOCAL_ASYNC, ApplyResponse.class).getJid();
    }

    public static String consul(SaltConnector sc, Target<String> target) {
        return applyState(sc, "consul", target).getJid();
    }

    public static Set<String> jidInfo(SaltConnector sc, String jid, Target<String> target, StateType stateType) {
        if (StateType.HIGH.equals(stateType)) {
            return highStateJidInfo(sc, jid, target);
        } else if (StateType.SIMPLE.equals(stateType)) {
            return applyStateJidInfo(sc, jid, target);
        }
        return new HashSet<>();
    }

    private static Set<String> applyStateJidInfo(SaltConnector sc, String jid, Target<String> target) {
        Map jidInfo = sc.run(target, "jobs.lookup_jid", RUNNER, Map.class, "jid", jid);
        LOGGER.info("Salt apply state jid info: {}", jidInfo);
        Map<String, Map<String, RunnerInfoObject>> states = JidInfoResponseTransformer.getSimpleStates(jidInfo);
        return collectMissingTargets(states);
    }

    private static Set<String> highStateJidInfo(SaltConnector sc, String jid, Target<String> target) {
        Map jidInfo = sc.run(target, "jobs.lookup_jid", RUNNER, Map.class, "jid", jid);
        LOGGER.info("Salt high state jid info: {}", jidInfo);
        Map<String, Map<String, RunnerInfoObject>> states = JidInfoResponseTransformer.getHighStates(jidInfo);
        return collectMissingTargets(states);
    }

    private static Set<String> collectMissingTargets(Map<String, Map<String, RunnerInfoObject>> stringRunnerInfoObjectMap) {
        Set<String> missingTargets = new HashSet<>();
        for (Map.Entry<String, Map<String, RunnerInfoObject>> stringMapEntry : stringRunnerInfoObjectMap.entrySet()) {
            for (Map.Entry<String, RunnerInfoObject> targetObject : stringMapEntry.getValue().entrySet()) {
                if (targetObject.getValue().getResult().toLowerCase().equals("true")) {
                    LOGGER.info("{} finished under {} seconds.", targetObject.getValue().getComment(), targetObject.getValue().getDuration());
                } else {
                    LOGGER.info("{} job state is {}.", targetObject.getValue().getComment(), targetObject.getValue().getResult());
                    missingTargets.add(targetObject.getKey());
                    break;
                }
            }
        }
        return missingTargets;
    }

    public static boolean jobIsRunning(SaltConnector sc, String jid, Target<String> target) {
        RunningJobsResponse runningInfo = sc.run(target, "jobs.active", RUNNER, RunningJobsResponse.class, "jid", jid);
        LOGGER.info("Active salt jobs: {}", runningInfo);
        for (Map<String, Map<String, Object>> results : runningInfo.getResult()) {
            for (Map.Entry<String, Map<String, Object>> stringMapEntry : results.entrySet()) {
                if (stringMapEntry.getKey().equals(jid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static NetworkInterfaceResponse networkInterfaceIP(SaltConnector sc, Target<String> target) {
        return sc.run(target, "network.interface_ip", LOCAL, NetworkInterfaceResponse.class, "eth0");
    }

    public static Object removeMinions(SaltConnector sc, List<String> hostnames) {
        // This is slow
        // String targetIps = "S@" + hostnames.stream().collect(Collectors.joining(" or S@"));
        //Map<String, String> ipToMinionId = SaltStates.networkInterfaceIP(sc, new Compound(targetIps)).getResultGroupByHost();

        Map<String, String> saltHostnames = SaltStates.networkInterfaceIP(sc, Glob.ALL).getResultGroupByHost();
        List<String> hostnamesWithoutDomain = hostnames.stream().map(host -> host.split("\\.")[0]).collect(Collectors.toList());
        List<String> minionIds = saltHostnames.entrySet().stream()
                .filter(entry -> hostnamesWithoutDomain.contains(entry.getKey().split("\\.")[0]))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        SaltAction saltAction = new SaltAction(SaltActionType.STOP);
        for (String hostname : minionIds) {
            Minion minion = new Minion();
            minion.setAddress(saltHostnames.get(hostname));
            saltAction.addMinion(minion);
        }
        sc.action(saltAction);

        return sc.wheel("key.delete", minionIds, Object.class);
    }

    private static ApplyResponse applyState(SaltConnector sc, String service, Target<String> target) {
        return sc.run(target, "state.apply", LOCAL_ASYNC, ApplyResponse.class, service);
    }

    public static String resolveHostNameToMinionHostName(SaltConnector sc, String minionName) {
        Map<String, String> saltHostnames = SaltStates.networkInterfaceIP(sc, Glob.ALL).getResultGroupByHost();
        return saltHostnames.get(minionName);
    }

}
