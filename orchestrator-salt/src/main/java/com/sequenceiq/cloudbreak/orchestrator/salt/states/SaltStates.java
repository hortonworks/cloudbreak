package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.RUNNER;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class SaltStates {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltStates.class);

    private SaltStates() {
    }

    public static String ambariReset(SaltConnector sc, Target<String> target) {
        return applyState(sc, "ambari.reset", target).getJid();
    }

    public static ApplyResponse addGrain(SaltConnector sc, Target<String> target, String key, String value) {
        return sc.run(target, "grains.append", LOCAL, ApplyResponse.class, key, value);
    }

    public static ApplyResponse removeGrain(SaltConnector sc, Target<String> target, String key, String value) {
        return sc.run(target, "grains.remove", LOCAL, ApplyResponse.class, key, value);
    }

    public static ApplyResponse syncGrains(SaltConnector sc) {
        return sc.run(Glob.ALL, "saltutil.sync_grains", LOCAL, ApplyResponse.class);
    }

    public static String highstate(SaltConnector sc) {
        return sc.run(Glob.ALL, "state.highstate", LOCAL_ASYNC, ApplyResponse.class).getJid();
    }

    public static Multimap<String, String> jidInfo(SaltConnector sc, String jid, Target<String> target, StateType stateType) {
        if (StateType.HIGH.equals(stateType)) {
            return highStateJidInfo(sc, jid, target);
        } else if (StateType.SIMPLE.equals(stateType)) {
            return applyStateJidInfo(sc, jid, target);
        }
        return ArrayListMultimap.create();
    }

    private static Multimap<String, String> applyStateJidInfo(SaltConnector sc, String jid, Target<String> target) {
        Map jidInfo = sc.run(target, "jobs.lookup_jid", RUNNER, Map.class, "jid", jid);
        LOGGER.info("Salt apply state jid info: {}", jidInfo);
        Map<String, List<RunnerInfo>> states = JidInfoResponseTransformer.getSimpleStates(jidInfo);
        return collectMissingTargets(states);
    }

    private static Multimap<String, String> highStateJidInfo(SaltConnector sc, String jid, Target<String> target) {
        Map jidInfo = sc.run(target, "jobs.lookup_jid", RUNNER, Map.class, "jid", jid);
        LOGGER.info("Salt high state jid info: {}", jidInfo);
        Map<String, List<RunnerInfo>> states = JidInfoResponseTransformer.getHighStates(jidInfo);
        return collectMissingTargets(states);
    }

    private static Multimap<String, String> collectMissingTargets(Map<String, List<RunnerInfo>> stringRunnerInfoObjectMap) {
        Multimap<String, String> missingTargetsWithErrors = ArrayListMultimap.create();
        for (Map.Entry<String, List<RunnerInfo>> stringMapEntry : stringRunnerInfoObjectMap.entrySet()) {
            LOGGER.info("Collect missing targets from host: {}", stringMapEntry.getKey());
            logRunnerInfos(stringMapEntry);
            for (RunnerInfo targetObject : stringMapEntry.getValue()) {
                if (targetObject.getResult()) {
                    LOGGER.info("{} finished in {} ms.", targetObject.getComment(), targetObject.getDuration());
                } else {
                    LOGGER.info("{} job state is {}.", targetObject.getComment(), targetObject.getResult());
                    missingTargetsWithErrors.put(stringMapEntry.getKey(), targetObject.getComment());
                }
            }
        }
        return missingTargetsWithErrors;
    }

    private static void logRunnerInfos(Map.Entry<String, List<RunnerInfo>> stringMapEntry) {
        List<RunnerInfo> runnerInfos = stringMapEntry.getValue();
        Collections.sort(runnerInfos, Collections.reverseOrder(new RunnerInfo.DurationComparator()));
        double sum = runnerInfos.stream().mapToDouble(runnerInfo -> runnerInfo.getDuration()).sum();
        try {
            LOGGER.info("Salt states executed on: {} within: {} sec, details {}", stringMapEntry.getKey(),
                    TimeUnit.MILLISECONDS.toSeconds(Math.round(sum)), JsonUtil.writeValueAsString(runnerInfos));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to serialise runnerInfos. Salt states executed on: {} within: {} sec", stringMapEntry.getKey(),
                    TimeUnit.MILLISECONDS.toSeconds(Math.round(sum)));
        }
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
