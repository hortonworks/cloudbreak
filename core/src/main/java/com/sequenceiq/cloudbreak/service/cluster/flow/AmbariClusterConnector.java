package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isFailure;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.PollingResult.isTimeout;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.AMBARI_POLLING_INTERVAL;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_AMBARI_SERVER_STARTUP;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService.MAX_ATTEMPTS_FOR_HOSTS;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.INSTALL_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.SMOKE_TEST_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.START_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.START_OPERATION_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.STOP_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationType.UPSCALE_AMBARI_PROGRESS_STATE;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.cloudbreak.api.model.AdlsFileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.ClusterException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.TopologyRecord;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariAuthenticationProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfigurationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.AutoRecoveryConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.DruidSupersetConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.RDSConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.SmartSenseConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.ZeppelinConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.cluster.flow.kerberos.KerberosContainerDnResolver;
import com.sequenceiq.cloudbreak.service.cluster.flow.kerberos.KerberosDomainResolver;
import com.sequenceiq.cloudbreak.service.cluster.flow.kerberos.KerberosHostResolver;
import com.sequenceiq.cloudbreak.service.cluster.flow.kerberos.KerberosLdapResolver;
import com.sequenceiq.cloudbreak.service.cluster.flow.kerberos.KerberosPrincipalResolver;
import com.sequenceiq.cloudbreak.service.cluster.flow.kerberos.KerberosRealmResolver;
import com.sequenceiq.cloudbreak.service.cluster.flow.kerberos.KerberosTypeResolver;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupListenerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupPollerObject;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterConnector.class);

    private static final String REALM = "NODE.DC1.CONSUL";

    private static final String DOMAIN = "node.dc1.consul";

    private static final String KEY_TYPE = "PERSISTED";

    private static final String FQDN = "fqdn";

    private static final String ADMIN = "admin";

    private static final int KERBEROS_DB_PROPAGATION_PORT = 6318;

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
    private RdsConfigRepository rdsConfigRepository;

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
    private AmbariHostsStatusCheckerTask ambariHostsStatusCheckerTask;

    @Inject
    private PollingService<AmbariHostsCheckerContext> ambariHostJoin;

    @Inject
    private PollingService<AmbariClientPollerObject> ambariHealthChecker;

    @Inject
    private PollingService<AmbariStartupPollerObject> ambariStartupPollerObjectPollingService;

    @Inject
    private AmbariStartupListenerTask ambariStartupListenerTask;

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
    private TlsSecurityService tlsSecurityService;

    @Inject
    private SmartSenseConfigProvider smartSenseConfigProvider;

    @Inject
    private ZeppelinConfigProvider zeppelinConfigProvider;

    @Inject
    private DruidSupersetConfigProvider druidSupersetConfigProvider;

    @Inject
    private RDSConfigProvider rdsConfigProvider;

    @Inject
    private AutoRecoveryConfigProvider autoRecoveryConfigProvider;

    @Inject
    private ImageService imageService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private AmbariViewProvider ambariViewProvider;

    @Inject
    private KerberosHostResolver kerberosHostResolver;

    @Inject
    private KerberosPrincipalResolver kerberosPrincipalResolver;

    @Inject
    private KerberosLdapResolver kerberosLdapResolver;

    @Inject
    private KerberosContainerDnResolver kerberosContainerDnResolver;

    @Inject
    private KerberosTypeResolver kerberosTypeResolver;

    @Inject
    private KerberosDomainResolver kerberosDomainResolver;

    @Inject
    private KerberosRealmResolver kerberosRealmResolver;

    @Inject
    private AmbariAuthenticationProvider ambariAuthenticationProvider;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    public void waitForAmbariServer(Stack stack) throws CloudbreakException {
        AmbariClient defaultAmbariClient = getDefaultAmbariClient(stack);
        AmbariClient cloudbreakAmbariClient = getAmbariClient(stack);
        AmbariStartupPollerObject ambariStartupPollerObject = new AmbariStartupPollerObject(stack, stack.getAmbariIp(),
                Arrays.asList(defaultAmbariClient, cloudbreakAmbariClient));
        PollingResult pollingResult = ambariStartupPollerObjectPollingService.pollWithTimeoutSingleFailure(ambariStartupListenerTask, ambariStartupPollerObject,
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_AMBARI_SERVER_STARTUP);
        if (isSuccess(pollingResult)) {
            LOGGER.info("Ambari has successfully started! Polling result: {}", pollingResult);
        } else if (isExited(pollingResult)) {
            throw new CancellationException("Polling of Ambari server start has been cancelled.");
        } else {
            LOGGER.info("Could not start Ambari. polling result: {}", pollingResult);
            throw new CloudbreakException(String.format("Could not start Ambari. polling result: '%s'", pollingResult));
        }
    }

    public void buildAmbariCluster(Stack stack) {
        Cluster cluster = stack.getCluster();
        try {
            if (cluster.getCreationStarted() == null) {
                cluster.setCreationStarted(new Date().getTime());
                cluster = clusterRepository.save(cluster);
            }

            Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
            Map<String, List<Map<String, String>>> hostGroupMappings = buildHostGroupAssociations(hostGroups);

            recipeEngine.executePreInstall(stack, hostGroups);

            String blueprintText = updateBlueprintWithInputs(cluster, cluster.getBlueprint());

            Set<RDSConfig> rdsConfigs = rdsConfigRepository.findByClusterId(stack.getOwner(), stack.getAccount(), cluster.getId());
            FileSystem fs = cluster.getFileSystem();
            blueprintText = updateBlueprintConfiguration(stack, blueprintText, rdsConfigs, fs);

            AmbariClient ambariClient = getAmbariClient(stack);
            setBaseRepoURL(stack, ambariClient);
            addBlueprint(stack, ambariClient, blueprintText);

            Set<HostMetadata> hostsInCluster = hostMetadataRepository.findHostsInCluster(cluster.getId());
            PollingResult waitForHostsResult = waitForHosts(stack, ambariClient, hostsInCluster);
            checkPollingResult(waitForHostsResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_HOST_JOIN_FAILED.code()));

            String clusterName = cluster.getName();
            String blueprintName = cluster.getBlueprint().getBlueprintName();
            String configStrategy = cluster.getConfigStrategy().name();
            Boolean hideQuickLinks = cluster.getGateway().getEnableGateway();
            String clusterTemplate;
            if (ambariClient.getClusterName() == null) {
                if (cluster.isSecure()) {
                    KerberosConfig kerberosConfig = cluster.getKerberosConfig();
                    String principal = kerberosPrincipalResolver.resolvePrincipalForKerberos(kerberosConfig);
                    clusterTemplate = ambariClient.createSecureCluster(clusterName, blueprintName, hostGroupMappings, configStrategy,
                            cluster.getPassword(), principal, kerberosConfig.getKerberosPassword(), KEY_TYPE, hideQuickLinks);
                } else {
                    clusterTemplate = ambariClient.createCluster(clusterName, blueprintName, hostGroupMappings, configStrategy,
                            ambariAuthenticationProvider.getAmbariPassword(cluster), hideQuickLinks);
                }
                LOGGER.info("Submitted cluster creation template: {}", JsonUtil.minify(clusterTemplate));
            } else {
                LOGGER.info("Ambari cluster already exists: {}", clusterName);
            }
            PollingResult pollingResult = ambariOperationService.waitForOperationsToStart(stack, ambariClient, singletonMap("INSTALL_START", 1),
                    START_OPERATION_STATE);
            checkPollingResult(pollingResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_INSTALL_FAILED.code()));
            pollingResult = waitForClusterInstall(stack, ambariClient);
            checkPollingResult(pollingResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_INSTALL_FAILED.code()));

            recipeEngine.executePostInstall(stack);

            triggerSmartSenseCapture(ambariClient, blueprintText);
            cluster = ambariViewProvider.provideViewInformation(ambariClient, cluster);
            cluster = handleClusterCreationSuccess(stack, cluster);
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

    private String updateBlueprintConfiguration(Stack stack, String blueprintText, Set<RDSConfig> rdsConfigs, FileSystem fs)
            throws IOException, CloudbreakImageNotFoundException, CloudbreakException {
        if (fs != null) {
            blueprintText = extendBlueprintWithFsConfig(blueprintText, fs, stack);
        }
        blueprintText = smartSenseConfigProvider.addToBlueprint(stack, blueprintText);
        blueprintText = zeppelinConfigProvider.addToBlueprint(stack, blueprintText);
        blueprintText = druidSupersetConfigProvider.addToBlueprint(stack, blueprintText);
        if (!orchestratorTypeResolver.resolveType(stack.getOrchestrator()).containerOrchestrator()) {
            HDPRepo hdpRepo = clusterComponentConfigProvider.getHDPRepo(stack.getCluster().getId());
            if (hdpRepo != null && hdpRepo.getHdpVersion() != null) {
                blueprintText = blueprintProcessor.modifyHdpVersion(blueprintText, hdpRepo.getHdpVersion());
            }
        }
        if (rdsConfigs != null && !rdsConfigs.isEmpty()) {
            blueprintText = blueprintProcessor.addConfigEntries(blueprintText, rdsConfigProvider.getConfigs(rdsConfigs), true);
            blueprintText = blueprintProcessor.removeComponentFromBlueprint("MYSQL_SERVER", blueprintText);
        }
        blueprintText = autoRecoveryConfigProvider.addToBlueprint(blueprintText);
        return blueprintText;
    }

    public String updateBlueprintWithInputs(Cluster cluster, Blueprint blueprint) throws CloudbreakSecuritySetupException, IOException {
        String blueprintText = blueprint.getBlueprintText();

        Map<String, String> bpI = cluster.getBlueprintInputs().get(Map.class);
        if (bpI != null) {
            for (Map.Entry<String, String> stringStringEntry : bpI.entrySet()) {
                blueprintText = blueprintText.replaceAll(String.format("\\{\\{ %s \\}\\}",
                        stringStringEntry.getKey()), stringStringEntry.getValue());
            }
        }

        return blueprintText;
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

    public void waitForAmbariHosts(Stack stack) throws CloudbreakSecuritySetupException {
        AmbariClient ambariClient = getAmbariClient(stack);
        Set<HostMetadata> hostMetadata = hostMetadataRepository.findHostsInCluster(stack.getCluster().getId());
        waitForHosts(stack, ambariClient, hostMetadata);
    }

    public void installServices(Stack stack, HostGroup hostGroup, Set<HostMetadata> hostMetadata)
            throws CloudbreakException {
        AmbariClient ambariClient = getAmbariClient(stack);
        List<String> existingHosts = ambariClient.getClusterHosts();
        List<String> upscaleHostNames = getHostNames(hostMetadata).stream().filter(hostName -> !existingHosts.contains(hostName)).collect(Collectors.toList());
        if (!upscaleHostNames.isEmpty()) {
            PollingResult pollingResult = ambariOperationService.waitForOperations(stack, ambariClient,
                    installServices(upscaleHostNames, stack, ambariClient, hostGroup.getName()), UPSCALE_AMBARI_PROGRESS_STATE);
            checkPollingResult(pollingResult, cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_UPSCALE_FAILED.code()));
        }
    }

    private AmbariClient getDefaultAmbariClient(Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());
        return ambariClientProvider.getDefaultAmbariClient(clientConfig, stack.getGatewayPort());
    }

    private AmbariClient getAmbariClient(Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());
        return ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
    }

    private AmbariClient getAmbariClient(Stack stack, String user, String password) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());
        return ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), user, password);
    }

    public void credentialReplaceAmbariCluster(Long stackId, String newUserName, String newPassword) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        AmbariClient ambariClient = getAmbariClient(stack, cluster.getUserName(), cluster.getPassword());
        ambariClient = createAmbariUser(newUserName, newPassword, stack, ambariClient);
        ambariClient.deleteUser(cluster.getUserName());
    }

    private AmbariClient createAmbariUser(String newUserName, String newPassword, Stack stack, AmbariClient ambariClient) {
        try {
            ambariClient.createUser(newUserName, newPassword, true);
        } catch (Exception e) {
            try {
                ambariClient = getAmbariClient(stack, newUserName, newPassword);
                ambariClient.ambariServerVersion();
            } catch (Exception ie) {
                throw e;
            }
        }
        return ambariClient;
    }

    public void credentialUpdateAmbariCluster(Long stackId, String newPassword) throws CloudbreakSecuritySetupException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
        AmbariClient ambariClient = getAmbariClient(stack, cluster.getUserName(), cluster.getPassword());
        changeAmbariPassword(cluster.getUserName(), cluster.getPassword(), newPassword, stack, ambariClient);
    }

    private AmbariClient changeAmbariPassword(String userName, String oldPassword, String newPassword, Stack stack, AmbariClient ambariClient) {
        try {
            ambariClient.changePassword(userName, oldPassword, newPassword, true);
        } catch (Exception e) {
            try {
                ambariClient = getAmbariClient(stack, userName, newPassword);
                ambariClient.ambariServerVersion();
            } catch (Exception ie) {
                throw e;
            }
        }
        return ambariClient;
    }

    public void changeOriginalAmbariCredentialsAndCreateCloudbreakUser(Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        LOGGER.info("Changing ambari credentials for cluster: {}, ambari ip: {}", cluster.getName(), cluster.getAmbariIp());
        String userName = cluster.getUserName();
        String password = cluster.getPassword();
        AmbariClient ambariClient = getDefaultAmbariClient(stack);
        String cloudbreakUserName = ambariAuthenticationProvider.getAmbariUserName(cluster);
        String cloudbreakPassword = ambariAuthenticationProvider.getAmbariPassword(cluster);
        createAmbariUser(cloudbreakUserName, cloudbreakPassword, stack, ambariClient);
        if (ADMIN.equals(userName)) {
            if (!ADMIN.equals(password)) {
                changeAmbariPassword(ADMIN, ADMIN, password, stack, ambariClient);
            }
        } else {
            ambariClient = createAmbariUser(userName, password, stack, ambariClient);
            ambariClient.deleteUser(ADMIN);
        }
    }

    public void stopCluster(Stack stack) throws CloudbreakException {
        AmbariClient ambariClient = getAmbariClient(stack);
        try {
            if (!allServiceStopped(ambariClient.getHostComponentsStates())) {
                stopAllServices(stack, ambariClient);
            }
//            if (!"BYOS".equals(stack.cloudPlatform())) {
//                stopAmbariAgents(stack, null);
//            }
        } catch (AmbariConnectionException ex) {
            LOGGER.debug("Ambari not running on the gateway machine, no need to stop it.");
        }
    }

    public int startCluster(Stack stack) throws CloudbreakException {
        AmbariClient ambariClient = getAmbariClient(stack);
        waitForAmbariToStart(stack);
        if (!BYOS.equals(stack.cloudPlatform())) {
            startAmbariAgents(stack);
        }
        return startAllServices(stack, ambariClient);
    }

    public void waitForAllServices(Stack stack, int requestId) throws CloudbreakException {
        AmbariClient ambariClient = getAmbariClient(stack);
        waitForAllServices(stack, ambariClient, requestId);
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
        if (CloudConstants.AZURE.equals(stack.getPlatformVariant())) {
            String resourceGroupName = stack.getResourceByType(ResourceType.ARM_TEMPLATE).getResourceName();
            fsConfiguration.addProperty(FileSystemConfiguration.RESOURCE_GROUP_NAME, resourceGroupName);
        }
        // we have to lookup secret key from the credential because it is not stored in client side
        if (fsConfiguration instanceof AdlsFileSystemConfiguration) {
            String credential = String.valueOf(stack.getCredential().getAttributes().getMap().get(AdlsFileSystemConfiguration.CREDENTIAL_SECRET_KEY));
            ((AdlsFileSystemConfiguration) fsConfiguration).setCredential(credential);
        }
    }

    private void stopAllServices(Stack stack, AmbariClient ambariClient) throws CloudbreakException {
        LOGGER.info("Stop all Hadoop services");
        eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_SERVICES_STOPPING.code()));
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
            LOGGER.warn("Failed to stop Hadoop services.");
            throw new CloudbreakException("Failed to stop Hadoop services.");
        }
        eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_SERVICES_STOPPED.code()));
    }

    private void startAllServicesAndWait(Stack stack, AmbariClient ambariClient) throws CloudbreakException {
        int requestId = startAllServices(stack, ambariClient);
        if (requestId != -1) {
            waitForAllServices(stack, ambariClient, requestId);
        } else {
            LOGGER.error("Failed to start Hadoop services.");
            throw new CloudbreakException("Failed to start Hadoop services.");
        }
    }

    private int startAllServices(Stack stack, AmbariClient ambariClient) throws CloudbreakException {
        LOGGER.info("Start all Hadoop services");
        eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_SERVICES_STARTING.code()));
        int requestId = ambariClient.startAllServices();
        if (requestId == -1) {
            LOGGER.error("Failed to start Hadoop services.");
            throw new CloudbreakException("Failed to start Hadoop services.");
        }
        return requestId;
    }

    private void waitForAllServices(Stack stack, AmbariClient ambariClient, int requestId) throws CloudbreakException {
        LOGGER.info("Waiting for Hadoop services to start on stack");
        PollingResult servicesStartResult = ambariOperationService.waitForOperations(stack, ambariClient, singletonMap("start services", requestId),
                START_AMBARI_PROGRESS_STATE);
        if (isExited(servicesStartResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
        } else if (isTimeout(servicesStartResult)) {
            throw new CloudbreakException("Timeout while starting Ambari services.");
        }
        eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_SERVICES_STARTED.code()));
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

    private void triggerSmartSenseCapture(AmbariClient ambariClient, String blueprintText) {
        if (smartSenseConfigProvider.smartSenseIsConfigurable(blueprintText)) {
            try {
                LOGGER.info("Triggering SmartSense data capture.");
                ambariClient.smartSenseCapture(0);
            } catch (Exception e) {
                LOGGER.error("Triggering SmartSense capture is failed.", e);
            }
        }
    }

    private List<String> getHostNames(Set<HostMetadata> hostMetadata) {
        return hostMetadata.stream().map(HostMetadata::getHostName).collect(Collectors.toList());
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

    private void startAmbariAgents(Stack stack) throws CloudbreakException {
        LOGGER.info("Starting Ambari agents on the hosts.");
        PollingResult hostsJoinedResult = waitForHostsToJoin(stack);
        if (PollingResult.EXIT.equals(hostsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while starting Ambari agents.");
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

    private void setBaseRepoURL(Stack stack, AmbariClient ambariClient) throws IOException, CloudbreakImageNotFoundException, CloudbreakException {
        HDPRepo hdpRepo = null;
        if (!orchestratorTypeResolver.resolveType(stack.getOrchestrator()).containerOrchestrator()) {
            hdpRepo = clusterComponentConfigProvider.getHDPRepo(stack.getCluster().getId());
        }
        if (hdpRepo != null) {
            try {
                LOGGER.info("Use specific Ambari repository: {}", hdpRepo);
                Map<String, String> stackRepo = hdpRepo.getStack();
                Map<String, String> utilRepo = hdpRepo.getUtil();
                String stackRepoId = stackRepo.remove(HDPRepo.REPO_ID_TAG);
                String utilRepoId = utilRepo.remove(HDPRepo.REPO_ID_TAG);
                stackRepo.remove(HDPRepo.MPACK_TAG);
                String[] typeVersion = stackRepoId.split("-");
                String stackType = typeVersion[0];
                String version = "";
                if (typeVersion.length > 1) {
                    version = typeVersion[1];
                }
                for (String os : stackRepo.keySet()) {
                    addRepository(ambariClient, stackType, version, os, stackRepoId, stackRepo.get(os), hdpRepo.isVerify());
                }
                for (String os : utilRepo.keySet()) {
                    addRepository(ambariClient, stackType, version, os, utilRepoId, utilRepo.get(os), hdpRepo.isVerify());
                }
            } catch (HttpResponseException e) {
                String exceptionErrorMsg = AmbariClientExceptionUtil.getErrorMessage(e);
                String msg = String.format("Cannot use the specified Ambari stack: %s. Error: %s", hdpRepo.toString(), exceptionErrorMsg);
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
            HDPRepo hdpRepo = clusterComponentConfigProvider.getHDPRepo(cluster.getId());
            String repoId = hdpRepo.getStack().get("repoid");
            if (!repoId.startsWith("HDF") && !repoId.startsWith("hdf")) {
                Map<String, Map<String, Map<String, String>>> hostGroupConfig = hadoopConfigurationService.getHostGroupConfiguration(cluster);
                blueprintText = ambariClient.extendBlueprintHostGroupConfiguration(blueprintText, hostGroupConfig);
                Map<String, Map<String, String>> globalConfig = hadoopConfigurationService.getGlobalConfiguration(cluster);
                blueprintText = ambariClient.extendBlueprintGlobalConfiguration(blueprintText, globalConfig);
                blueprintText = addHBaseClient(blueprintText);
            } else {
                blueprintText = addHDFConfigToBlueprint(stack, ambariClient, blueprintText);
            }
            if (cluster.isSecure()) {
                String gatewayHost = cluster.getAmbariIp();
                if (stack.getInstanceGroups() != null && !stack.getInstanceGroups().isEmpty()) {
                    Integer propagationPort = stack.getGatewayInstanceMetadata().size() > 1 ? KERBEROS_DB_PROPAGATION_PORT : null;
                    gatewayHost = stack.getPrimaryGatewayInstance().getDiscoveryFQDN();
                    String domain = gatewayHost.substring(gatewayHost.indexOf(".") + 1);
                    blueprintText = ambariClient.extendBlueprintWithKerberos(blueprintText,
                            kerberosTypeResolver.resolveTypeForKerberos(cluster.getKerberosConfig()),
                            kerberosHostResolver.resolveHostForKerberos(cluster, gatewayHost),
                            kerberosRealmResolver.getRealm(domain, cluster.getKerberosConfig()),
                            kerberosDomainResolver.getDomains(domain),
                            kerberosLdapResolver.resolveLdapUrlForKerberos(cluster.getKerberosConfig()),
                            kerberosContainerDnResolver.resolveContainerDnForKerberos(cluster.getKerberosConfig()),
                            !cluster.getKerberosConfig().getKerberosTcpAllowed(), propagationPort);
                } else {
                    // TODO this won't work on mesos, but it doesn't work anyway
                    blueprintText = ambariClient.extendBlueprintWithKerberos(blueprintText,
                            kerberosTypeResolver.resolveTypeForKerberos(cluster.getKerberosConfig()),
                            gatewayHost,
                            REALM,
                            DOMAIN,
                            kerberosLdapResolver.resolveLdapUrlForKerberos(cluster.getKerberosConfig()),
                            kerberosContainerDnResolver.resolveContainerDnForKerberos(cluster.getKerberosConfig()),
                            !cluster.getKerberosConfig().getKerberosTcpAllowed(), null);
                }
            }
            LOGGER.info("Adding generated blueprint to Ambari: {}", JsonUtil.minify(blueprintText));
            ambariClient.addBlueprint(blueprintText, cluster.getTopologyValidation());
        } catch (IOException e) {
            if ("Conflict".equals(e.getMessage())) {
                LOGGER.info("Ambari blueprint already exists for stack: {}", stack.getId());
            } else if (e instanceof HttpResponseException) {
                String errorMessage = AmbariClientExceptionUtil.getErrorMessage((HttpResponseException) e);
                throw new CloudbreakServiceException("Ambari Blueprint could not be added: " + errorMessage, e);
            } else {
                throw new CloudbreakServiceException(e);
            }
        }
    }

    private String addHDFConfigToBlueprint(Stack stack, AmbariClient ambariClient, String blueprintText) {
        List<String> nifiFqdns = stack.getInstanceGroupByInstanceGroupName("NiFi").getAllInstanceMetaData().stream()
                .map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
        AtomicInteger index = new AtomicInteger(0);
        String nodeIdentities = nifiFqdns.stream()
                .map(fqdn -> String.format("<property name=\"Node Identity %s\">CN=%s, OU=NIFI</property>", index.addAndGet(1), fqdn))
                .collect(Collectors.joining());
        blueprintText = ambariClient.extendBlueprintGlobalConfiguration(blueprintText, ImmutableMap.of("nifi-ambari-ssl-config", ImmutableMap.of(
                "content", nodeIdentities)));
        blueprintText = ambariClient.extendBlueprintGlobalConfiguration(blueprintText, ImmutableMap.of("nifi-ambari-ssl-config", ImmutableMap.of(
                "nifi.initial.admin.identity", stack.getCluster().getUserName())));
        blueprintText = ambariClient.extendBlueprintGlobalConfiguration(blueprintText, ImmutableMap.of("ams-grafana-env", ImmutableMap.of(
                "metrics_grafana_username", stack.getCluster().getUserName(),
                "metrics_grafana_password", stack.getCluster().getPassword())));
        return blueprintText;
    }

    private String addHBaseClient(String blueprint) {
        String processingBlueprint = blueprint;
        try {
            JsonNode root = JsonUtil.readTree(processingBlueprint);
            ArrayNode hostGroupsNode = (ArrayNode) root.path("host_groups");
            Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
            while (hostGroups.hasNext()) {
                JsonNode hostGroupNode = hostGroups.next();
                ArrayNode componentsArray = (ArrayNode) hostGroupNode.path("components");
                Iterator<JsonNode> iterator = componentsArray.elements();
                boolean masterPresent = false;
                boolean clientPresent = false;
                while (iterator.hasNext()) {
                    String componentName = iterator.next().path("name").textValue();
                    if ("HBASE_MASTER".equals(componentName)) {
                        masterPresent = true;
                    } else if ("HBASE_CLIENT".equals(componentName)) {
                        clientPresent = true;
                    }
                }
                if (masterPresent && !clientPresent) {
                    ObjectNode arrayElementNode = componentsArray.addObject();
                    arrayElementNode.put("name", "HBASE_CLIENT");
                }
            }
            processingBlueprint = JsonUtil.writeValueAsString(root);
        } catch (Exception e) {
            LOGGER.warn("Cannot extend blueprint with HBASE_CLIENT", e);
        }
        return processingBlueprint;
    }

    private String extendHiveConfig(AmbariClient ambariClient, String processingBlueprint) {
        Map<String, Map<String, String>> config = new HashMap<>();
        Map<String, String> hiveSite = new HashMap<>();
        hiveSite.put("hive.server2.authentication.kerberos.keytab", "/etc/security/keytabs/hive2.service.keytab");
        config.put("hive-site", hiveSite);
        return ambariClient.extendBlueprintGlobalConfiguration(processingBlueprint, config);
    }

    private PollingResult waitForHosts(Stack stack, AmbariClient ambariClient, Set<HostMetadata> hostsInCluster) {
        LOGGER.info("Waiting for hosts to connect.[Ambari server address: {}]", stack.getAmbariIp());
        return hostsPollingService.pollWithTimeoutSingleFailure(
                ambariHostsStatusCheckerTask, new AmbariHostsCheckerContext(stack, ambariClient, hostsInCluster, hostsInCluster.size()),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS);
    }

    private Map<String, List<Map<String, String>>> buildHostGroupAssociations(Set<HostGroup> hostGroups) {
        Map<String, List<Map<String, String>>> hostGroupMappings = new HashMap<>();
        LOGGER.info("Computing host - hostGroup mappings based on hostGroup - instanceGroup associations");
        for (HostGroup hostGroup : hostGroups) {
            List<Map<String, String>> hostInfoForHostGroup = new ArrayList<>();
            if (hostGroup.getConstraint().getInstanceGroup() != null) {
                Map<String, String> topologyMapping = getTopologyMapping(hostGroup);
                Long instanceGroupId = hostGroup.getConstraint().getInstanceGroup().getId();
                List<InstanceMetaData> metas = instanceMetadataRepository.findAliveInstancesInInstanceGroup(instanceGroupId);
                if (metas.isEmpty()) {
                    for (HostMetadata hostMetadata : hostGroup.getHostMetadata()) {
                        Map<String, String> hostInfo = new HashMap<>();
                        hostInfo.put(FQDN, hostMetadata.getHostName());
                        hostInfoForHostGroup.add(hostInfo);
                    }
                } else {
                    for (InstanceMetaData meta : metas) {
                        Map<String, String> hostInfo = new HashMap<>();
                        hostInfo.put(FQDN, meta.getDiscoveryFQDN());
                        String localityIndicator = meta.getLocalityIndicator();
                        if (localityIndicator != null) {
                            if (topologyMapping.isEmpty()) {
                                // Azure
                                if (localityIndicator.startsWith("/")) {
                                    hostInfo.put("rack", meta.getLocalityIndicator());
                                    // Openstack
                                } else {
                                    hostInfo.put("rack", "/" + meta.getLocalityIndicator());
                                }
                                // With topology mapping
                            } else {
                                hostInfo.put("hypervisor", meta.getLocalityIndicator());
                                hostInfo.put("rack", topologyMapping.get(meta.getLocalityIndicator()));
                            }
                        }
                        hostInfoForHostGroup.add(hostInfo);
                    }
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
            return singletonMap("UPSCALE_REQUEST", ambariClient.addHostsWithBlueprint(blueprintName, hostGroup, hosts));
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Host already exists.", e);
            } else {
                String errorMessage = AmbariClientExceptionUtil.getErrorMessage(e);
                throw new CloudbreakServiceException("Ambari could not install services. " + errorMessage, e);
            }
        }
    }

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
        AMBARI_CLUSTER_UPSCALE_FAILED("ambari.cluster.upscale.failed"),
        AMBARI_CLUSTER_MR_SMOKE_FAILED("ambari.cluster.mr.smoke.failed"),
        AMBARI_CLUSTER_SERVICES_STARTING("ambari.cluster.services.starting"),
        AMBARI_CLUSTER_SERVICES_STARTED("ambari.cluster.services.started"),
        AMBARI_CLUSTER_SERVICES_STOPPING("ambari.cluster.services.stopping"),
        AMBARI_CLUSTER_SERVICES_STOPPED("ambari.cluster.services.stopped");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }
}
