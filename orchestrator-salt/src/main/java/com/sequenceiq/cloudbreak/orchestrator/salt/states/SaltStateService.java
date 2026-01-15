package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.RUNNER;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.common.model.PackageInfo;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.Memory;
import com.sequenceiq.cloudbreak.orchestrator.model.MemoryInfo;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyFullResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Cloud;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.CommandExecutionResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JidInfoResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusFromFileResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Os;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PackageVersionResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo.DurationComparator;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAuth;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltMaster;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SlsExistsSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.MinionUtil;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.RetryType;

@Service
public class SaltStateService {

    public static final Pattern RUNNING_HIGHSTATE_JID = Pattern.compile(".*The function .*state\\.highstate.* is running as PID.*with jid (\\d+).*");

    public static final String CMF_JAVA_OPTS_REGEX = "(?m)^(export CMF_JAVA_OPTS=\".*)" +
            "(?<! -Dcom.cloudera.cmf.service.AbstractCommandHandler.WORKAROUND_TIMEOUT_INTERVAL=600)\"$";

    private static final Pattern CLOUDERA_MANAGER_JVM_CONFIG_LINE = Pattern.compile(".+-Xmx(?<memory>\\d+)G.+");

    private static final long NETWORK_IPADDRS_TIMEOUT = 15L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltStateService.class);

    @Inject
    private MinionUtil minionUtil;

    public Map<String, String> getUuidList(SaltConnector sc) {
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

    public ApplyResponse addGrain(SaltConnector sc, Target<String> target, String key, String value) {
        return measure(() -> sc.run(target, "grains.append", LOCAL, ApplyResponse.class, key, value), LOGGER,
                "Grains append took {}ms for key [{}] value [{}] to targets: {}", key, value, target);
    }

    public ApplyResponse removeGrain(SaltConnector sc, Target<String> target, String key, String value) {
        return measure(() -> sc.run(target, "grains.remove", LOCAL, ApplyResponse.class, key, value), LOGGER,
                "Grains remove took {}ms for key [{}] value [{}] to targets: {}", key, value, target);
    }

    public ApplyResponse syncAll(SaltConnector sc) {
        return measure(() -> sc.run(Glob.ALL, "saltutil.sync_all", LOCAL_ASYNC, ApplyResponse.class), LOGGER,
                "SyncAll call took {}ms");
    }

    public ApplyResponse updateMine(SaltConnector sc) {
        return measure(() -> sc.run(Glob.ALL, "mine.update", LOCAL, ApplyResponse.class), LOGGER,
                "Mine update took {}msd");
    }

    public ApplyResponse highstate(SaltConnector sc) {
        return highstate(sc, Glob.ALL);
    }

    public ApplyResponse highstate(SaltConnector sc, Target<String> target) {
        return measure(() -> sc.run(target, "state.highstate", LOCAL_ASYNC, ApplyResponse.class), LOGGER,
                "HighState call took {}ms for targets: {}", target);
    }

    public ApplyFullResponse showState(SaltConnector sc, String state) {
        return sc.run(Glob.ALL, "state.show_sls", LOCAL, ApplyFullResponse.class, state);
    }

    public Multimap<String, Map<String, String>> jidInfo(SaltConnector sc, String jid, StateType stateType) {
        if (StateType.HIGH.equals(stateType)) {
            try {
                return highStateJidInfo(sc, jid);
            } catch (SaltExecutionWentWrongException e) {
                Optional<String> runningJid = extractJidIfPossible(e);
                if (runningJid.isPresent()) {
                    return highStateJidInfo(sc, runningJid.get());
                } else {
                    throw e;
                }
            }
        } else if (StateType.SIMPLE.equals(stateType)) {
            return applyStateJidInfo(sc, jid);
        }
        return ArrayListMultimap.create();
    }

    private Optional<String> extractJidIfPossible(SaltExecutionWentWrongException e) {
        Optional<String> jid = Optional.empty();
        if (e.getMessage() != null) {
            Matcher matcher = RUNNING_HIGHSTATE_JID.matcher(e.getMessage());
            if (matcher.matches()) {
                String runningHighStateJid = matcher.group(1);
                LOGGER.info("Highstate is running, but with another jid, check that jid also: {}", runningHighStateJid);
                jid = Optional.of(runningHighStateJid);
            }
        }
        return jid;
    }

    private Multimap<String, Map<String, String>> applyStateJidInfo(SaltConnector sc, String jid) {
        JidInfoResponse jidInfo = sc.run("jobs.lookup_jid", RUNNER, JidInfoResponse.class, "jid", jid);
        if (jidInfo.isEmpty()) {
            LOGGER.error("jobs.lookup_jid returns an empty response: {}", jidInfo);
            throw new SaltEmptyResponseException("jobs.lookup_jid returns an empty response. Please check the salt log.");
        }
        LOGGER.debug("Salt apply state jid info: {}", jidInfo);
        Map<String, List<RunnerInfo>> states = JidInfoResponseTransformer.getSimpleStates(jidInfo);
        return collectMissingTargets(states);
    }

    private Multimap<String, Map<String, String>> highStateJidInfo(SaltConnector sc, String jid) {
        JidInfoResponse jidInfo = sc.run("jobs.lookup_jid", RUNNER, JidInfoResponse.class, "jid", jid, "missing", "True");
        if (jidInfo.isEmpty()) {
            LOGGER.error("jobs.lookup_jid returns an empty response: {}", jidInfo);
            throw new SaltEmptyResponseException("jobs.lookup_jid returns an empty response. Please check the salt log.");
        }
        Map<String, List<RunnerInfo>> states = JidInfoResponseTransformer.getHighStates(jidInfo);
        return collectMissingTargets(states);
    }

    private Multimap<String, Map<String, String>> collectMissingTargets(Map<String, List<RunnerInfo>> stringRunnerInfoObjectMap) {
        Multimap<String, Map<String, String>> missingTargetsWithErrors = ArrayListMultimap.create();
        for (Entry<String, List<RunnerInfo>> stringMapEntry : stringRunnerInfoObjectMap.entrySet()) {
            LOGGER.debug("Collect missing targets from host: {}", stringMapEntry.getKey());
            if (stringMapEntry.getValue() != null) {
                logRunnerInfos(stringMapEntry);
                for (RunnerInfo targetObject : stringMapEntry.getValue()) {
                    if (!targetObject.getResult()) {
                        LOGGER.info("SaltStates: State id: {} job state has failed. Name: {} Reason: {}", targetObject.getStateId(), targetObject.getName(),
                                targetObject.getComment());
                        missingTargetsWithErrors.put(stringMapEntry.getKey(), targetObject.getErrorResultSummary());
                    }
                }
            }
        }
        return missingTargetsWithErrors;
    }

    private void logRunnerInfos(Entry<String, List<RunnerInfo>> stringMapEntry) {
        List<RunnerInfo> runnerInfos = stringMapEntry.getValue();
        runnerInfos.sort(Collections.reverseOrder(new DurationComparator()));
        double sum = runnerInfos.stream().mapToDouble(RunnerInfo::getDuration).sum();
        LOGGER.debug("SaltStates executed on: {} within: {} sec", stringMapEntry.getKey(),
                TimeUnit.MILLISECONDS.toSeconds(Math.round(sum)));
    }

    public boolean jobIsRunning(SaltConnector sc, String jid) throws CloudbreakOrchestratorFailedException {
        RunningJobsResponse runningInfo = getRunningJobs(sc);
        for (Map<String, Map<String, Object>> results : runningInfo.getResult()) {
            for (Entry<String, Map<String, Object>> stringMapEntry : results.entrySet()) {
                if (stringMapEntry.getKey().equals(jid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public RunningJobsResponse getRunningJobs(SaltConnector sc) throws CloudbreakOrchestratorFailedException {
        RunningJobsResponse runningInfo = sc.run("jobs.active", RUNNER, RunningJobsResponse.class);
        LOGGER.debug("Active salt jobs: {}", runningInfo);
        validateRunningInfoResultNotNull(runningInfo);
        return runningInfo;
    }

    private void validateRunningInfoResultNotNull(RunningJobsResponse runningInfo) throws CloudbreakOrchestratorFailedException {
        if (runningInfo == null || runningInfo.getResult() == null) {
            throw new CloudbreakOrchestratorFailedException("Configuration Management Software (Salt) installed on CDP cluster has returned an empty response "
                    + "multiple times. Contact Cloudera support with this message for resolving this error.");
        }
    }

    public Set<String> collectMinionIpAddresses(Optional<Set<Node>> saltTargetNodes, Retry retry, SaltConnector sc) {
        Set<String> minionIpAddresses = new HashSet<>();
        try {
            return collectMinionIpAddressesWithRetry(saltTargetNodes, retry, sc, minionIpAddresses);
        } catch (Retry.ActionFailedException e) {
            if ("Unreachable nodes found.".equals(e.getMessage())) {
                return minionIpAddresses;
            } else {
                throw e;
            }
        }
    }

    private Set<String> collectMinionIpAddressesWithRetry(Optional<Set<Node>> saltTargetNodes, Retry retry, SaltConnector sc, Set<String> minionIpAddresses) {
        return retry.testWith1SecDelayMax5Times(() -> {
            try {
                return collectMinionIpAddressesAndHandleUnreachableNodes(saltTargetNodes, sc, minionIpAddresses);
            } catch (Retry.ActionFailedException e) {
                throw e;
            } catch (RuntimeException e) {
                LOGGER.error("Collecting minion IP addresses failed", e);
                throw new Retry.ActionFailedException("Collecting minion IP addresses failed", e);
            }
        });
    }

    private Set<String> collectMinionIpAddressesAndHandleUnreachableNodes(Optional<Set<Node>> saltTargetNodes, SaltConnector sc, Set<String> minionIpAddresses) {
        MinionIpAddressesResponse minionIpAddressesResponse = saltTargetNodes.isPresent() ?
                collectMinionIpAddresses(sc, Optional.of(saltTargetNodes.get().stream().map(Node::getHostname).collect(Collectors.toSet()))) :
                collectMinionIpAddresses(sc);
        if (minionIpAddressesResponse == null) {
            LOGGER.debug("Minions ip address collection returned null value");
            throw new Retry.ActionFailedException("Minions ip address collection returned null value");
        }
        minionIpAddresses.addAll(minionIpAddressesResponse.getAllIpAddresses());
        if (!minionIpAddressesResponse.getUnreachableNodes().isEmpty()) {
            LOGGER.debug("Unreachable nodes found: {}, retry and collect minion ip addresses.", minionIpAddressesResponse.getUnreachableNodes());
            throw new Retry.ActionFailedException("Unreachable nodes found.");
        }
        return minionIpAddresses;
    }

    public boolean stateSlsExists(SaltConnector sc, Target<String> target, String state) {
        SlsExistsSaltResponse slsExistsSaltResponse = measure(() -> sc.run(target, "state.sls_exists", LOCAL,
                        SlsExistsSaltResponse.class, NETWORK_IPADDRS_TIMEOUT, state),
                LOGGER, "State SLS exists call took {}ms");
        LOGGER.debug("State SLS exists {}: {}", state, slsExistsSaltResponse);
        return slsExistsSaltResponse.getResult().stream().flatMap(map -> map.values().stream()).allMatch(Boolean::valueOf);
    }

    public MinionIpAddressesResponse collectMinionIpAddresses(SaltConnector sc) {
        return collectMinionIpAddresses(sc, Optional.empty());
    }

    public MinionIpAddressesResponse collectMinionIpAddresses(SaltConnector sc, Optional<Set<String>> targets) {
        Target<String> runTargets = targets.isPresent() ? new HostList(targets.get()) : Glob.ALL;
        MinionIpAddressesResponse minionIpAddressesResponse = measure(() -> sc.run(runTargets, "network.ipaddrs", LOCAL,
                        MinionIpAddressesResponse.class, NETWORK_IPADDRS_TIMEOUT),
                LOGGER, "Network IP address call took {}ms");
        LOGGER.debug("Minion ip response: {}", minionIpAddressesResponse);
        return minionIpAddressesResponse;
    }

    public MinionStatusSaltResponse collectNodeStatus(SaltConnector sc) {
        MinionStatusSaltResponse minionStatus = measure(() -> sc.run("manage.status", RUNNER, MinionStatusSaltResponse.class), LOGGER,
                "Manage status call took {}ms");
        LOGGER.debug("Minion status: {}", minionStatus);
        return minionStatus;
    }

    public MinionStatusFromFileResponse collectNodeStatusWithLimitedRetry(SaltConnector sc, String resultFileLocation) {
        MinionStatusFromFileResponse minionStatus = measure(() -> sc.runWithLimitedRetry(new HostList(List.of(sc.getHostname())), "file.read", LOCAL,
                MinionStatusFromFileResponse.class, resultFileLocation), LOGGER, "Read minion status json file took {} ms");
        LOGGER.debug("Minion status: {}", minionStatus);
        return minionStatus;
    }

    public boolean fileExists(SaltConnector sc, String fileLocation) {
        return measure(() -> sc.runWithLimitedRetry(new HostList(List.of(sc.getHostname())), "file.file_exists", LOCAL,
                        PingResponse.class, fileLocation), LOGGER, "Checking if file exists took {}ms").getResult().stream()
                .anyMatch(map -> map.values().stream().anyMatch(Boolean::booleanValue));
    }

    @Retryable(retryFor = WebApplicationException.class, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public PingResponse ping(SaltConnector sc, Target<String> target) {
        return measure(() -> sc.run(target, "test.ping", LOCAL, PingResponse.class), LOGGER, "Ping took {}ms");
    }

    @Retryable(retryFor = WebApplicationException.class, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public PingResponse ping(SaltConnector sc) {
        return measure(() -> sc.run(Glob.ALL, "test.ping", LOCAL, PingResponse.class), LOGGER, "Ping took {}ms");
    }

    public void stopMinions(SaltConnector sc, Set<String> privateIPs) {
        SaltAction saltAction = new SaltAction(SaltActionType.STOP);
        for (String entry : privateIPs) {
            Minion minion = new Minion();
            minion.setAddress(entry);
            saltAction.addMinion(minion);
        }
        sc.action(saltAction);
    }

    public void stopMasters(SaltConnector sc, Set<String> privateIPs) {
        SaltAction saltAction = new SaltAction(SaltActionType.STOP);
        for (String entry : privateIPs) {
            SaltMaster master = new SaltMaster();
            master.setAddress(entry);
            saltAction.addMaster(master);
        }
        sc.action(saltAction);
    }

    public GenericResponses changePassword(SaltConnector sc, Set<String> privateIPs, String password) throws CloudbreakOrchestratorFailedException {
        SaltAuth auth = new SaltAuth(password);
        SaltAction saltAction = new SaltAction(SaltActionType.CHANGE_PASSWORD);
        for (String privateIp : privateIPs) {
            SaltMaster master = new SaltMaster();
            master.setAddress(privateIp);
            master.setAuth(auth);
            saltAction.addMaster(master);
        }
        return sc.action(saltAction);
    }

    public GenericResponses bootstrap(SaltConnector sc, BootstrapParams params, List<GatewayConfig> allGatewayConfigs, Set<Node> targets) {
        SaltAction saltAction = new SaltAction(SaltActionType.RUN);
        if (params.getCloud() != null) {
            saltAction.setCloud(new Cloud(params.getCloud()));
        }
        if (params.getOs() != null) {
            saltAction.setOs(new Os(params.getOs()));
        }
        SaltAuth auth = new SaltAuth(sc.getSaltPassword());
        List<String> targetIps = targets.stream().map(Node::getPrivateIp).collect(Collectors.toList());
        List<String> gatewayPrivateIps = allGatewayConfigs.stream().map(GatewayConfig::getPrivateAddress).collect(Collectors.toList());
        for (GatewayConfig gatewayConfig : allGatewayConfigs) {
            String gatewayAddress = gatewayConfig.getPrivateAddress();
            if (targetIps.contains(gatewayAddress)) {
                Node saltMaster = targets.stream().filter(n -> n.getPrivateIp().equals(gatewayAddress)).findFirst().get();
                SaltMaster master = new SaltMaster();
                master.setAddress(gatewayAddress);
                master.setAuth(auth);
                master.setDomain(saltMaster.getDomain());
                master.setHostName(saltMaster.getHostname());
                // set due to compatibility reasons
                saltAction.setServer(gatewayAddress);
                saltAction.setMaster(master);
                saltAction.addMinion(minionUtil.createMinion(saltMaster, gatewayPrivateIps, params.isRestartNeededFlagSupported(), params.isRestartNeeded()));
                saltAction.addMaster(master);
            }
        }
        for (Node minionNode : targets.stream().filter(node -> !gatewayPrivateIps.contains(node.getPrivateIp())).collect(Collectors.toList())) {
            saltAction.addMinion(minionUtil.createMinion(minionNode, gatewayPrivateIps, params.isRestartNeededFlagSupported(), params.isRestartNeeded()));
        }
        return sc.action(saltAction);
    }

    public Map<String, List<PackageInfo>> getPackageVersions(SaltConnector sc, Map<String, Optional<String>> packages) {
        Map<String, List<PackageInfo>> packageVersions = new HashMap<>();
        packages.forEach((key, versionPattern) -> {
            Map<String, PackageInfo> singlePackageVersion = getSinglePackageVersion(sc, key, versionPattern);
            singlePackageVersion.entrySet().forEach(entry -> addToVersionList(packageVersions, entry));
        });
        return packageVersions;
    }

    private void addToVersionList(Map<String, List<PackageInfo>> packageVersions, Entry<String, PackageInfo> entry) {
        String hostKey = entry.getKey();
        PackageInfo packageInfoValue = entry.getValue();
        if (packageVersions.containsKey(hostKey)) {
            ArrayList<PackageInfo> packageInfos = new ArrayList<>(packageVersions.get(hostKey));
            packageInfos.add(packageInfoValue);
            packageVersions.put(hostKey, packageInfos);
        } else {
            packageVersions.put(hostKey, List.of(packageInfoValue));
        }
    }

    private Map<String, PackageInfo> getSinglePackageVersion(SaltConnector sc, String singlePackage, Optional<String> versionPattern) {
        PackageVersionResponse packageVersionResponse = measure(() -> sc.run(Glob.ALL, "pkg.version", LOCAL, PackageVersionResponse.class, singlePackage),
                LOGGER, "Get package version took {}ms for package [{}] with pattern [{}]", singlePackage, versionPattern);
        Map<String, String> packageVersionsMap =
                CollectionUtils.isEmpty(packageVersionResponse.getResult()) ? new HashMap<>() : packageVersionResponse.getResult().get(0);
        Map<String, PackageInfo> result = new HashMap<>();
        for (Entry<String, String> e : packageVersionsMap.entrySet()) {
            result.put(e.getKey(), parseVersion(singlePackage, e.getValue(), versionPattern));
        }
        return result;
    }

    public Optional<MemoryInfo> getMemoryInfo(SaltConnector sc, String masterFqdn) {
        try {
            Map<String, List<Map<String, Map<String, Map<String, String>>>>> result =
                    measure(() -> sc.run(new HostList(List.of(masterFqdn)), "status.meminfo", LOCAL, Map.class), LOGGER, "Get memory info took {}ms");
            return Optional.of(new MemoryInfo(result.get("return").get(0).get(masterFqdn)));
        } catch (Exception e) {
            LOGGER.error("Couldn't get memory info from master {}. Message: {}", masterFqdn, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<Memory> getClouderaManagerMemory(SaltConnector sc, GatewayConfig gatewayConfig) {
        try {
            Map<String, List<Map<String, String>>> result =
                    measure(() -> sc.run(new HostList(List.of(gatewayConfig.getHostname())), "cmd.run", LOCAL, Map.class,
                            "cat /etc/default/cloudera-scm-server | grep '^export CMF_JAVA_OPTS' | tail -n1"), LOGGER, "Get memory info took {}ms");
            LOGGER.info("Salt response from cloudera manager config: {}", result);
            String configLine = result.get("return").get(0).get(gatewayConfig.getHostname());
            Matcher matcher = CLOUDERA_MANAGER_JVM_CONFIG_LINE.matcher(configLine);
            if (matcher.matches()) {
                return Optional.of(Memory.ofGigaBytes(Integer.parseInt(matcher.group(1))));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't get memory info from master {}. Message: {}", gatewayConfig.getHostname(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Map<String, String> runCommand(Retry retry, SaltConnector sc, String command) {
        return retry.testWith2SecDelayMax15Times(() -> runCommand(sc, command));
    }

    public Map<String, String> runCommandWithFewRetry(Retry retry, SaltConnector sc, String command) {
        return retry.testWith1SecDelayMax3Times(() -> runCommand(sc, command));
    }

    public Map<String, String> runCommandWithoutRetry(SaltConnector sc, String command) {
        return runCommand(sc, command);
    }

    private Map<String, String> runCommand(SaltConnector sc, String command) {
        try {
            CommandExecutionResponse resp = measure(() -> sc.run(Glob.ALL, "cmd.run", LOCAL, CommandExecutionResponse.class, command), LOGGER,
                    "Command run took {}ms for command [{}]", command);
            List<Map<String, String>> result = resp.getResult();
            return CollectionUtils.isEmpty(result) ? new HashMap<>() : result.get(0);
        } catch (RuntimeException e) {
            LOGGER.error("Salt run command failed", e);
            throw new Retry.ActionFailedException("Salt run command failed");
        }
    }

    public Map<String, String> replacePatternInFile(Retry retry, SaltConnector sc, String file, String pattern, String replace) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                String[] args = new String[]{file, String.format("pattern='%s'", pattern), String.format("repl='%s'", replace)};
                CommandExecutionResponse resp = measure(() -> sc.run(Glob.ALL, "file.replace", LOCAL, CommandExecutionResponse.class, args), LOGGER,
                        "Command run took {}ms for file.replace with args [{}]", (Object) args);
                List<Map<String, String>> result = resp.getResult();
                return CollectionUtils.isEmpty(result) ? new HashMap<>() : result.get(0);
            } catch (RuntimeException e) {
                LOGGER.error("Salt run command failed", e);
                throw new Retry.ActionFailedException("Salt run command failed");
            }
        });
    }

    public Map<String, String> runCommandOnHosts(Retry retry, SaltConnector sc, Target<String> target, String command) {
        return runCommandOnHosts(retry, sc, target, command, RetryType.WITH_2_SEC_DELAY_MAX_15_TIMES);
    }

    public Map<String, String> runCommandOnHosts(Retry retry, SaltConnector sc, Target<String> target, String command, RetryType retryType) {
        return retryType.execute(retry, () -> {
            try {
                CommandExecutionResponse resp = measure(() -> sc.run(target, "cmd.run", LOCAL, CommandExecutionResponse.class, command), LOGGER,
                        "Command run took {}ms for command: [{}]", command);
                List<Map<String, String>> result = resp.getResult();
                return CollectionUtils.isEmpty(result) ? new HashMap<>() : result.getFirst();
            } catch (RuntimeException e) {
                LOGGER.error("Salt run command on hosts failed", e);
                throw new Retry.ActionFailedException("Salt run command on hosts failed", e);
            }
        });
    }

    public Map<String, JsonNode> getGrains(SaltConnector sc, String grain) {
        return getGrains(sc, Glob.ALL, grain);
    }

    @Retryable(retryFor = WebApplicationException.class, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000), maxAttempts = 5)
    public Map<String, JsonNode> getGrains(SaltConnector sc, Target<String> target, String grain) {
        ApplyResponse resp = measure(() -> sc.run(target, "grains.get", LOCAL, ApplyResponse.class, grain), LOGGER,
                "GrainsGet took {}ms for grain [{}]", grain);
        Iterable<Map<String, JsonNode>> result = resp.getResult();
        return result.iterator().hasNext() ? result.iterator().next() : new HashMap<>();
    }

    public ApplyResponse applyStateSync(SaltConnector sc, String service, Target<String> target) {
        return measure(() -> sc.run(target, "state.apply", LOCAL, ApplyResponse.class, service), LOGGER,
                "ApplyState sync took {}ms for service [{}]", service);
    }

    public ApplyResponse applyState(SaltConnector sc, String service, Target<String> target) {
        return measure(() -> sc.run(target, "state.apply", LOCAL_ASYNC, ApplyResponse.class, service), LOGGER,
                "ApplyState async took {}ms for service [{}]", service);
    }

    public ApplyResponse applyState(SaltConnector sc, String service, Target<String> target,
            Map<String, Object> inlinePillars) throws JsonProcessingException {
        String inlinePillarsStr = new ObjectMapper().writeValueAsString(inlinePillars);
        return measure(() -> sc.run(target, "state.apply", LOCAL_ASYNC, ApplyResponse.class, service, String.format("pillar=%s", inlinePillarsStr)), LOGGER,
                "ApplyState with pillars took {}ms for service [{}]", service);
    }

    public ApplyResponse applyConcurrentState(SaltConnector sc, String service, Target<String> target,
            Map<String, Object> inlinePillars) throws JsonProcessingException {
        String inlinePillarsStr = new ObjectMapper().writeValueAsString(inlinePillars);
        return measure(() -> sc.run(target, "state.apply", LOCAL_ASYNC, ApplyResponse.class, service, String.format("pillar=%s", inlinePillarsStr),
                "concurrent=True"), LOGGER, "ApplyConcurrentState took {}ms for service [{}]", service);
    }

    public ApplyResponse applyStateAll(SaltConnector sc, String service) {
        return applyState(sc, service, Glob.ALL);
    }

    private ApplyResponse applyStateAllSync(SaltConnector sc, String service) {
        return measure(() -> sc.run(Glob.ALL, "state.apply", LOCAL, ApplyResponse.class, service), LOGGER,
                "ApplyState in sync for ALL took {}ms for service [{}]", service);
    }

    private PackageInfo parseVersion(String packageName, String versionCommandOutput, Optional<String> pattern) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setName(packageName);
        packageInfo.setVersion(versionCommandOutput);
        if (pattern.isPresent()) {
            Matcher matcher = Pattern.compile(pattern.get()).matcher(versionCommandOutput);
            if (matcher.matches()) {
                packageInfo.setVersion(matcher.group(1));
                if (matcher.groupCount() > 1) {
                    packageInfo.setBuildNumber(matcher.group(2));
                }
            }
        }
        LOGGER.debug("Package version parsed as {}", packageInfo);
        return packageInfo;
    }

    public boolean unboundClusterConfigPresentOnAnyNodes(SaltConnector sc, Target<String> target) {
        return measure(() -> sc.run(target, "file.file_exists", LOCAL, PingResponse.class, "/etc/unbound/conf.d/00-cluster.conf"), LOGGER,
                "Getting information about existence of unbound config took {}ms").getResult().stream()
                .anyMatch(map -> map.values().stream().anyMatch(Boolean::booleanValue));
    }

    public void setClouderaManagerMemory(SaltConnector sc, GatewayConfig gatewayConfig, Memory memory) {
        try {
            measure(() -> sc.run(new HostList(List.of(gatewayConfig.getHostname())), "file.replace", LOCAL, Map.class,
                            "/etc/default/cloudera-scm-server", "Xmx\\d+G", "Xmx" + (int) Math.ceil(memory.getValueInGigaBytes()) + "G"), LOGGER,
                    "Set memory took {}ms");
        } catch (Exception e) {
            LOGGER.error("Couldn't set memory info for master {}. Message: {}", gatewayConfig.getHostname(), e.getMessage(), e);
        }
    }

    public boolean setClouderaManagerOperationTimeout(SaltConnector sc, GatewayConfig gatewayConfig) {
        try {
            Map<String, List<Map<String, String>>> result = measure(() -> sc.run(new HostList(List.of(gatewayConfig.getHostname())), "file.replace", LOCAL,
                    Map.class, "/etc/default/cloudera-scm-server",
                    CMF_JAVA_OPTS_REGEX,
                    "\\1 -Dcom.cloudera.cmf.service.AbstractCommandHandler.WORKAROUND_TIMEOUT_INTERVAL=600\""
            ), LOGGER, "Add WORKAROUND_TIMEOUT_INTERVAL to CM config took {}ms");
            LOGGER.info("Result of CM server config change: {}", result);
            return result.get("return").stream().flatMap(element -> element.values().stream()).noneMatch(String::isEmpty);
        } catch (Exception e) {
            LOGGER.error("Couldn't add WORKAROUND_TIMEOUT_INTERVAL to CM config for master {}. Message: {}", gatewayConfig.getHostname(), e.getMessage(), e);
            return false;
        }
    }

    public void removeSecurityConfigFromCMAgentsConfig(SaltConnector sc, Target<String> target) {
        measure(() -> sc.run(target, "file.replace", LOCAL,
                Map.class, "/etc/cloudera-scm-agent/config.ini",
                "(?ms)### FOLLOWING SECTION AUTO-GENERATED BY AUTO-TLS.*?### PRECEDING SECTION AUTO-GENERATED BY AUTO-TLS ###\\n",
                ""
        ), LOGGER, "Remove security config from CM took {}ms");
    }

}