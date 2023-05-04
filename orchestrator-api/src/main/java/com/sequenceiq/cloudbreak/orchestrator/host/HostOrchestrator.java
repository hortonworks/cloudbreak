package com.sequenceiq.cloudbreak.orchestrator.host;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.model.PackageInfo;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.CmAgentStopFlags;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.KeytabModel;
import com.sequenceiq.cloudbreak.orchestrator.model.Memory;
import com.sequenceiq.cloudbreak.orchestrator.model.MemoryInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface HostOrchestrator extends HostRecipeExecutor {

    String name();

    void bootstrap(List<GatewayConfig> allGatewayConfigs, Set<Node> targets, BootstrapParams params,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    void bootstrapNewNodes(List<GatewayConfig> allGatewayConfigs, Set<Node> nodes, Set<Node> allNodes, byte[] stateConfigZip,
            BootstrapParams params, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    void reBootstrapExistingNodes(List<GatewayConfig> allGatewayConfigs, Set<Node> nodes, BootstrapParams params,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig);

    void changePassword(List<GatewayConfig> allGatewayConfigs, String newPassword, String oldPassword) throws CloudbreakOrchestratorException;

    void initServiceRun(OrchestratorAware stack, List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes, Set<Node> reachableNodes,
            SaltConfig pillarConfig, ExitCriteriaModel exitCriteriaModel, String cloudPlatform) throws CloudbreakOrchestratorException;

    void initSaltConfig(OrchestratorAware stack, List<GatewayConfig> allGateway, Set<Node> allNodes, SaltConfig saltConfig, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException;

    void runService(List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes,
            SaltConfig pillarConfig, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    void resetClusterManager(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void stopClusterManagerOnMaster(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    void startClusterManagerOnMaster(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void restartClusterManagerOnMaster(GatewayConfig gatewayConfig, Set<String> target, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void stopClusterManagerWithItsAgents(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void startClusterManagerWithItsAgents(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void updateAgentCertDirectoryPermission(GatewayConfig gatewayConfig, Set<String> target, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void upgradeClusterManager(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, SaltConfig pillarConfig, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    NodeReachabilityResult getResponsiveNodes(Set<Node> nodes, GatewayConfig gatewayConfig, boolean targeted);

    void tearDown(OrchestratorAware stack, List<GatewayConfig> allGatewayConfigs, Map<String, String> removeNodePrivateIPsByFQDN,
            Set<Node> remainingNodes, ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException;

    Map<String, Map<String, String>> getPackageVersionsFromAllHosts(GatewayConfig gateway, Map<String, Optional<String>> packages)
            throws CloudbreakOrchestratorFailedException;

    Map<String, List<PackageInfo>> getFullPackageVersionsFromAllHosts(GatewayConfig gateway, Map<String, Optional<String>> packages)
            throws CloudbreakOrchestratorFailedException;

    Map<String, String> runCommandOnAllHosts(GatewayConfig gateway, String command) throws CloudbreakOrchestratorFailedException;

    Map<String, String> runCommandOnHosts(List<GatewayConfig> allGatewayConfigs, Set<String> targetFqdns, String command)
            throws CloudbreakOrchestratorFailedException;

    Map<String, String> replacePatternInFileOnAllHosts(GatewayConfig gatewayConfig, String file, String pattern, String replace)
            throws CloudbreakOrchestratorFailedException;

    Map<String, JsonNode> getGrainOnAllHosts(GatewayConfig gateway, String grain) throws CloudbreakOrchestratorFailedException;

    Map<String, String> getMembers(GatewayConfig gatewayConfig, List<String> privateIps) throws CloudbreakOrchestratorException;

    void changePrimaryGateway(GatewayConfig formerGateway, GatewayConfig newPrimaryGateway, List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    void validateCloudStorageBackup(GatewayConfig primaryGateway, List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    void installFreeIpa(GatewayConfig primaryGateway, List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    Optional<String> getFreeIpaMasterHostname(GatewayConfig primaryGateway, Set<Node> allNodes) throws CloudbreakOrchestratorException;

    void leaveDomain(GatewayConfig gatewayConfig, Set<Node> allNodes, String roleToRemove, String roleToAdd, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException;

    byte[] getStateConfigZip() throws IOException;

    Map<String, Map<String, String>> formatAndMountDisksOnNodes(OrchestratorAware stack, List<GatewayConfig> allGateway, Set<Node> targets, Set<Node> allNodes,
            ExitCriteriaModel exitModel, String platformVariant) throws CloudbreakOrchestratorFailedException;

    void stopClusterManagerAgent(OrchestratorAware stack, GatewayConfig gatewayConfig, Set<Node> allNodes, Set<Node> stoppedNodes,
            ExitCriteriaModel exitCriteriaModel, CmAgentStopFlags flags) throws CloudbreakOrchestratorFailedException;

    void uploadKeytabs(List<GatewayConfig> allGatewayConfigs, Set<KeytabModel> keytabModels, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException;

    Map<String, Map<String, String>> formatAndMountDisksOnNodesLegacy(List<GatewayConfig> gatewayConfigs, Set<Node> targets, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel, String platformVariant) throws CloudbreakOrchestratorFailedException;

    void backupDatabase(GatewayConfig primaryGateway, Set<String> target, SaltConfig saltConfig, ExitCriteriaModel exitModel, int databaseMaxDurationInMin)
            throws CloudbreakOrchestratorFailedException;

    void restoreDatabase(GatewayConfig primaryGateway, Set<String> target, SaltConfig saltConfig, ExitCriteriaModel exitModel, int databaseMaxDurationInMin)
            throws CloudbreakOrchestratorFailedException;

    void applyDiagnosticsState(List<GatewayConfig> gatewayConfigs, String state, Map<String, Object> parameters,
            ExitCriteriaModel stackBasedExitCriteriaModel) throws CloudbreakOrchestratorFailedException;

    void uploadGatewayPillar(List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes, ExitCriteriaModel exitModel, SaltConfig saltConfig)
            throws CloudbreakOrchestratorFailedException;

    void runOrchestratorState(OrchestratorStateParams stateParameters) throws CloudbreakOrchestratorFailedException;

    void runOrchestratorGrainRunner(OrchestratorGrainRunnerParams grainRunnerParams) throws CloudbreakOrchestratorFailedException;

    Map<String, String> getFreeDiskSpaceByNodes(Set<Node> nodes, List<GatewayConfig> gatewayConfigs);

    void removeDeadSaltMinions(GatewayConfig gatewayConfig) throws CloudbreakOrchestratorFailedException;

    boolean unboundClusterConfigPresentOnAnyNodes(GatewayConfig primaryGateway, Set<String> nodes);

    void uploadStates(List<GatewayConfig> allGatewayConfigs, ExitCriteriaModel exitModel) throws CloudbreakOrchestratorException;

    LocalDate getPasswordExpiryDate(List<GatewayConfig> allGatewayConfigs, String user) throws CloudbreakOrchestratorException;

    void createCronForUserHomeCreation(List<GatewayConfig> gatewayConfigs, Set<String> targets, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException;

    List<Map<String, JsonNode>> applyOrchestratorState(OrchestratorStateParams stateParams) throws CloudbreakOrchestratorFailedException;

    boolean doesPhaseSlsExistWithTimeouts(GatewayConfig gatewayConfig, String stateSlsName, int connectTimeoutMs, int readTimeout)
            throws CloudbreakOrchestratorFailedException;

    void saveCustomPillars(SaltConfig saltConfig, ExitCriteriaModel exitModel, OrchestratorStateParams stateParams)
            throws CloudbreakOrchestratorFailedException;

    Optional<MemoryInfo> getMemoryInfo(GatewayConfig gatewayConfig) throws CloudbreakOrchestratorFailedException;

    Optional<Memory> getClusterManagerMemory(GatewayConfig gatewayConfig) throws CloudbreakOrchestratorFailedException;

    void setClusterManagerMemory(GatewayConfig gatewayConfig, Memory memory) throws CloudbreakOrchestratorFailedException;
}
