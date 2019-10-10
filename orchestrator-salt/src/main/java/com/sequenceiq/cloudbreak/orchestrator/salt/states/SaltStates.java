package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.RUNNER;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.CommandExecutionResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PackageVersionResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo.DurationComparator;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;

public class SaltStates {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltStates.class);

    private SaltStates() {
    }

    public static String ambariReset(SaltConnector sc, Target<String> target) {
        return applyState(sc, "ambari.reset", target).getJid();
    }

    public static String mountDisks(SaltConnector sc) {
        return applyStateAll(sc, "disks.format-and-mount").getJid();
    }

    public static Map<String, String> getUuidList(SaltConnector sc) {
        ApplyResponse applyResponse = applyStateAllSync(sc, "disks.get-uuid-list");
        List<Map<String, JsonNode>> result = (List<Map<String, JsonNode>>) applyResponse.getResult();
        if (CollectionUtils.isEmpty(result) || 1 != result.size()) {
            return Map.of();
        }

        return result.get(0).entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> {
            JsonNode responseJson = entry.getValue();
            return Optional.ofNullable(responseJson.get("cmd_|-execute_get_uuid_list_|-/opt/salt/scripts/get-uuid-list.sh_|-run"))
                    .map(cmdNode -> cmdNode.get("changes"))
                    .map(changesNode -> changesNode.get("stdout"))
                    .map(JsonNode::textValue)
                    .orElse("");
        }));
    }

    public static ApplyResponse addGrain(SaltConnector sc, Target<String> target, String key, String value) {
        return sc.run(target, "grains.append", LOCAL, ApplyResponse.class, key, value);
    }

    public static ApplyResponse removeGrain(SaltConnector sc, Target<String> target, String key, String value) {
        return sc.run(target, "grains.remove", LOCAL, ApplyResponse.class, key, value);
    }

    public static ApplyResponse syncAll(SaltConnector sc) {
        return sc.run(Glob.ALL, "saltutil.sync_all", LOCAL, ApplyResponse.class);
    }

    public static ApplyResponse updateMine(SaltConnector sc) {
        return sc.run(Glob.ALL, "mine.update", LOCAL, ApplyResponse.class);
    }

    public static String highstate(SaltConnector sc) {
        return sc.run(Glob.ALL, "state.highstate", LOCAL_ASYNC, ApplyResponse.class).getJid();
    }

    public static Multimap<String, String> jidInfo(SaltConnector sc, String jid, Target<String> target, StateType stateType) {
        if (StateType.HIGH.equals(stateType)) {
            return highStateJidInfo(sc, jid);
        } else if (StateType.SIMPLE.equals(stateType)) {
            return applyStateJidInfo(sc, jid);
        }
        return ArrayListMultimap.create();
    }

    private static Multimap<String, String> applyStateJidInfo(SaltConnector sc, String jid) {
        Map<?, ?> jidInfo = sc.run("jobs.lookup_jid", RUNNER, Map.class, "jid", jid);
        LOGGER.debug("Salt apply state jid info: {}", jidInfo);
        Map<String, List<RunnerInfo>> states = JidInfoResponseTransformer.getSimpleStates(jidInfo);
        return collectMissingTargets(states);
    }

    private static Multimap<String, String> highStateJidInfo(SaltConnector sc, String jid) {
        Map<String, List<Map<String, Object>>> jidInfo = sc.run("jobs.lookup_jid", RUNNER, Map.class, "jid", jid);
        Map<String, List<RunnerInfo>> states = JidInfoResponseTransformer.getHighStates(jidInfo);
        return collectMissingTargets(states);
    }

    private static Multimap<String, String> collectMissingTargets(Map<String, List<RunnerInfo>> stringRunnerInfoObjectMap) {
        Multimap<String, String> missingTargetsWithErrors = ArrayListMultimap.create();
        for (Entry<String, List<RunnerInfo>> stringMapEntry : stringRunnerInfoObjectMap.entrySet()) {
            LOGGER.debug("Collect missing targets from host: {}", stringMapEntry.getKey());
            logRunnerInfos(stringMapEntry);
            for (RunnerInfo targetObject : stringMapEntry.getValue()) {
                if (!targetObject.getResult()) {
                    LOGGER.info("SaltStates: State id: {} job state has failed. Name: {} Reason: {}", targetObject.getStateId(), targetObject.getName(),
                            targetObject.getComment());
                    missingTargetsWithErrors.put(stringMapEntry.getKey(), targetObject.getErrorResultSummary());
                }
            }
        }
        return missingTargetsWithErrors;
    }

    private static void logRunnerInfos(Entry<String, List<RunnerInfo>> stringMapEntry) {
        List<RunnerInfo> runnerInfos = stringMapEntry.getValue();
        runnerInfos.sort(Collections.reverseOrder(new DurationComparator()));
        double sum = runnerInfos.stream().mapToDouble(RunnerInfo::getDuration).sum();
        LOGGER.debug("SaltStates executed on: {} within: {} sec", stringMapEntry.getKey(),
                TimeUnit.MILLISECONDS.toSeconds(Math.round(sum)));
    }

    public static boolean jobIsRunning(SaltConnector sc, String jid) {
        RunningJobsResponse runningInfo = sc.run("jobs.active", RUNNER, RunningJobsResponse.class);
        LOGGER.debug("Active salt jobs: {}", runningInfo);
        for (Map<String, Map<String, Object>> results : runningInfo.getResult()) {
            for (Entry<String, Map<String, Object>> stringMapEntry : results.entrySet()) {
                if (stringMapEntry.getKey().equals(jid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static MinionIpAddressesResponse collectMinionIpAddresses(SaltConnector sc) {
        MinionIpAddressesResponse minionIpAddressesResponse = sc.run(Glob.ALL, "network.ipaddrs", LOCAL, MinionIpAddressesResponse.class);
        LOGGER.debug("Minion ip response: {}", minionIpAddressesResponse);
        return minionIpAddressesResponse;
    }

    public static MinionStatusSaltResponse collectNodeStatus(SaltConnector sc) {
        MinionStatusSaltResponse minionStatus = sc.run("manage.status", RUNNER, MinionStatusSaltResponse.class);
        LOGGER.debug("Minion status: {}", minionStatus);
        return minionStatus;
    }

    public static PingResponse ping(SaltConnector sc, Target<String> target) {
        return sc.run(target, "test.ping", LOCAL, PingResponse.class);
    }

    public static void stopMinions(SaltConnector sc, Map<String, String> privateIPsByFQDN) {
        SaltAction saltAction = new SaltAction(SaltActionType.STOP);
        for (Entry<String, String> entry : privateIPsByFQDN.entrySet()) {
            Minion minion = new Minion();
            minion.setAddress(entry.getValue());
            saltAction.addMinion(minion);
        }
        sc.action(saltAction);
    }

    public static Map<String, Map<String, String>> getPackageVersions(SaltConnector sc, String... packages) {
        if (packages.length == 1) {
            return getSinglePackageVersion(sc, packages[0]);
        } else if (packages.length > 1) {
            // Table<host, packageName, version>
            Table<String, String, String> packageTable = HashBasedTable.create();
            Arrays.stream(packages).forEach(singlePackage -> {
                Map<String, Map<String, String>> singlePackageVersionByHost = getSinglePackageVersion(sc, singlePackage);
                singlePackageVersionByHost.entrySet()
                        .stream()
                        .forEach(singlePackageVersionByHostEntry -> singlePackageVersionByHostEntry.getValue().entrySet()
                                .stream()
                                .forEach(singlePackageVersion -> packageTable.put(singlePackageVersionByHostEntry.getKey(),
                                        singlePackageVersion.getKey(), singlePackageVersion.getValue())));
            });
            return packageTable.rowMap();
        } else {
            return Collections.emptyMap();
        }
    }

    private static Map<String, Map<String, String>> getSinglePackageVersion(SaltConnector sc, String singlePackage) {
        PackageVersionResponse packageVersionResponse = sc.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, singlePackage);
        Map<String, String> packageVersionsMap =
                CollectionUtils.isEmpty(packageVersionResponse.getResult()) ? new HashMap<>() : packageVersionResponse.getResult().get(0);
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Entry<String, String> e : packageVersionsMap.entrySet()) {
            Map<String, String> versionMap = new HashMap<>();
            versionMap.put(singlePackage, e.getValue());
            result.put(e.getKey(), versionMap);
        }
        return result;
    }

    public static Map<String, String> runCommand(SaltConnector sc, String command) {
        CommandExecutionResponse resp = sc.run(Glob.ALL, "cmd.run", LOCAL, CommandExecutionResponse.class, command);
        List<Map<String, String>> result = resp.getResult();
        return CollectionUtils.isEmpty(result) ? new HashMap<>() : result.get(0);
    }

    public static Map<String, String> runCommandOnHosts(SaltConnector sc, Target<String> target, String command) {
        CommandExecutionResponse resp = sc.run(target, "cmd.run", LOCAL, CommandExecutionResponse.class, command);
        List<Map<String, String>> result = resp.getResult();
        return CollectionUtils.isEmpty(result) ? new HashMap<>() : result.get(0);
    }

    public static Map<String, JsonNode> getGrains(SaltConnector sc, String grain) {
        return getGrains(sc, Glob.ALL, grain);
    }

    public static Map<String, JsonNode> getGrains(SaltConnector sc, Target<String> target, String grain) {
        ApplyResponse resp = sc.run(target, "grains.get", LOCAL, ApplyResponse.class, grain);
        Iterable<Map<String, JsonNode>> result = resp.getResult();
        return result.iterator().hasNext() ? result.iterator().next() : new HashMap<>();
    }

    private static ApplyResponse applyState(SaltConnector sc, String service, Target<String> target) {
        return sc.run(target, "state.apply", LOCAL_ASYNC, ApplyResponse.class, service);
    }

    private static ApplyResponse applyStateAll(SaltConnector sc, String service) {
        return applyState(sc, service, Glob.ALL);
    }

    private static ApplyResponse applyStateAllSync(SaltConnector sc, String service) {
        return sc.run(Glob.ALL, "state.apply", LOCAL, ApplyResponse.class, service);
    }
}
