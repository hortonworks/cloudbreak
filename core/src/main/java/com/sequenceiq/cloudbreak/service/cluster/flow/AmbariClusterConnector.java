package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.common.type.CloudPlatform.AZURE_RM;
import static com.sequenceiq.cloudbreak.common.type.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_DB;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_SERVER;
import static com.sequenceiq.cloudbreak.service.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isFailure;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.PollingResult.isTimeout;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS;
import static com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.DEFAULT_RECIPE_TIMEOUT;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.common.type.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.PluginExecutionType;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.common.type.Status;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.ClusterException;
import com.sequenceiq.cloudbreak.core.flow.service.AmbariHostsRemover;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfigurationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterConnector {

    private static final int MAX_ATTEMPTS_FOR_REGION_DECOM = 500;

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterConnector.class);

    @Inject
    private StackRepository stackRepository;
    @Inject
    private ClusterRepository clusterRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;
    @Inject
    private HostGroupRepository hostGroupRepository;
    @Inject
    private AmbariOperationService ambariOperationService;
    @Inject
    private PollingService<AmbariHostsCheckerContext> hostsPollingService;
    @Inject
    private PollingService<AmbariHostsWithNames> rsPollerService;
    @Inject
    private HadoopConfigurationService hadoopConfigurationService;
    @Inject
    private AmbariClientProvider ambariClientProvider;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private RecipeEngine recipeEngine;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private AmbariHostsStatusCheckerTask ambariHostsStatusCheckerTask;
    @Inject
    private AmbariHostsLeaveStatusCheckerTask hostsLeaveStatusCheckerTask;
    @Inject
    private DNDecommissionStatusCheckerTask dnDecommissionStatusCheckerTask;
    @Inject
    private RSDecommissionStatusCheckerTask rsDecommissionStatusCheckerTask;
    @Inject
    private ClusterSecurityService securityService;
    @Inject
    private InstanceTerminationHandler instanceTerminationHandler;
    @Inject
    private AmbariHostsRemover ambariHostsRemover;
    @Inject
    private PollingService<AmbariHostsCheckerContext> ambariHostJoin;
    @Inject
    private PollingService<AmbariHostsWithNames> ambariHostLeave;
    @Inject
    private PollingService<AmbariClientPollerObject> ambariHealthChecker;
    @Inject
    private AmbariHealthCheckerTask ambariHealthCheckerTask;
    @Inject
    private AmbariHostsJoinStatusCheckerTask ambariHostsJoinStatusCheckerTask;
    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private HostMetadataRepository hostMetadataRepository;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;
    @Resource
    private Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators;
    @Inject
    private BlueprintProcessor blueprintProcessor;
    @Inject
    private RecipeBuilder recipeBuilder;
    @Inject
    private DefaultConfigProvider defaultConfigProvider;

    private enum Msg {
        AMBARI_CLUSTER_RESETTING_AMBARI_DATABASE("ambari.cluster.resetting.ambari.database"),
        AMBARI_CLUSTER_AMBARI_DATABASE_RESET("ambari.cluster.ambari.database.reset"),
        AMBARI_CLUSTER_RESTARTING_AMBARI_SERVER("ambari.cluster.restarting.ambari.server"),
        AMBARI_CLUSTER_AMBARI_SERVER_RESTARTED("ambari.cluster.ambari.server.restarted"),
        AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP("ambari.cluster.removing.node.from.hostgroup"),
        AMBARI_CLUSTER_ADDING_NODE_TO_HOSTGROUP("ambari.cluster.adding.node.to.hostgroup"),
        AMBARI_CLUSTER_HOST_JOIN_FAILED("ambari.cluster.host.join.failed"),
        AMBARI_CLUSTER_INSTALL_FAILED("ambari.cluster.install.failed"),
        AMBARI_CLUSTER_MR_SMOKE_FAILED("ambari.cluster.mr.smoke.failed");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }


    public Cluster buildAmbariCluster(Stack stack) {
        Cluster cluster = stack.getCluster();
        try {
            cluster.setCreationStarted(new Date().getTime());
            cluster = clusterRepository.save(cluster);

            String blueprintText = cluster.getBlueprint().getBlueprintText();
            FileSystem fs = cluster.getFileSystem();
            if (fs != null) {
                blueprintText = extendBlueprintWithFsConfig(blueprintText, fs, stack);
            }

            blueprintText = blueprintProcessor.addConfigEntries(blueprintText, defaultConfigProvider.getDefaultConfigs(), false);

            TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
            setBaseRepoURL(cluster, ambariClient);
            addBlueprint(stack, ambariClient, blueprintText);
            int nodeCount = stack.getFullNodeCountWithoutDecommissionedNodes() - stack.getGateWayNodeCount();

            Set<HostGroup> hostGroups = hostGroupRepository.findHostGroupsInCluster(cluster.getId());
            Map<String, List<String>> hostGroupMappings = buildHostGroupAssociations(hostGroups);
            hostGroups = saveHostMetadata(cluster, hostGroupMappings);

            Set<HostMetadata> hostsInCluster = hostMetadataRepository.findHostsInCluster(cluster.getId());
            PollingResult waitForHostsResult = waitForHosts(stack, ambariClient, nodeCount, hostsInCluster);
            checkPollingResult(waitForHostsResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_HOST_JOIN_FAILED.code()));

            if (fs != null) {
                addFsRecipes(blueprintText, fs, hostGroups);
            }
            addHDFSRecipe(cluster, blueprintText, hostGroups);

            final boolean recipesFound = recipesFound(hostGroups);
            if (recipesFound) {
                recipeEngine.setupRecipes(stack, hostGroups);
                recipeEngine.executePreInstall(stack);
            }
            ambariClient.createCluster(cluster.getName(), cluster.getBlueprint().getBlueprintName(), hostGroupMappings);
            PollingResult pollingResult = waitForClusterInstall(stack, ambariClient);
            checkPollingResult(pollingResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_INSTALL_FAILED.code()));
            if (recipesFound) {
                recipeEngine.executePostInstall(stack);
            }
            executeSmokeTest(stack, ambariClient);
            createDefaultViews(ambariClient, blueprintText, hostGroups);
            cluster = handleClusterCreationSuccess(stack, cluster);
            return cluster;
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (HttpResponseException hre) {
            String errorMessage = AmbariClientExceptionUtil.getErrorMessage(hre);
            throw new AmbariOperationFailedException("Ambari could not create the cluster: " + errorMessage, hre);
        } catch (Exception e) {
            LOGGER.error("Error while building the Ambari cluster. Message {}, throwable: {}", e.getMessage(), e);
            throw new AmbariOperationFailedException(e.getMessage(), e);
        }
    }

    private void executeSmokeTest(Stack stack, AmbariClient ambariClient) {
        PollingResult pollingResult;
        pollingResult = runSmokeTest(stack, ambariClient);
        if (isExited(pollingResult)) {
            throw new CancellationException("Stack or cluster in delete in progress phase.");
        } else if (isFailure(pollingResult) || isTimeout(pollingResult)) {
            eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(),
                    cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_MR_SMOKE_FAILED.code()));
        }
    }

    private void checkPollingResult(PollingResult pollingResult, String message) throws ClusterException {
        if (isExited(pollingResult)) {
            throw new CancellationException("Stack or cluster in delete in progress phase.");
        } else if (isTimeout(pollingResult) || isFailure(pollingResult)) {
            throw new ClusterException(message);
        }
    }

    public Set<HostMetadata> installAmbariNode(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) throws CloudbreakSecuritySetupException {
        Set<String> successHosts = Collections.emptySet();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stackId, cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, cluster);
        int nodeCount = stack.getFullNodeCountWithoutDecommissionedAndUnRegisteredNodes() - stack.getGateWayNodeCount()
                + hostGroupAdjustment.getScalingAdjustment();
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupAdjustment.getHostGroup());
        List<String> hosts = findFreeHosts(stack.getId(), hostGroup, hostGroupAdjustment.getScalingAdjustment());
        Set<HostGroup> hostGroupAsSet = Sets.newHashSet(hostGroup);
        if (cluster.getFileSystem() != null) {
            addFsRecipes(cluster.getBlueprint().getBlueprintText(), cluster.getFileSystem(), hostGroupAsSet);
        }
        Set<HostMetadata> hostMetadata = addHostMetadata(cluster, hosts, hostGroupAdjustment);
        Set<HostMetadata> hostsInCluster = hostMetadataRepository.findHostsInCluster(cluster.getId());
        PollingResult pollingResult = waitForHosts(stack, ambariClient, nodeCount, hostsInCluster);
        if (isSuccess(pollingResult)) {
            final boolean recipesFound = recipesFound(hostGroupAsSet);
            if (!recipesFound || prepareAndExecuteRecipes(true, stack, hostGroup, hostMetadata)) {
                pollingResult = waitForAmbariOperations(stack, ambariClient, installServices(hosts, stack, ambariClient, hostGroupAdjustment));
                if (isSuccess(pollingResult)) {
                    pollingResult = waitForAmbariOperations(stack, ambariClient, singletonMap("START_SERVICES", ambariClient.startAllServices()));
                    if (isSuccess(pollingResult)) {
                        pollingResult = restartHadoopServices(stack, ambariClient, false);
                        if (isSuccess(pollingResult)) {
                            successHosts = new HashSet<>(hosts);
                            if (!prepareAndExecuteRecipes(false, stack, hostGroup, hostMetadata)) {
                                eventService.fireCloudbreakEvent(stackId, UPDATE_FAILED.name(), "Post recipe installation failed.");
                            }
                        }
                    }
                }
            }
        }
        updateFailedHostMetaData(successHosts, hostMetadata);
        return hostMetadata;
    }

    private boolean prepareAndExecuteRecipes(boolean preExecute, Stack stack, HostGroup hostGroup, Set<HostMetadata> hostMetadata) {
        try {
            if (preExecute) {
                recipeEngine.setupRecipesOnHosts(stack, hostGroup.getRecipes(), hostMetadata);
                recipeEngine.executePreInstall(stack, hostMetadata);
            } else {
                recipeEngine.executePostInstall(stack, hostMetadata);
            }
            return true;
        } catch (CloudbreakSecuritySetupException e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    private void updateFailedHostMetaData(Set<String> successHosts, Set<HostMetadata> hostMetadata) {
        for (HostMetadata metaData : hostMetadata) {
            if (!successHosts.contains(metaData.getHostName())) {
                metaData.setHostMetadataState(HostMetadataState.UNHEALTHY);
                hostMetadataRepository.save(metaData);
            }
        }
    }

    public AmbariClient getAmbariClientByStack(Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        return ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(),
                cluster.getPassword());
    }

    public Cluster resetAmbariCluster(Long stackId) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        InstanceGroup instanceGroupByType = stack.getGatewayInstanceGroup();
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        List<String> hostNames = instanceMetadataRepository.findAliveInstancesHostNamesInInstanceGroup(instanceGroupByType.getId());
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_RESETTING_AMBARI_DATABASE.code()));
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.RESET_AMBARI_DB_EVENT, DEFAULT_RECIPE_TIMEOUT, AMBARI_DB,
                Collections.<String>emptyList(), new HashSet<>(hostNames));
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_AMBARI_DATABASE_RESET.code()));
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_RESTARTING_AMBARI_SERVER.code()));
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.RESET_AMBARI_EVENT, DEFAULT_RECIPE_TIMEOUT, AMBARI_SERVER,
                Collections.<String>emptyList(), new HashSet<>(hostNames));
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_AMBARI_SERVER_RESTARTED.code()));
        return cluster;
    }

    public Cluster credentialChangeAmbariCluster(Long stackId, String newUserName, String newPassword) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        String oldUserName = cluster.getUserName();
        String oldPassword = cluster.getPassword();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, cluster);
        if (newUserName.equals(oldUserName)) {
            if (!newPassword.equals(oldPassword)) {
                ambariClient.changePassword(oldUserName, oldPassword, newPassword, true);
            }
        } else {
            ambariClient.createUser(newUserName, newPassword, true);
            ambariClient.deleteUser(oldUserName);
        }
        return cluster;
    }

    public Set<String> decommissionAmbariNodes(Long stackId, HostGroupAdjustmentJson adjustmentRequest, List<HostMetadata> decommissionCandidates)
            throws CloudbreakException {
        Set<String> result = new HashSet<>();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        LOGGER.info("Decommission requested");
        int adjustment = Math.abs(adjustmentRequest.getScalingAdjustment());
        String hostGroupName = adjustmentRequest.getHostGroup();
        LOGGER.info("Decommissioning {} hosts from host group '{}'", adjustment, hostGroupName);


        eventService.fireCloudbreakInstanceGroupEvent(stackId, Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_REMOVING_NODE_FROM_HOSTGROUP.code(), asList(adjustment, hostGroupName)), hostGroupName);
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stackId, cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, cluster);
        String blueprintName = stack.getCluster().getBlueprint().getBlueprintName();
        PollingResult pollingResult = startServiceIfNeeded(stack, ambariClient, blueprintName);
        if (isSuccess(pollingResult)) {
            Set<String> components = getHadoopComponents(cluster, ambariClient, hostGroupName, blueprintName);
            Map<String, HostMetadata> hostsToRemove = selectHostsToRemove(decommissionCandidates, stack, adjustment);
            List<String> hostList = new ArrayList<>(hostsToRemove.keySet());
            pollingResult = waitForAmbariOperations(stack, ambariClient, decommissionComponents(ambariClient, hostList, components));
            if (isSuccess(pollingResult)) {
                pollingResult = waitForDataNodeDecommission(stack, ambariClient);
                if (isSuccess(pollingResult)) {
                    pollingResult = waitForRegionServerDecommission(stack, ambariClient, hostList, components);
                    if (isSuccess(pollingResult)) {
                        pollingResult = stopHadoopComponents(stack, ambariClient, hostList);
                        if (isSuccess(pollingResult)) {
                            stopAndDeleteHosts(stack, ambariClient, hostList, components);
                            pollingResult = restartHadoopServices(stack, ambariClient, true);
                            if (isSuccess(pollingResult)) {
                                cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
                                HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupName);
                                hostGroup.getHostMetadata().removeAll(hostsToRemove.values());
                                hostGroupRepository.save(hostGroup);
                                result.addAll(hostsToRemove.keySet());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public void stopCluster(Stack stack) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
        try {
            if (!allServiceStopped(ambariClient.getHostComponentsStates())) {
                stopAllServices(stack, ambariClient);
            }
            stopAmbariAgents(stack, null);
        } catch (AmbariConnectionException ex) {
            LOGGER.debug("Ambari not running on the gateway machine, no need to stop it.");
        }
    }

    public void startCluster(Stack stack) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
        waitForAmbariToStart(stack);
        startAmbariAgents(stack);
        startAllServices(stack, ambariClient);
    }

    public boolean isAmbariAvailable(Stack stack) throws CloudbreakException {
        boolean result = false;
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
            AmbariClientPollerObject ambariClientPollerObject = new AmbariClientPollerObject(stack, ambariClient);
            try {
                result = ambariHealthCheckerTask.checkStatus(ambariClientPollerObject);
            } catch (Exception ex) {
                result = false;
            }
        }
        return result;
    }

    public boolean deleteHostFromAmbari(Stack stack, HostMetadata data) throws CloudbreakSecuritySetupException {
        boolean hostDeleted = false;
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), stack.getCluster().getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getSecureAmbariClient(clientConfig, stack.getCluster());
        Set<String> components = getHadoopComponents(stack.getCluster(), ambariClient, data.getHostGroup().getName(),
                stack.getCluster().getBlueprint().getBlueprintName());
        if (ambariClient.getClusterHosts().contains(data.getHostName())) {
            String hostState = ambariClient.getHostState(data.getHostName());
            if ("UNKNOWN".equals(hostState)) {
                deleteHosts(stack, asList(data.getHostName()), components);
                PollingResult result = restartHadoopServices(stack, ambariClient, true);
                if (isTimeout(result)) {
                    throw new AmbariOperationFailedException("Timeout while restarting Hadoop services.");
                } else if (isExited(result)) {
                    throw new CancellationException("Cluster was terminated while restarting Hadoop services.");
                }
                hostDeleted = true;
            }
        } else {
            LOGGER.debug("Host is already deleted.");
            hostDeleted = true;
        }
        return hostDeleted;
    }

    private String extendBlueprintWithFsConfig(String blueprintText, FileSystem fs, Stack stack) throws IOException {
        FileSystemConfigurator fsConfigurator = fileSystemConfigurators.get(FileSystemType.valueOf(fs.getType()));
        String json = JsonUtil.writeValueAsString(fs.getProperties());
        FileSystemConfiguration fsConfiguration = (FileSystemConfiguration) JsonUtil.readValue(json, FileSystemType.valueOf(fs.getType()).getClazz());
        decorateFsConfigurationProperties(fsConfiguration, stack);
        Map<String, String> resourceProperties = fsConfigurator.createResources(fsConfiguration);
        List<BlueprintConfigurationEntry> bpConfigEntries = fsConfigurator.getFsProperties(fsConfiguration, resourceProperties);
        if (fs.isDefaultFs()) {
            bpConfigEntries.addAll(fsConfigurator.getDefaultFsProperties(fsConfiguration));
        }
        return blueprintProcessor.addConfigEntries(blueprintText, bpConfigEntries, true);


    }

    private void decorateFsConfigurationProperties(FileSystemConfiguration fsConfiguration, Stack stack) {
        fsConfiguration.addProperty(FileSystemConfiguration.STORAGE_CONTAINER, "cloudbreak" + stack.getId());
        CloudPlatform stackPlatform = CloudPlatform.valueOf(stack.getPlatformVariant());
        if (AZURE_RM.equals(stackPlatform)) {
            String resourceGroupName = stack.getResourceByType(ResourceType.ARM_TEMPLATE).getResourceName();
            fsConfiguration.addProperty(FileSystemConfiguration.RESOURCE_GROUP_NAME, resourceGroupName);
        }
    }

    private void addFsRecipes(String blueprintText, FileSystem fs, Set<HostGroup> hostGroups) {
        FileSystemConfigurator fsConfigurator = fileSystemConfigurators.get(FileSystemType.valueOf(fs.getType()));
        List<RecipeScript> recipeScripts = fsConfigurator.getScripts();
        List<Recipe> fsRecipes = recipeBuilder.buildRecipes(recipeScripts, fs.getProperties());
        for (Recipe recipe : fsRecipes) {
            boolean oneNode = false;
            for (Entry<String, PluginExecutionType> pluginEntries : recipe.getPlugins().entrySet()) {
                if (PluginExecutionType.ONE_NODE.equals(pluginEntries.getValue())) {
                    oneNode = true;
                }
            }
            if (oneNode) {
                for (HostGroup hostGroup : hostGroups) {
                    if (isComponentPresent(blueprintText, "HDFS_CLIENT", hostGroup)) {
                        hostGroup.addRecipe(recipe);
                        break;
                    }
                }
            } else {
                for (HostGroup hostGroup : hostGroups) {
                    hostGroup.addRecipe(recipe);
                }
            }
        }
    }

    private void addHDFSRecipe(Cluster cluster, String blueprintText, Set<HostGroup> hostGroups) {
        try {
            for (HostGroup hostGroup : hostGroups) {
                if (isComponentPresent(blueprintText, "HDFS_CLIENT", hostGroup)) {
                    String script = FileReaderUtils.readFileFromClasspath("scripts/hdfs-home.sh").replaceAll("\\$USER", cluster.getUserName());
                    RecipeScript recipeScript = new RecipeScript(script, ClusterLifecycleEvent.POST_INSTALL, PluginExecutionType.ONE_NODE);
                    Recipe recipe = recipeBuilder.buildRecipes(asList(recipeScript), Collections.<String, String>emptyMap()).get(0);
                    hostGroup.addRecipe(recipe);
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot create HDFS home dir recipe", e);
        }
    }

    private boolean isComponentPresent(String blueprint, String component, HostGroup hostGroup) {
        return isComponentPresent(blueprint, component, Sets.newHashSet(hostGroup));
    }

    private boolean isComponentPresent(String blueprint, String component, Set<HostGroup> hostGroups) {
        for (HostGroup hostGroup : hostGroups) {
            Set<String> components = blueprintProcessor.getComponentsInHostGroup(blueprint, hostGroup.getName());
            if (components.contains(component)) {
                return true;
            }
        }
        return false;
    }

    private void tryToDeleteHosts(Stack stack, Set<String> components, List<String> hostList) {
        try {
            deleteHosts(stack, hostList, components);
        } catch (Exception e) {
            String msg = "Could not delete host(s) from Ambari! ";
            if (e instanceof HttpResponseException) {
                String errorMessage = AmbariClientExceptionUtil.getErrorMessage((HttpResponseException) e);
                msg += errorMessage;
            }
            LOGGER.warn(msg, e);
        }
    }

    private void stopAndDeleteHosts(Stack stack, AmbariClient ambariClient, List<String> hostList, Set<String> components) throws CloudbreakException {
        stopAmbariAgents(stack, new HashSet<>(hostList));
        PollingResult pollingResult = waitForHostsToLeave(stack, ambariClient, hostList);
        if (isTimeout(pollingResult)) {
            LOGGER.warn("Ambari agent stop timed out, delete the hosts anyway, hosts: {}", hostList);
        }
        if (!isExited(pollingResult)) {
            deleteHosts(stack, hostList, components);
        }
    }

    private void deleteHosts(Stack stack, List<String> hostList, Set<String> components) throws CloudbreakSecuritySetupException {
        ambariHostsRemover.deleteHosts(stack, hostList, new ArrayList<>(components));
    }

    private void stopAllServices(Stack stack, AmbariClient ambariClient) throws CloudbreakException {
        int requestId = ambariClient.stopAllServices();
        if (requestId != -1) {
            LOGGER.info("Waiting for Hadoop services to stop on stack");
            PollingResult servicesStopResult = ambariOperationService.waitForAmbariOperations(stack, ambariClient, singletonMap("stop services", requestId));
            if (isExited(servicesStopResult)) {
                throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
            } else if (isTimeout(servicesStopResult)) {
                throw new CloudbreakException("Timeout while stopping Ambari services.");
            }
        } else {
            throw new CloudbreakException("Failed to stop Hadoop services.");
        }
    }

    private void startAllServices(Stack stack, AmbariClient ambariClient) throws CloudbreakException {
        int requestId = ambariClient.startAllServices();
        if (requestId != -1) {
            LOGGER.info("Waiting for Hadoop services to start on stack");
            PollingResult servicesStartResult = ambariOperationService.waitForAmbariOperations(stack, ambariClient, singletonMap("start services", requestId));
            if (isExited(servicesStartResult)) {
                throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
            } else if (isTimeout(servicesStartResult)) {
                throw new CloudbreakException("Timeout while starting Ambari services.");
            }
        } else {
            throw new CloudbreakException("Failed to start Hadoop services.");
        }
    }

    private Set<String> getHadoopComponents(Cluster cluster, AmbariClient ambariClient, String hostGroupName, String blueprintName) {
        Set<String> components = new HashSet<>(ambariClient.getComponentsCategory(blueprintName, hostGroupName).keySet());
        if (cluster.isSecure()) {
            components.add(ClusterSecurityService.KERBEROS_CLIENT);
        }
        return components;
    }

    private Cluster handleClusterCreationSuccess(Stack stack, Cluster cluster) {
        LOGGER.info("Cluster created successfully. Cluster name: {}", cluster.getName());
        cluster.setCreationFinished(new Date().getTime());
        cluster.setUpSince(new Date().getTime());
        cluster = clusterRepository.save(cluster);
        List<InstanceMetaData> updatedInstances = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            Set<InstanceMetaData> instances = instanceGroup.getAllInstanceMetaData();
            for (InstanceMetaData instanceMetaData : instances) {
                if (!instanceMetaData.isTerminated()) {
                    instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
                    updatedInstances.add(instanceMetaData);
                }
            }
        }
        instanceMetadataRepository.save(updatedInstances);
        return cluster;

    }

    private List<String> getHostNames(Set<InstanceMetaData> instances) {
        return FluentIterable.from(instances).transform(new Function<InstanceMetaData, String>() {
            @Nullable
            @Override
            public String apply(@Nullable InstanceMetaData input) {
                return input.getDiscoveryFQDN();
            }
        }).toList();
    }

    private boolean recipesFound(Set<HostGroup> hostGroups) {
        for (HostGroup hostGroup : hostGroups) {
            if (!hostGroup.getRecipes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Map<String, HostMetadata> selectHostsToRemove(List<HostMetadata> decommissionCandidates, Stack stack, int adjustment) {
        Map<String, HostMetadata> hostsToRemove = new HashMap<>();
        int i = 0;
        for (HostMetadata hostMetadata : decommissionCandidates) {
            String hostName = hostMetadata.getHostName();
            InstanceMetaData instanceMetaData = instanceMetadataRepository.findHostInStack(stack.getId(), hostName);
            if (!instanceMetaData.getAmbariServer()) {
                if (i < adjustment) {
                    LOGGER.info("Host '{}' will be removed from Ambari cluster", hostName);
                    hostsToRemove.put(hostName, hostMetadata);
                } else {
                    break;
                }
                i++;
            }
        }
        return hostsToRemove;
    }

    private Map<String, Integer> decommissionComponents(AmbariClient ambariClient, List<String> hosts, Set<String> components) {
        Map<String, Integer> decommissionRequests = new HashMap<>();
        if (components.contains("NODEMANAGER")) {
            int requestId = ambariClient.decommissionNodeManagers(hosts);
            decommissionRequests.put("NODEMANAGER_DECOMMISSION", requestId);
        }
        if (components.contains("DATANODE")) {
            int requestId = ambariClient.decommissionDataNodes(hosts);
            decommissionRequests.put("DATANODE_DECOMMISSION", requestId);
        }
        if (components.contains("HBASE_REGIONSERVER")) {
            ambariClient.decommissionHBaseRegionServers(hosts);
            ambariClient.setHBaseRegionServersToMaintenance(hosts, true);
        }
        return decommissionRequests;
    }

    private PollingResult runSmokeTest(Stack stack, AmbariClient ambariClient) {
        int id = ambariClient.runMRServiceCheck();
        return waitForAmbariOperations(stack, ambariClient, singletonMap("MR_SMOKE_TEST", id));
    }

    private PollingResult stopHadoopComponents(Stack stack, AmbariClient ambariClient, List<String> hosts) {
        try {
            int requestId = ambariClient.stopAllComponentsOnHosts(hosts);
            return waitForAmbariOperations(stack, ambariClient, singletonMap("Stopping components on the decommissioned hosts", requestId));
        } catch (HttpResponseException e) {
            String errorMessage = AmbariClientExceptionUtil.getErrorMessage(e);
            throw new AmbariOperationFailedException("Ambari could not stop components. " + errorMessage, e);
        }
    }

    private PollingResult restartHadoopServices(Stack stack, AmbariClient ambariClient, boolean decommissioned) {
        Map<String, Integer> restartRequests = new HashMap<>();
        Map<String, Map<String, String>> serviceComponents = ambariClient.getServiceComponentsMap();
        if (decommissioned) {
            restartRequests.put("ZOOKEEPER", restartComponentWithClient(ambariClient, "ZOOKEEPER", "ZOOKEEPER_SERVER"));
        }
        if (serviceComponents.containsKey("NAGIOS")) {
            restartRequests.put("NAGIOS", restartComponentWithClient(ambariClient, "NAGIOS", "NAGIOS_SERVER"));
        }
        if (serviceComponents.containsKey("GANGLIA")) {
            restartRequests.put("GANGLIA", restartComponentWithClient(ambariClient, "GANGLIA", "GANGLIA_SERVER"));
        }
        return waitForAmbariOperations(stack, ambariClient, restartRequests);
    }

    private int restartComponentWithClient(AmbariClient ambariClient, String service, String... compontents) {
        return ambariClient.restartServiceComponents(service, asList(compontents));
    }

    private PollingResult startServiceIfNeeded(Stack stack, AmbariClient ambariClient, String blueprint) throws CloudbreakException {
        Map<String, Integer> stringIntegerMap = new HashMap<>();
        Map<String, String> componentsCategory = ambariClient.getComponentsCategory(blueprint);
        Map<String, Map<String, String>> hostComponentsStates = ambariClient.getHostComponentsStates();
        Set<String> services = new HashSet<>();
        collectServicesToStart(componentsCategory, hostComponentsStates, services);
        if (!services.isEmpty()) {
            if (services.contains("HDFS")) {
                int requestId = ambariClient.startService("HDFS");
                stringIntegerMap.put("HDFS_START", requestId);
            }
            if (services.contains("HBASE")) {
                int requestId = ambariClient.startService("HBASE");
                stringIntegerMap.put("HBASE_START", requestId);
            }
        }

        if (!stringIntegerMap.isEmpty()) {
            return waitForAmbariOperations(stack, ambariClient, stringIntegerMap);
        } else {
            return SUCCESS;
        }
    }

    private void collectServicesToStart(Map<String, String> componentsCategory, Map<String, Map<String, String>> hostComponentsStates, Set<String> services) {
        for (Entry<String, Map<String, String>> hostComponentsEntry : hostComponentsStates.entrySet()) {
            Map<String, String> componentStateMap = hostComponentsEntry.getValue();
            for (Entry<String, String> componentStateEntry : componentStateMap.entrySet()) {
                String componentKey = componentStateEntry.getKey();
                String category = componentsCategory.get(componentKey);
                if (!"CLIENT".equals(category)) {
                    if (!"STARTED".equals(componentStateEntry.getValue())) {
                        if ("NODEMANAGER".equals(componentKey) || "DATANODE".equals(componentKey)) {
                            services.add("HDFS");
                        } else if ("HBASE_REGIONSERVER".equals(componentKey)) {
                            services.add("HBASE");
                        } else {
                            LOGGER.info("No need to restart ambari service: {}", componentKey);
                        }
                    } else {
                        LOGGER.info("Ambari service already running: {}", componentKey);
                    }
                }
            }
        }
    }

    private void waitForAmbariToStart(Stack stack) throws CloudbreakException {
        LOGGER.info("Checking if Ambari Server is available.");
        Cluster cluster = stack.getCluster();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        PollingResult ambariHealthCheckResult = ambariHealthChecker.pollWithTimeout(
                ambariHealthCheckerTask,
                new AmbariClientPollerObject(stack, ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword())),
                AMBARI_POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_HOSTS,
                AmbariOperationService.MAX_FAILURE_COUNT);
        if (isExited(ambariHealthCheckResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Ambari to start.");
        } else if (isTimeout(ambariHealthCheckResult)) {
            throw new CloudbreakException("Ambari server was not restarted properly.");
        }
    }

    private void stopAmbariAgents(Stack stack, Set<String> hosts) throws CloudbreakException {
        LOGGER.info("Stopping Ambari agents on hosts: {}", hosts == null || hosts.isEmpty() ? "all" : hosts);
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.STOP_AMBARI_EVENT,
                DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT, Collections.<String>emptyList(), hosts);
    }

    private void startAmbariAgents(Stack stack) throws CloudbreakException {
        LOGGER.info("Starting Ambari agents on the hosts.");
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.START_AMBARI_EVENT, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT);
        PollingResult hostsJoinedResult = waitForHostsToJoin(stack);
        if (PollingResult.EXIT.equals(hostsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while starting Ambari agents.");
        } else if (PollingResult.TIMEOUT.equals(hostsJoinedResult)) {
            LOGGER.info("Ambari agents couldn't join. Restarting ambari agents...");
            restartAmbariAgents(stack);
        }
    }

    private void restartAmbariAgents(Stack stack) throws CloudbreakException {
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.RESTART_AMBARI_EVENT, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT);
        PollingResult hostsJoinedResult = waitForHostsToJoin(stack);
        if (isExited(hostsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while restarting Ambari agents.");
        } else if (isTimeout(hostsJoinedResult)) {
            throw new CloudbreakException("Services could not start because host(s) could not join.");
        }
    }

    private PollingResult waitForHostsToJoin(Stack stack) throws CloudbreakSecuritySetupException {
        Set<HostMetadata> hostsInCluster = hostMetadataRepository.findHostsInCluster(stack.getCluster().getId());
        AmbariHostsCheckerContext ambariHostsCheckerContext =
                new AmbariHostsCheckerContext(stack, getAmbariClientByStack(stack), hostsInCluster, stack.getFullNodeCount());
        return ambariHostJoin.pollWithTimeout(
                ambariHostsJoinStatusCheckerTask,
                ambariHostsCheckerContext,
                AMBARI_POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_HOSTS,
                AmbariOperationService.MAX_FAILURE_COUNT);
    }

    private boolean allServiceStopped(Map<String, Map<String, String>> hostComponentsStates) {
        boolean stopped = true;
        Collection<Map<String, String>> values = hostComponentsStates.values();
        for (Map<String, String> value : values) {
            for (String state : value.values()) {
                if (!"INSTALLED".equals(state)) {
                    stopped = false;
                }
            }
        }
        return stopped;
    }

    private Set<HostGroup> saveHostMetadata(Cluster cluster, Map<String, List<String>> hostGroupMappings) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (Entry<String, List<String>> hostGroupMapping : hostGroupMappings.entrySet()) {
            Set<HostMetadata> hostMetadata = new HashSet<>();
            HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupMapping.getKey());
            for (String hostName : hostGroupMapping.getValue()) {
                HostMetadata hostMetadataEntry = new HostMetadata();
                hostMetadataEntry.setHostName(hostName);
                hostMetadataEntry.setHostGroup(hostGroup);
                hostMetadata.add(hostMetadataEntry);
            }
            hostGroup.setHostMetadata(hostMetadata);
            hostGroups.add(hostGroupRepository.save(hostGroup));
        }
        return hostGroups;
    }

    private Set<HostMetadata> addHostMetadata(Cluster cluster, List<String> hosts, HostGroupAdjustmentJson hostGroupAdjustment) {
        Set<HostMetadata> hostMetadata = new HashSet<>();
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupAdjustment.getHostGroup());
        for (String host : hosts) {
            HostMetadata hostMetadataEntry = new HostMetadata();
            hostMetadataEntry.setHostName(host);
            hostMetadataEntry.setHostGroup(hostGroup);
            hostMetadataRepository.save(hostMetadataEntry);
            hostMetadata.add(hostMetadataEntry);
        }
        hostGroup.getHostMetadata().addAll(hostMetadata);
        hostGroupRepository.save(hostGroup);
        return hostMetadata;
    }

    private void setBaseRepoURL(Cluster cluster, AmbariClient ambariClient) {
        AmbariStackDetails ambStack = cluster.getAmbariStackDetails();
        if (ambStack != null) {
            LOGGER.info("Use specific Ambari repository: {}", ambStack);
            try {
                String stack = ambStack.getStack();
                String version = ambStack.getVersion();
                String os = ambStack.getOs();
                boolean verify = ambStack.isVerify();
                addRepository(ambariClient, stack, version, os, ambStack.getStackRepoId(), ambStack.getStackBaseURL(), verify);
                addRepository(ambariClient, stack, version, os, ambStack.getUtilsRepoId(), ambStack.getUtilsBaseURL(), verify);
            } catch (HttpResponseException e) {
                String exceptionErrorMsg = AmbariClientExceptionUtil.getErrorMessage(e);
                String msg = String.format("Cannot use the specified Ambari stack: %s. Error: %s", ambStack.toString(), exceptionErrorMsg);
                throw new BadRequestException(msg, e);
            }
        } else {
            LOGGER.info("Using latest HDP repository");
        }
    }

    private void addRepository(AmbariClient client, String stack, String version, String os,
            String repoId, String repoUrl, boolean verify) throws HttpResponseException {
        client.addStackRepository(stack, version, os, repoId, repoUrl, verify);
    }

    private void addBlueprint(Stack stack, AmbariClient ambariClient, String blueprintText) {
        try {
            Cluster cluster = stack.getCluster();
            Map<String, Map<String, Map<String, String>>> hostGroupConfig = hadoopConfigurationService.getHostGroupConfiguration(cluster);
            blueprintText = ambariClient.extendBlueprintHostGroupConfiguration(blueprintText, hostGroupConfig);
            Map<String, Map<String, String>> globalConfig = hadoopConfigurationService.getGlobalConfiguration(cluster);
            blueprintText = ambariClient.extendBlueprintGlobalConfiguration(blueprintText, globalConfig);
            ambariClient.addBlueprint(blueprintText);
            LOGGER.info("Blueprint added to Ambari.");
        } catch (IOException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Ambari blueprint already exists.", e);
            } else if (e instanceof HttpResponseException) {
                String errorMessage = AmbariClientExceptionUtil.getErrorMessage((HttpResponseException) e);
                throw new CloudbreakServiceException("Ambari Blueprint could not be added: " + errorMessage, e);
            } else {
                throw new CloudbreakServiceException(e);
            }
        }
    }

    private PollingResult waitForHosts(Stack stack, AmbariClient ambariClient, int nodeCount, Set<HostMetadata> hostsInCluster) {
        LOGGER.info("Waiting for hosts to connect.[Ambari server address: {}]", stack.getAmbariIp());
        return hostsPollingService.pollWithTimeoutSingleFailure(
                ambariHostsStatusCheckerTask, new AmbariHostsCheckerContext(stack, ambariClient, hostsInCluster, nodeCount),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS);
    }

    private Map<String, List<String>> buildHostGroupAssociations(Set<HostGroup> hostGroups) throws InvalidHostGroupHostAssociation {
        Map<String, List<String>> hostGroupMappings = new HashMap<>();
        LOGGER.info("Computing host - hostGroup mappings based on hostGroup - instanceGroup associations");
        for (HostGroup hostGroup : hostGroups) {
            hostGroupMappings.put(hostGroup.getName(),
                    instanceMetadataRepository.findAliveInstancesHostNamesInInstanceGroup(hostGroup.getInstanceGroup().getId()));
        }
        LOGGER.info("Computed host-hostGroup associations: {}", hostGroupMappings);
        return hostGroupMappings;
    }

    private PollingResult waitForClusterInstall(Stack stack, AmbariClient ambariClient) {
        Map<String, Integer> clusterInstallRequest = new HashMap<>();
        clusterInstallRequest.put("CLUSTER_INSTALL", 1);
        return waitForAmbariOperations(stack, ambariClient, clusterInstallRequest);
    }

    private List<String> findFreeHosts(Long stackId, HostGroup hostGroup, int scalingAdjustment) {
        Set<InstanceMetaData> unregisteredHosts = instanceMetadataRepository.findUnregisteredHostsInInstanceGroup(hostGroup.getInstanceGroup().getId());
        Set<InstanceMetaData> instances = FluentIterable.from(unregisteredHosts).limit(scalingAdjustment).toSet();
        eventService.fireCloudbreakInstanceGroupEvent(stackId, Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_ADDING_NODE_TO_HOSTGROUP.code(),
                        asList(instances.size(), hostGroup.getName())), hostGroup.getName());
        return getHostNames(instances);
    }

    private Map<String, Integer> installServices(List<String> hosts, Stack stack, AmbariClient ambariClient, HostGroupAdjustmentJson hostGroup) {
        try {
            Map<String, Integer> requests = new HashMap<>();
            Cluster cluster = stack.getCluster();
            ambariClient.addHosts(hosts);
            String blueprintName = cluster.getBlueprint().getBlueprintName();
            String hGroupName = hostGroup.getHostGroup();
            ambariClient.addComponentsToHosts(hosts, blueprintName, hGroupName);
            ambariClient.addHostsToConfigGroups(hosts, hGroupName);
            if (cluster.isSecure()) {
                ambariClient.addComponentsToHosts(hosts, asList(securityService.KERBEROS_CLIENT));
            }
            requests.put("Install components to the new hosts", ambariClient.installAllComponentsOnHosts(hosts));
            if (cluster.isSecure()) {
                requests.put("Re-generate missing keytabs", ambariClient.generateKeytabs(true));
            }
            return requests;
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Host already exists.", e);
            } else {
                String errorMessage = AmbariClientExceptionUtil.getErrorMessage(e);
                throw new CloudbreakServiceException("Ambari could not install services. " + errorMessage, e);
            }
        }
    }

    private PollingResult waitForHostsToLeave(Stack stack, AmbariClient ambariClient, List<String> hostNames) throws CloudbreakSecuritySetupException {
        return ambariHostLeave.pollWithTimeout(hostsLeaveStatusCheckerTask, new AmbariHostsWithNames(stack, ambariClient, hostNames),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS, AmbariOperationService.MAX_FAILURE_COUNT);
    }

    private PollingResult waitForAmbariOperations(Stack stack, AmbariClient ambariClient, Map<String, Integer> operationRequests) {
        LOGGER.info("Waiting for Ambari operations to finish. [Operation requests: {}]", operationRequests);
        return ambariOperationService.waitForAmbariOperations(stack, ambariClient, operationRequests);
    }

    private PollingResult waitForDataNodeDecommission(Stack stack, AmbariClient ambariClient) {
        LOGGER.info("Waiting for DataNodes to move the blocks to other nodes. stack id: {}", stack.getId());
        return ambariOperationService.waitForAmbariOperations(stack, ambariClient, dnDecommissionStatusCheckerTask, Collections.<String, Integer>emptyMap());
    }

    private PollingResult waitForRegionServerDecommission(Stack stack, AmbariClient ambariClient, List<String> hosts, Set<String> components) {
        if (!components.contains("HBASE_REGIONSERVER")) {
            return SUCCESS;
        }
        LOGGER.info("Waiting for RegionServers to move the regions to other servers");
        return rsPollerService.pollWithTimeoutSingleFailure(
                rsDecommissionStatusCheckerTask,
                new AmbariHostsWithNames(stack, ambariClient, hosts),
                AMBARI_POLLING_INTERVAL,
                MAX_ATTEMPTS_FOR_REGION_DECOM);
    }

    private void createDefaultViews(AmbariClient ambariClient, String blueprintText, Set<HostGroup> hostGroups) {
        try {
            ambariClient.createFilesView();
            if (isComponentPresent(blueprintText, "PIG", hostGroups)) {
                ambariClient.createPigView();
            }
            if (isComponentPresent(blueprintText, "HIVE_SERVER", hostGroups)) {
                ambariClient.createHiveView();
            }
        } catch (Exception e) {
            LOGGER.warn("Could not create default views", e);
        }
    }

}
