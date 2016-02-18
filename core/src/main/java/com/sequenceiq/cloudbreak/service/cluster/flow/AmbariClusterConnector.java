package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_DB;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_SERVER;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isFailure;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.PollingResult.isTimeout;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.INSTALL_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.SMOKE_TEST_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.START_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.START_OPERATION_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.STOP_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.DEFAULT_RECIPE_TIMEOUT;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.PluginExecutionType;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakRecipeSetupException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.ClusterException;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.SssdConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.TopologyRecord;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
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
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterConnector.class);
    private static final String REALM = "NODE.DC1.CONSUL";
    private static final String DOMAIN = "node.dc1.consul";
    private static final String KEY_TYPE = "PERSISTED";
    private static final String PRINCIPAL = "/admin";
    private static final String FQDN = "fqdn";
    private static final String UPSCALE_REQUEST_CONTEXT = "Logical Request: Scale Cluster";
    private static final String UPSCALE_REQUEST_STATUS = "IN_PROGRESS";
    private static final String SSSD_CONFIG = "sssd-config-";
    private static final String SWARM = "SWARM";


    @Inject
    private StackRepository stackRepository;
    @Inject
    private ClusterRepository clusterRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;
    @Inject
    private HostGroupService hostGroupService;
    @Inject
    private AmbariOperationService ambariOperationService;
    @Inject
    private PollingService<AmbariHostsCheckerContext> hostsPollingService;
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
    private PollingService<AmbariHostsCheckerContext> ambariHostJoin;
    @Inject
    private PollingService<AmbariClientPollerObject> ambariHealthChecker;
    @Inject
    private AmbariHealthCheckerTask ambariHealthCheckerTask;
    @Inject
    private AmbariHostsJoinStatusCheckerTask ambariHostsJoinStatusCheckerTask;
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
    @Inject
    private TlsSecurityService tlsSecurityService;

    private enum Msg {
        AMBARI_CLUSTER_RESETTING_AMBARI_DATABASE("ambari.cluster.resetting.ambari.database"),
        AMBARI_CLUSTER_AMBARI_DATABASE_RESET("ambari.cluster.ambari.database.reset"),
        AMBARI_CLUSTER_RESTARTING_AMBARI_SERVER("ambari.cluster.restarting.ambari.server"),
        AMBARI_CLUSTER_RESTARTING_AMBARI_AGENT("ambari.cluster.restarting.ambari.agent"),
        AMBARI_CLUSTER_AMBARI_AGENT_RESTARTED("ambari.cluster.ambari.agent.restarted"),
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

            AmbariClient ambariClient = getAmbariClient(stack);
            setBaseRepoURL(cluster, ambariClient);
            addBlueprint(stack, ambariClient, blueprintText);
            Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
            Map<String, List<Map<String, String>>> hostGroupMappings = buildHostGroupAssociations(hostGroups);

            Set<HostMetadata> hostsInCluster = hostMetadataRepository.findHostsInCluster(cluster.getId());
            int nodeCount = hostsInCluster.size();
            PollingResult waitForHostsResult = waitForHosts(stack, ambariClient, nodeCount, hostsInCluster);
            checkPollingResult(waitForHostsResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_HOST_JOIN_FAILED.code()));

            executeSssdRecipe(stack, null, cluster.getSssdConfig());

            final boolean recipesFound = recipesPreInstall(stack, cluster, blueprintText, fs, hostGroups);
            String clusterName = cluster.getName();
            String blueprintName = cluster.getBlueprint().getBlueprintName();
            String configStrategy = cluster.getConfigStrategy().name();
            if (cluster.isSecure()) {
                ambariClient.createSecureCluster(clusterName, blueprintName,
                        hostGroupMappings, configStrategy, cluster.getKerberosAdmin() + PRINCIPAL, cluster.getKerberosPassword(), KEY_TYPE);
            } else {
                ambariClient.createCluster(clusterName, blueprintName, hostGroupMappings, configStrategy);
            }
            PollingResult pollingResult = ambariOperationService.waitForOperationsToStart(stack, ambariClient, singletonMap("INSTALL_START", 1),
                    START_OPERATION_STATE);
            checkPollingResult(pollingResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_INSTALL_FAILED.code()));
            pollingResult = waitForClusterInstall(stack, ambariClient);
            checkPollingResult(pollingResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_INSTALL_FAILED.code()));
            recipesPostInstall(stack, recipesFound);
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

    private boolean recipesPreInstall(Stack stack, Cluster cluster, String blueprintText, FileSystem fs, Set<HostGroup> hostGroups)
            throws CloudbreakSecuritySetupException, CloudbreakRecipeSetupException {
        if (SWARM.equals(cluster.getStack().getOrchestrator().getType())) {
            if (fs != null) {
                addFsRecipes(blueprintText, fs, hostGroups);
            }
            addHDFSRecipe(cluster, blueprintText, hostGroups);
        }

        final boolean recipesFound = recipesFound(hostGroups);
        if (recipesFound) {
            recipeEngine.setupRecipes(stack, hostGroups);
            recipeEngine.executePreInstall(stack);
        }
        return recipesFound;
    }

    private void recipesPostInstall(Stack stack, boolean recipesFound) throws CloudbreakSecuritySetupException, CloudbreakRecipeSetupException {
        if (recipesFound) {
            recipeEngine.executePostInstall(stack);
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

    public Set<HostMetadata> installAmbariNode(Long stackId, Set<HostMetadata> hostMetadata, String hostGroupName) throws CloudbreakSecuritySetupException {
        Set<String> successHosts = Collections.emptySet();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        AmbariClient ambariClient = getSecureAmbariClient(stack);
        Set<HostMetadata> hostsInCluster = hostMetadataRepository.findHostsInCluster(cluster.getId());
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), hostGroupName);
        Set<HostGroup> hostGroupAsSet = Sets.newHashSet(hostGroup);
        if (cluster.getFileSystem() != null) {
            addFsRecipes(cluster.getBlueprint().getBlueprintText(), cluster.getFileSystem(), hostGroupAsSet);
        }
        PollingResult pollingResult = waitForHosts(stack, ambariClient, hostsInCluster.size(), hostsInCluster);
        if (isSuccess(pollingResult)) {
            List<String> upscaleHostNames = FluentIterable.from(hostMetadata).transform(new Function<HostMetadata, String>() {
                @Nullable
                @Override
                public String apply(HostMetadata input) {
                    return input.getHostName();
                }
            }).toList();
            if (cluster.getSssdConfig() != null) {
                executeSssdRecipe(stack, new HashSet<>(upscaleHostNames), cluster.getSssdConfig());
            }
            final boolean recipesFound = recipesFound(hostGroupAsSet);
            if (!recipesFound || prepareAndExecuteRecipes(true, stack, hostGroup, hostMetadata)) {
                pollingResult = ambariOperationService.waitForOperations(stack, ambariClient,
                        installServices(upscaleHostNames, stack, ambariClient, hostGroupName), INSTALL_AMBARI_PROGRESS_STATE);
                if (isSuccess(pollingResult)) {
                    successHosts = new HashSet<>(upscaleHostNames);
                    if (recipesFound && !prepareAndExecuteRecipes(false, stack, hostGroup, hostMetadata)) {
                        eventService.fireCloudbreakEvent(stackId, UPDATE_FAILED.name(), "Post recipe installation failed.");
                    }
                }
            }
        }
        updateFailedHostMetaData(successHosts, hostMetadata);
        return hostMetadata;
    }

    private void executeSssdRecipe(Stack stack, Set<String> hosts, SssdConfig sssdConfig) throws CloudbreakSecuritySetupException {
        if (sssdConfig != null) {
            SssdConfig config = stack.getCluster().getSssdConfig();
            List<String> payload;
            if (config.getConfiguration() != null) {
                Map<String, String> keyValues = new HashMap<>();
                String configName = SSSD_CONFIG + config.getId();
                keyValues.put(configName, config.getConfiguration());
                InstanceGroup gateway = stack.getGatewayInstanceGroup();
                InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
                HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), gatewayInstance.getPublicIp());
                pluginManager.prepareKeyValues(httpClientConfig, keyValues);
                payload = Arrays.asList(configName);
            } else {
                payload = Arrays.asList("-", config.getProviderType().getType(), config.getUrl(), config.getSchema().getRepresentation(),
                        config.getBaseSearch(), config.getTlsReqcert().getRepresentation(), config.getAdServer(),
                        config.getKerberosServer(), config.getKerberosRealm());
            }
            pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.SSSD_SETUP, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT, payload, hosts);
        }
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

    private AmbariClient getAmbariClient(Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        return ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
    }

    private AmbariClient getSecureAmbariClient(Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        return ambariClientProvider.getSecureAmbariClient(clientConfig, cluster);
    }

    public Cluster resetAmbariCluster(Long stackId) throws CloudbreakException {
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
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_RESTARTING_AMBARI_AGENT.code()));
        restartAmbariAgents(stack);
        eventService.fireCloudbreakEvent(stackId, Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_AMBARI_AGENT_RESTARTED.code()));
        return cluster;
    }

    public Cluster credentialChangeAmbariCluster(Long stackId, String newUserName, String newPassword) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        String oldUserName = cluster.getUserName();
        String oldPassword = cluster.getPassword();
        AmbariClient ambariClient = getSecureAmbariClient(stack);
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

    public void stopCluster(Stack stack) throws CloudbreakException {
        AmbariClient ambariClient = getAmbariClient(stack);
        try {
            if (!allServiceStopped(ambariClient.getHostComponentsStates())) {
                stopAllServices(stack, ambariClient);
            }
            // TODO: ambari agent containers should be stopped through the orchestrator API
            if (!"BYOS".equals(stack.cloudPlatform())) {
                stopAmbariAgents(stack, null);
            }
        } catch (AmbariConnectionException ex) {
            LOGGER.debug("Ambari not running on the gateway machine, no need to stop it.");
        }
    }

    public void startCluster(Stack stack) throws CloudbreakException {
        AmbariClient ambariClient = getAmbariClient(stack);
        waitForAmbariToStart(stack);
        if (!"BYOS".equals(stack.cloudPlatform())) {
            startAmbariAgents(stack);
        }
        startAllServices(stack, ambariClient);
    }

    public boolean isAmbariAvailable(Stack stack) throws CloudbreakException {
        boolean result = false;
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            AmbariClient ambariClient = getAmbariClient(stack);
            AmbariClientPollerObject ambariClientPollerObject = new AmbariClientPollerObject(stack, ambariClient);
            try {
                result = ambariHealthCheckerTask.checkStatus(ambariClientPollerObject);
            } catch (Exception ex) {
                result = false;
            }
        }
        return result;
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
        if (CloudConstants.AZURE_RM.equals(stack.getPlatformVariant())) {
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

    private void stopAllServices(Stack stack, AmbariClient ambariClient) throws CloudbreakException {
        int requestId = ambariClient.stopAllServices();
        if (requestId != -1) {
            LOGGER.info("Waiting for Hadoop services to stop on stack");
            PollingResult servicesStopResult = ambariOperationService.waitForOperations(stack, ambariClient, singletonMap("stop services", requestId),
                    STOP_AMBARI_PROGRESS_STATE);
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
            PollingResult servicesStartResult = ambariOperationService.waitForOperations(stack, ambariClient, singletonMap("start services", requestId),
                    START_AMBARI_PROGRESS_STATE);
            if (isExited(servicesStartResult)) {
                throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
            } else if (isTimeout(servicesStartResult)) {
                throw new CloudbreakException("Timeout while starting Ambari services.");
            }
        } else {
            throw new CloudbreakException("Failed to start Hadoop services.");
        }
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
        List<HostMetadata> hostMetadata = new ArrayList<>();
        for (HostMetadata host : hostMetadataRepository.findHostsInCluster(cluster.getId())) {
            host.setHostMetadataState(HostMetadataState.HEALTHY);
            hostMetadata.add(host);
        }
        hostMetadataRepository.save(hostMetadata);
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

    private PollingResult runSmokeTest(Stack stack, AmbariClient ambariClient) {
        int id = ambariClient.runMRServiceCheck();
        return ambariOperationService.waitForOperations(stack, ambariClient, singletonMap("MR_SMOKE_TEST", id), SMOKE_TEST_AMBARI_PROGRESS_STATE);
    }

    private void waitForAmbariToStart(Stack stack) throws CloudbreakException {
        LOGGER.info("Checking if Ambari Server is available.");
        AmbariClient ambariClient = getAmbariClient(stack);
        PollingResult ambariHealthCheckResult = ambariHealthChecker.pollWithTimeout(
                ambariHealthCheckerTask,
                new AmbariClientPollerObject(stack, ambariClient),
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
                new AmbariHostsCheckerContext(stack, getAmbariClient(stack), hostsInCluster, stack.getFullNodeCount());
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
            if (cluster.isSecure()) {
                String gatewayHost = cluster.getAmbariIp();
                if (stack.getInstanceGroups() != null && !stack.getInstanceGroups().isEmpty()) {
                    InstanceGroup instanceGroupByType = stack.getGatewayInstanceGroup();
                    gatewayHost = instanceMetadataRepository.findAliveInstancesHostNamesInInstanceGroup(instanceGroupByType.getId()).get(0);
                }
                blueprintText = ambariClient.extendBlueprintWithKerberos(blueprintText, gatewayHost, REALM, DOMAIN);
            }
            LOGGER.info("Adding generated blueprint to Ambari: {}", JsonUtil.readTree(blueprintText).toString());
            ambariClient.addBlueprint(blueprintText);
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

    private Map<String, List<Map<String, String>>> buildHostGroupAssociations(Set<HostGroup> hostGroups) throws InvalidHostGroupHostAssociation {
        Map<String, List<Map<String, String>>> hostGroupMappings = new HashMap<>();
        LOGGER.info("Computing host - hostGroup mappings based on hostGroup - instanceGroup associations");
        for (HostGroup hostGroup : hostGroups) {
            List<Map<String, String>> hostInfoForHostGroup = new ArrayList<>();
            if (hostGroup.getConstraint().getInstanceGroup() != null) {
                Map<String, String> topologyMapping = getTopologyMapping(hostGroup);
                Long instanceGroupId = hostGroup.getConstraint().getInstanceGroup().getId();
                List<InstanceMetaData> metas = instanceMetadataRepository.findAliveInstancesInInstanceGroup(instanceGroupId);
                for (InstanceMetaData meta : metas) {
                    Map<String, String> hostInfo = new HashMap<>();
                    hostInfo.put(FQDN, meta.getDiscoveryFQDN());
                    if (meta.getHypervisor() != null) {
                        hostInfo.put("hypervisor", meta.getHypervisor());
                        hostInfo.put("rack", topologyMapping.get(meta.getHypervisor()));
                    }
                    hostInfoForHostGroup.add(hostInfo);
                }
            } else {
                for (HostMetadata hostMetadata : hostGroup.getHostMetadata()) {
                    Map<String, String> hostInfo = new HashMap<>();
                    hostInfo.put(FQDN, hostMetadata.getHostName());
                    hostInfoForHostGroup.add(hostInfo);
                }
            }

            hostGroupMappings.put(hostGroup.getName(), hostInfoForHostGroup);
        }
        LOGGER.info("Computed host-hostGroup associations: {}", hostGroupMappings);
        return hostGroupMappings;
    }

    private Map<String, String> getTopologyMapping(HostGroup hg) {
        Map<String, String> result = new HashMap();
        LOGGER.info("Computing hypervisor - rack mapping based on topology");
        Topology topology = hg.getCluster().getStack().getCredential().getTopology();
        if (topology == null) {
            return result;
        }
        List<TopologyRecord> records = topology.getRecords();
        if (records != null) {
            for (TopologyRecord t : records) {
                result.put(t.getHypervisor(), t.getRack());
            }
        }
        return result;
    }

    private PollingResult waitForClusterInstall(Stack stack, AmbariClient ambariClient) {
        Map<String, Integer> clusterInstallRequest = new HashMap<>();
        clusterInstallRequest.put("CLUSTER_INSTALL", 1);
        return ambariOperationService.waitForOperations(stack, ambariClient, clusterInstallRequest, INSTALL_AMBARI_PROGRESS_STATE);
    }

    private Map<String, Integer> installServices(List<String> hosts, Stack stack, AmbariClient ambariClient, String hostGroup) {
        try {
            Cluster cluster = stack.getCluster();
            String blueprintName = cluster.getBlueprint().getBlueprintName();
            ambariClient.addHostsWithBlueprint(blueprintName, hostGroup, hosts);
            ambariOperationService.waitForOperations(stack, ambariClient, UPSCALE_REQUEST_CONTEXT, UPSCALE_REQUEST_STATUS, START_OPERATION_STATE);
            return singletonMap("UPSCALE_REQUEST", ambariClient.getRequestIdWithContext(UPSCALE_REQUEST_CONTEXT, UPSCALE_REQUEST_STATUS));
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Host already exists.", e);
            } else {
                String errorMessage = AmbariClientExceptionUtil.getErrorMessage(e);
                throw new CloudbreakServiceException("Ambari could not install services. " + errorMessage, e);
            }
        }
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
