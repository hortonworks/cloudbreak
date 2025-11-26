package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_6_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider.CipherSuitesLimitType.DEFAULT;
import static com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider.CipherSuitesLimitType.JAVA_INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider.CipherSuitesLimitType.MINIMAL;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.http.conn.util.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.CMLicenseParser;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnEncoder;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.cluster.model.ServiceLocationMap;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazDatahubConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazDatalakeConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.BackUpDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.CsdParcelDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.HostAttributeDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.JavaPillarDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.NameserverPillarDecorator;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.kerberos.KerberosPillarConfigGenerator;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.GrainOperation;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorGrainRunnerParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GrainProperties;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.san.LoadBalancerSANProvider;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.encryptionprofile.EncryptionProfileService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerFqdnUtil;
import com.sequenceiq.cloudbreak.service.paywall.PaywallConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;
import com.sequenceiq.cloudbreak.service.stack.flow.MountDisks;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltPillarDecorator;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.template.views.provider.RdsViewProvider;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider.CipherSuitesLimitType;
import com.sequenceiq.cloudbreak.util.EphemeralVolumeUtil;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class ClusterHostServiceRunner {

    public static final String CM_DATABASE_PILLAR_KEY = "cloudera-manager-database";

    public static final String IDBROKER_PILLAR_KEY = "idbroker";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterHostServiceRunner.class);

    private static final int CM_HTTP_PORT = 7180;

    private static final int CM_HTTPS_PORT = 7183;

    @Value("${cb.cm.heartbeat.interval}")
    private String cmHeartbeatInterval;

    @Value("${cb.cm.missed.heartbeat.interval}")
    private String cmMissedHeartbeatInterval;

    @Value("${cb.ccmRevertJob.activationInMinutes}")
    private Integer activationInMinutes;

    @Value("${cb.knox.gateway.security.dir}")
    private String knoxGatewaySecurityDir;

    @Value("${cb.knox.idbroker.security.dir}")
    private String knoxIdBrokerSecurityDir;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private LoadBalancerSANProvider loadBalancerSANProvider;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ComponentLocatorService componentLocator;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private ProxyConfigProvider proxyConfigProvider;

    @Inject
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private TelemetrySaltPillarDecorator telemetrySaltPillarDecorator;

    @Inject
    private MountDisks mountDisks;

    @Inject
    private VirtualGroupService virtualGroupService;

    @Inject
    private GrainPropertiesService grainPropertiesService;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Inject
    private EnvironmentConfigProvider environmentConfigProvider;

    @Inject
    private CMLicenseParser cmLicenseParser;

    @Inject
    private HostAttributeDecorator hostAttributeDecorator;

    @Inject
    private LoadBalancerFqdnUtil loadBalancerFqdnUtil;

    @Inject
    private IdBrokerService idBrokerService;

    @Inject
    private CsdParcelDecorator csdParcelDecorator;

    @Inject
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @Inject
    private GatewayService gatewayService;

    @Inject
    private SssdConfigProvider sssdConfigProvider;

    @Inject
    private NameserverPillarDecorator nameserverPillarDecorator;

    @Inject
    private RdsViewProvider rdsViewProvider;

    @Inject
    private JavaPillarDecorator javaPillarDecorator;

    @Inject
    private RangerRazDatahubConfigProvider rangerRazDatahubConfigProvider;

    @Inject
    private RangerRazDatalakeConfigProvider rangerRazDatalakeConfigProvider;

    @Inject
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @Inject
    private PaywallConfigService paywallConfigService;

    @Inject
    private ComponentLocatorService componentLocatorService;

    @Inject
    private BackUpDecorator backUpDecorator;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private BlueprintService blueprintService;

    @Autowired
    private EnvironmentService environmentService;

    @Inject
    private KerberosPillarConfigGenerator kerberosPillarConfigGenerator;

    @Inject
    private EncryptionProfileProvider encryptionProfileProvider;

    @Inject
    private EncryptionProfileService encryptionProfileService;

    public NodeReachabilityResult runClusterServices(@Nonnull StackDto stackDto,
            Map<String, String> candidateAddresses, boolean runPreServiceDeploymentRecipe) {
        LOGGER.debug("Run cluster services on candidates: {}", candidateAddresses);
        try {
            Set<Node> allNodes = stackUtil.collectNodes(stackDto, emptySet());
            Set<Node> reachableNodes = stackUtil.collectReachableAndCheckNecessaryNodes(stackDto, candidateAddresses.keySet());
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
            List<GrainProperties> grainsProperties = grainPropertiesService.createGrainProperties(gatewayConfigs, stackDto, reachableNodes);
            executeRunClusterServices(stackDto, candidateAddresses,
                    allNodes, reachableNodes, gatewayConfigs, grainsProperties, runPreServiceDeploymentRecipe);
            return new NodeReachabilityResult(reachableNodes, Set.of());
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException | IOException | CloudbreakException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        } catch (NodesUnreachableException e) {
            String errorMessage = "Can not run cluster services on new nodes because the configuration management service is not responding on these nodes: "
                    + e.getUnreachableNodes();
            LOGGER.error(errorMessage);
            throw new CloudbreakServiceException(errorMessage, e);
        }
    }

    public NodeReachabilityResult runTargetedClusterServices(StackDto stackDto, Map<String, String> candidateAddresses) {
        LOGGER.debug("Run targeted cluster services on candidates: {}", candidateAddresses);
        try {
            Set<String> notTerminatedAndNotZombieGateways = stackDto.getNotTerminatedAndNotZombieGatewayInstanceMetadata().stream()
                    .map(InstanceMetadataView::getDiscoveryFQDN).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<Node> reachableGatewayNodes = stackUtil.collectReachableAndCheckNecessaryNodes(stackDto, notTerminatedAndNotZombieGateways)
                    .stream()
                    .filter(node -> notTerminatedAndNotZombieGateways.contains(node.getHostname()))
                    .collect(Collectors.toSet());
            NodeReachabilityResult candidateNodesReachabilityResult =
                    stackUtil.collectReachableAndUnreachableCandidateNodes(stackDto, candidateAddresses.keySet());
            if (candidateNodesReachabilityResult.getReachableNodes().isEmpty()) {
                LOGGER.warn("No reachable candidates found, unreachable candidates: {}", candidateNodesReachabilityResult.getUnreachableHosts());
                throw new CloudbreakServiceException("No reachable candidates found.");
            }
            Set<Node> reachableCandidates = Sets.union(candidateNodesReachabilityResult.getReachableNodes(), reachableGatewayNodes);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
            List<GrainProperties> grainsProperties = grainPropertiesService
                    .createGrainPropertiesForTargetedUpscale(gatewayConfigs, stackDto, reachableCandidates);
            LOGGER.debug("We are about to execute cluster services (salt highstate, pre cluster manager recipe execution, mount disks, etc.) " +
                    "for reachable candidates (targeted operation): {}", reachableCandidates.stream().map(Node::getHostname).collect(Collectors.joining(",")));
            executeRunClusterServices(stackDto, candidateAddresses, reachableCandidates, reachableCandidates,
                    gatewayConfigs, grainsProperties, true);
            return new NodeReachabilityResult(reachableCandidates, candidateNodesReachabilityResult.getUnreachableNodes());
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException | IOException | CloudbreakException | NodesUnreachableException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void executeRunClusterServices(StackDto stackDto, Map<String, String> candidateAddresses,
            Set<Node> allNodes, Set<Node> reachableNodes, List<GatewayConfig> gatewayConfigs,
            List<GrainProperties> grainsProperties, boolean runPreServiceDeploymentRecipe)
            throws IOException, CloudbreakOrchestratorException, CloudbreakException {
        SaltConfig saltConfig = createSaltConfig(stackDto, grainsProperties);
        StackView stack = stackDto.getStack();
        ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), stack.getClusterId());
        modifyStartupMountRole(stackDto, reachableNodes, GrainOperation.ADD);
        hostOrchestrator.initServiceRun(stackDto, gatewayConfigs, allNodes, reachableNodes, saltConfig,
                exitCriteriaModel, stack.getCloudPlatform());
        if (StackService.REATTACH_COMPATIBLE_PLATFORMS.contains(stack.getPlatformVariant())) {
            LOGGER.debug("Platform is reattach compatible: {}", stack.getPlatformVariant());
            mountDisks(stackDto.getStack(), candidateAddresses, allNodes, reachableNodes);
        }
        if (runPreServiceDeploymentRecipe) {
            recipeEngine.executePreServiceDeploymentRecipes(stackDto, candidateAddresses, hostGroupService.getByClusterWithRecipes(stack.getClusterId()));
        }
        hostOrchestrator.runService(gatewayConfigs, reachableNodes, exitCriteriaModel);
        modifyStartupMountRole(stackDto, reachableNodes, GrainOperation.REMOVE);
    }

    private void mountDisks(StackView stack, Map<String, String> candidateAddresses, Set<Node> allNodes, Set<Node> reachableNodes) throws CloudbreakException {
        Set<String> reachableCandidateAddresses = reachableNodes.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(candidateAddresses)) {
            mountDisks.mountAllDisks(stack.getId());
        } else {
            mountDisks.mountDisksOnNewNodes(stack.getId(), reachableCandidateAddresses, allNodes);
        }
    }

    public void updateClusterConfigs(@Nonnull StackDto stackDto, boolean distributeSalt) {
        try {
            Set<Node> allNodes = stackUtil.collectNodes(stackDto, emptySet());
            Set<Node> reachableNodes = stackUtil.collectReachableNodesByInstanceStates(stackDto);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
            List<GrainProperties> grainsProperties = grainPropertiesService.createGrainProperties(gatewayConfigs, stackDto, reachableNodes);
            SaltConfig saltConfig = createSaltConfig(stackDto, grainsProperties);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stackDto.getStack().getId(), stackDto.getCluster().getId());
            if (distributeSalt) {
                LOGGER.debug("Adding GRAIN.ADD operation to update the salt properties on the mount-instance-storage - adding startup_mount role");
                modifyStartupMountRole(stackDto, reachableNodes, GrainOperation.ADD);
            }
            hostOrchestrator.initSaltConfig(stackDto, gatewayConfigs, allNodes, saltConfig, exitCriteriaModel);
            hostOrchestrator.runService(gatewayConfigs, reachableNodes, exitCriteriaModel);
            if (distributeSalt) {
                LOGGER.debug("Removing GRAIN.ADD operation after execution - removing startup_mount role");
                modifyStartupMountRole(stackDto, reachableNodes, GrainOperation.REMOVE);
            }
        } catch (CloudbreakOrchestratorException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public void updateClusterConfigs(@Nonnull StackDto stackDto) throws CloudbreakServiceException {
        updateClusterConfigs(stackDto, false);
    }

    public NodeReachabilityResult addClusterServices(StackDto stackDto, Map<String, Integer> hostGroupWithAdjustment, boolean repair) {
        Map<String, String> candidates = collectUpscaleCandidates(stackDto, hostGroupWithAdjustment);
        Set<String> gatewayHosts = stackDto.getNotTerminatedAndNotZombieGatewayInstanceMetadata().stream()
                .map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toSet());
        boolean candidatesContainGatewayNode = candidates.keySet().stream().anyMatch(gatewayHosts::contains);
        NodeReachabilityResult nodeReachabilityResult;
        if (!repair && !candidatesContainGatewayNode && targetedUpscaleSupportService.targetedUpscaleOperationSupported(stackDto.getStack())) {
            nodeReachabilityResult = runTargetedClusterServices(stackDto, candidates);
        } else {
            nodeReachabilityResult = runClusterServices(stackDto, candidates, true);
        }
        return nodeReachabilityResult;
    }

    public String changePrimaryGateway(StackDto stackDto) throws CloudbreakException {
        GatewayConfig formerPrimaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
        Optional<GatewayConfig> newPrimaryCandidate = gatewayConfigs.stream().filter(gc -> !gc.isPrimary()).findFirst();
        if (newPrimaryCandidate.isPresent()) {
            GatewayConfig newPrimary = newPrimaryCandidate.get();
            Set<Node> allNodes = stackUtil.collectNodes(stackDto);
            try {
                hostOrchestrator.changePrimaryGateway(formerPrimaryGatewayConfig, newPrimary, gatewayConfigs,
                        allNodes, clusterDeletionBasedModel(stackDto.getStack().getId(), stackDto.getCluster().getId()));
                return newPrimary.getHostname();
            } catch (CloudbreakOrchestratorException ex) {
                throw new CloudbreakException(ex);
            }
        } else {
            throw new CloudbreakException("Primary gateway change is not possible because there is no available node for the action");
        }
    }

    public void redeployGatewayCertificate(StackDto stackDto) {
        throwIfNull(stackDto, () -> new IllegalArgumentException("Stack should not be null"));
        throwIfNull(stackDto.getCluster(), () -> new IllegalArgumentException("Cluster should not be null"));
        try {
            Set<Node> allNodes = stackUtil.collectNodes(stackDto);
            Set<Node> reachableNodes = stackUtil.collectReachableNodes(stackDto);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
            List<GrainProperties> grainsProperties = grainPropertiesService.createGrainProperties(gatewayConfigs, stackDto, reachableNodes);
            SaltConfig saltConfig = createSaltConfig(stackDto, grainsProperties);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stackDto.getStack().getId(), stackDto.getCluster().getId());
            hostOrchestrator.initServiceRun(stackDto, gatewayConfigs, allNodes, reachableNodes, saltConfig, exitCriteriaModel, stackDto.getCloudPlatform());
            hostOrchestrator.runService(gatewayConfigs, reachableNodes, exitCriteriaModel);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public void redeployGatewayPillarOnly(StackDto stackDto, Set<String> removableHosts) {
        ClusterView cluster = stackDto.getCluster();
        throwIfNull(stackDto, () -> new IllegalArgumentException("Stack should not be null"));
        throwIfNull(cluster, () -> new IllegalArgumentException("Cluster should not be null"));
        try {
            Set<Node> allNodes = stackUtil.collectNodes(stackDto);
            Set<Node> reachableNodes = stackUtil.collectReachableNodes(stackDto);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
            List<GrainProperties> grainsProperties = grainPropertiesService.createGrainProperties(gatewayConfigs, stackDto, reachableNodes);
            SaltConfig saltConfig = createSaltConfigWithGatewayPillarOnly(stackDto, grainsProperties, removableHosts);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stackDto.getId(), stackDto.getId());
            LOGGER.debug("Calling orchestrator to upload gateway pillar");
            hostOrchestrator.uploadGatewayPillar(gatewayConfigs, allNodes, exitCriteriaModel, saltConfig);
            if (!CollectionUtils.isEmpty(removableHosts)) {
                Set<Node> knoxNodes = getHostsWithKnoxRole(grainsProperties, reachableNodes, removableHosts);
                hostOrchestrator.runService(gatewayConfigs, knoxNodes, exitCriteriaModel);
            }
        } catch (CloudbreakOrchestratorCancelledException e) {
            LOGGER.debug("Orchestration cancelled during redeploying gateway pillar", e);
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException | IOException e) {
            LOGGER.debug("Orchestration exception during redeploying gateway pillar", e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Set<Node> getHostsWithKnoxRole(List<GrainProperties> grainsProperties, Set<Node> nodes, Set<String> removableHosts) {
        Set<String> hostsWithKnoxRole = new HashSet<>();
        for (GrainProperties properties : grainsProperties) {
            properties.getProperties().forEach((fqdn, grains) -> {
                if (!removableHosts.contains(fqdn) && "knox".equals(grains.get("roles"))) {
                    hostsWithKnoxRole.add(fqdn);
                }
            });
        }
        return nodes.stream().filter(node -> hostsWithKnoxRole.contains(node.getHostname())).collect(Collectors.toSet());
    }

    public void redeployStates(StackDto stackDto) {
        ClusterView cluster = stackDto.getCluster();
        throwIfNull(stackDto, () -> new IllegalArgumentException("Stack should not be null"));
        throwIfNull(cluster, () -> new IllegalArgumentException("Cluster should not be null"));
        try {
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stackDto.getId(), cluster.getId());
            LOGGER.debug("Calling orchestrator to upload states");
            hostOrchestrator.uploadStates(gatewayConfigs, exitCriteriaModel);
        } catch (CloudbreakOrchestratorCancelledException e) {
            LOGGER.debug("Orchestration cancelled during redeploying states", e);
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.debug("Orchestration exception during redeploying states", e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private SaltConfig createSaltConfig(StackDto stackDto, List<GrainProperties> grainsProperties)
            throws IOException, CloudbreakOrchestratorException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
        StackView stack = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId());
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        DetailedEnvironmentResponse detailedEnvironmentResponse = environmentConfigProvider.getEnvironmentByCrn(stackDto.getEnvironmentCrn());
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        servicePillar.putAll(nameserverPillarDecorator.createPillarForNameservers(kerberosConfig, stack.getEnvironmentCrn(),
                detailedEnvironmentResponse.getEnvironmentType()));
        servicePillar.putAll(createUnboundEliminationPillar(stack.getDomainDnsResolver()));
        servicePillar.putAll(kerberosPillarConfigGenerator.createKerberosPillar(kerberosConfig, detailedEnvironmentResponse));
        servicePillar.putAll(hostAttributeDecorator.createHostAttributePillars(stackDto));
        servicePillar.put("discovery", new SaltPillarProperties("/discovery/init.sls", singletonMap("platform", stack.getCloudPlatform())));
        String virtualGroupsEnvironmentCrn = environmentConfigProvider.getParentEnvironmentCrn(stack.getEnvironmentCrn());
        servicePillar.putAll(createMetadataPillars(stackDto, stack, detailedEnvironmentResponse, virtualGroupsEnvironmentCrn,
                clouderaManagerRepo.getVersion()));
        ClusterPreCreationApi connector = clusterApiConnectors.getConnector(cluster);
        Map<String, List<String>> serviceLocations = getServiceLocations(stackDto);
        Optional<LdapView> ldapView = ldapConfigService.get(stack.getEnvironmentCrn(), stack.getName());
        VirtualGroupRequest virtualGroupRequest = getVirtualGroupRequest(virtualGroupsEnvironmentCrn, ldapView);
        servicePillar.putAll(createGatewayPillar(primaryGatewayConfig, stackDto, virtualGroupRequest, connector, kerberosConfig, serviceLocations,
                clouderaManagerRepo));
        saveIdBrokerPillar(cluster, servicePillar);
        postgresConfigService.decorateServicePillarWithPostgresIfNeeded(servicePillar, stackDto);
        servicePillar.putAll(javaPillarDecorator.createJavaPillars(stackDto, detailedEnvironmentResponse));

        addClouderaManagerConfig(stackDto, servicePillar, clouderaManagerRepo, primaryGatewayConfig);
        ldapView.ifPresent(ldap -> saveLdapPillar(ldap, servicePillar));

        servicePillar.putAll(sssdConfigProvider.createSssdAdPillar(kerberosConfig));
        servicePillar.putAll(sssdConfigProvider.createSssdIpaPillar(kerberosConfig, serviceLocations,
                stack.getEnvironmentCrn(), stack.getStackVersion(), cluster.getExtendedBlueprintText()));

        Map<String, Map<String, String>> mountPathMap = new HashMap<>();
        stackDto.getInstanceGroupDtos().forEach(group -> {
            mountPathMap.putAll(group.getInstanceMetadataViews().stream()
                    .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                    .collect(Collectors.toMap(
                            InstanceMetadataView::getDiscoveryFQDN,
                            node -> getMountPath(stackDto, group.getInstanceGroup()),
                            (l, r) -> getMountPath(stackDto, group.getInstanceGroup()))));
        });
        servicePillar.put("startup", new SaltPillarProperties("/mount/startup.sls", singletonMap("mount", mountPathMap)));

        proxyConfigProvider.decoratePillarWithProxyDataIfNeeded(servicePillar, stackDto);

        decoratePillarWithJdbcConnectors(cluster, servicePillar);
        servicePillar.putAll(paywallConfigService.createPaywallPillarConfig(stackDto));

        if (detailedEnvironmentResponse.isEnableSecretEncryption()) {
            backUpDecorator.decoratePillarWithBackup(stackDto, detailedEnvironmentResponse, servicePillar);
        }

        return new SaltConfig(servicePillar, grainsProperties);
    }

    private Map<String, SaltPillarProperties> createMetadataPillars(StackDto stackDto, StackView stack, DetailedEnvironmentResponse detailedEnvironmentResponse,
            String virtualGroupsEnvironmentCrn, String cmVersion) {
        boolean deployedInChildEnvironment = !virtualGroupsEnvironmentCrn.equals(stack.getEnvironmentCrn());
        String seLinux = stackDto.getSecurityConfig() != null && stackDto.getSecurityConfig().getSeLinux() != null ?
                stackDto.getSecurityConfig().getSeLinux().toString().toLowerCase(Locale.ROOT) : SeLinux.PERMISSIVE.toString().toLowerCase(Locale.ROOT);
        boolean hybridEnabled = stack.isDatalake() && EnvironmentType.HYBRID_BASE.toString().equals(detailedEnvironmentResponse.getEnvironmentType());
        // Hive with remote HMS workaround - TODO remove after OPSAPS-73356
        String blueprintJsonText = stackDto.getBlueprintJsonText();
        boolean hiveWithRemoteHiveMetastore = blueprintJsonText != null
                && blueprintJsonText.contains(HiveRoles.HIVESERVER2)
                && !blueprintJsonText.contains(HiveRoles.HIVEMETASTORE);

        EncryptionProfileResponse encryptionProfileResponse =  encryptionProfileService.getEncryptionProfileByCrnOrDefault(
                detailedEnvironmentResponse, stackDto);
        Set<String> useTlsVersions =
                Optional.ofNullable(encryptionProfileResponse)
                        .map(EncryptionProfileResponse::getTlsVersions)
                        .orElse(null);
        Map<String, List<String>> userCipherSuits =
                Optional.ofNullable(encryptionProfileResponse)
                        .map(EncryptionProfileResponse::getCipherSuites)
                        .orElse(null);
        boolean encryptionProfileEnabled = entitlementService.isConfigureEncryptionProfileEnabled(detailedEnvironmentResponse.getAccountId());

        Map<String, ? extends Serializable> clusterProperties = Map.ofEntries(
                Map.entry("name", stackDto.getCluster().getName()),
                Map.entry("deployedInChildEnvironment", deployedInChildEnvironment),
                Map.entry("gov_cloud", stackDto.isOnGovPlatformVariant()),
                Map.entry("secretEncryptionEnabled", detailedEnvironmentResponse.isEnableSecretEncryption()),
                Map.entry("selinux_mode", seLinux),
                Map.entry("hybridEnabled", hybridEnabled),
                Map.entry("hiveWithRemoteHiveMetastore", hiveWithRemoteHiveMetastore),
                Map.entry("tlsv13Enabled", Boolean.FALSE),
                Map.entry("tlsAdvancedControl", encryptionProfileEnabled),
                Map.entry("tlsVersionsSpaceSeparated", encryptionProfileProvider.getTlsVersions(useTlsVersions, " ")),
                Map.entry("tlsVersionsCommaSeparated", encryptionProfileProvider.getTlsVersions(useTlsVersions, ",")),
                Map.entry("cmVersionSupportsTlsSetup", isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_13_2_0)),
                Map.entry("tlsCipherSuites", encryptionProfileProvider.getTlsCipherSuites(
                        userCipherSuits,
                        DEFAULT,
                        ":",
                        false
                )),
                Map.entry("tlsCipherSuitesMinimal", encryptionProfileProvider.getTlsCipherSuites(
                        userCipherSuits,
                        MINIMAL,
                        ":",
                        false
                )),
                Map.entry("tlsCipherSuitesJavaIntermediate", encryptionProfileProvider.getTlsCipherSuites(
                        userCipherSuits,
                        JAVA_INTERMEDIATE2018,
                        ":",
                        true
                ))
        );

        return Map.of("metadata", new SaltPillarProperties("/metadata/init.sls", singletonMap("cluster", clusterProperties)));
    }

    public void removeSecurityConfigFromCMAgentsConfig(StackDto stackDto, Set<Node> reachableNodes) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
        try {
            ClusterDeletionBasedExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stackDto.getId(), stackDto.getCluster().getId());
            Set<String> targets = reachableNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            hostOrchestrator.removeSecurityConfigFromCMAgentsConfig(primaryGatewayConfig, targets);
            hostOrchestrator.restartClusterManagerAgents(primaryGatewayConfig, targets, exitCriteriaModel);
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private SaltConfig createSaltConfigWithGatewayPillarOnly(StackDto stackDto, List<GrainProperties> grainsProperties, Set<String> removableHosts)
            throws IOException, CloudbreakOrchestratorException {
        StackView stack = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
        String virtualGroupsEnvironmentCrn = environmentConfigProvider.getParentEnvironmentCrn(stack.getEnvironmentCrn());
        ClusterPreCreationApi connector = clusterApiConnectors.getConnector(cluster);
        Map<String, List<String>> serviceLocations = filterRemovableHostFromServiceLocations(getServiceLocations(stackDto), removableHosts);
        LOGGER.debug("Getting LDAP config for Gateway pillar");
        Optional<LdapView> ldapView = ldapConfigService.get(stack.getEnvironmentCrn(), stack.getName());
        VirtualGroupRequest virtualGroupRequest = getVirtualGroupRequest(virtualGroupsEnvironmentCrn, ldapView);
        LOGGER.debug("Getting kerberos config for Gateway pillar");
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId());

        LOGGER.debug("Creating gateway pillar");
        Map<String, SaltPillarProperties> servicePillar =
                new HashMap<>(createGatewayPillar(primaryGatewayConfig, stackDto, virtualGroupRequest, connector, kerberosConfig, serviceLocations,
                        clouderaManagerRepo));

        return new SaltConfig(servicePillar, grainsProperties);
    }

    private Map<String, List<String>> filterRemovableHostFromServiceLocations(Map<String, List<String>> serviceLocations, Set<String> removableHosts) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        serviceLocations.forEach((service, locations) -> {
            result.put(service, locations.stream()
                    .filter(location -> removableHosts.stream().noneMatch(location::contains))
                    .toList());
        });
        return result;
    }

    private Map<String, String> getMountPath(StackDto stack, InstanceGroupView group) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        Optional<String> stackVersion = Optional.ofNullable(stack.getStackVersion()).or(() -> {
            try {
                return Optional.ofNullable(blueprintService.getCdhVersion(NameOrCrn.ofName(stack.getBlueprint().getName()), stack.getWorkspaceId()));
            } catch (Exception e) {
                LOGGER.error("Failed to get version to check if xfs is supported for the cluster, ", e);
                return Optional.empty();
            }
        });
        boolean xfsSupportForEphemeralDisksEntitled = entitlementService.isXfsForEphemeralDisksSupported(accountId);
        boolean xfsForEphemeralDisksSupported =
                EphemeralVolumeUtil.xfsForEphemeralSupported(xfsSupportForEphemeralDisksEntitled, stack.getStack(), stackVersion);
        return Map.of("mount_path", getMountPath(group),
                "cloud_platform", stack.getCloudPlatform(),
                "temporary_storage", group.getTemplate().getTemporaryStorage().name(),
                "xfs_for_ephemeral_supported", String.valueOf(xfsForEphemeralDisksSupported));
    }

    private String getMountPath(InstanceGroupView group) {
        if (TemporaryStorage.EPHEMERAL_VOLUMES.equals(group.getTemplate().getTemporaryStorage())) {
            return "ephfs";
        }
        return "fs";
    }

    private void addClouderaManagerConfig(StackDto stackDto, Map<String, SaltPillarProperties> servicePillar,
            ClouderaManagerRepo clouderaManagerRepo, GatewayConfig primaryGatewayConfig) throws CloudbreakOrchestratorFailedException {
        StackView stack = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stack.getId());
        servicePillar.putAll(telemetrySaltPillarDecorator.generatePillarConfigMap(stackDto));
        decoratePillarWithTags(stack, servicePillar);
        decorateWithClouderaManagerEnterpriseDetails(telemetry, servicePillar);
        Optional<String> licenseOpt = decoratePillarWithClouderaManagerLicense(stack, servicePillar);
        decoratePillarWithClouderaManagerRepo(clouderaManagerRepo, servicePillar, licenseOpt);
        decoratePillarWithClouderaManagerDatabase(cluster, servicePillar, stack.getCloudPlatform());
        decoratePillarWithClouderaManagerCommunicationSettings(stackDto, servicePillar);
        decoratePillarWithClouderaManagerAutoTls(cluster, servicePillar);
        csdParcelDecorator.decoratePillarWithCsdParcels(stackDto, servicePillar);
        servicePillar.putAll(createPillarWithClouderaManagerSettings(clouderaManagerRepo, stackDto, primaryGatewayConfig));
    }

    private VirtualGroupRequest getVirtualGroupRequest(String virtualGroupsEnvironmentCrn, Optional<LdapView> ldapView) {
        String adminGroup = ldapView.isPresent() ? ldapView.get().getAdminGroup() : "";
        return new VirtualGroupRequest(virtualGroupsEnvironmentCrn, adminGroup);
    }

    // Right now we are assuming that CM enterprise is enabled if workload analytics is used
    // In the future that should be enabled based on the license
    private void decorateWithClouderaManagerEnterpriseDetails(Telemetry telemetry, Map<String, SaltPillarProperties> servicePillar) {
        if (telemetry != null && telemetry.getWorkloadAnalytics() != null) {
            servicePillar.put("cloudera-manager-cme",
                    new SaltPillarProperties("/cloudera-manager/cme.sls", singletonMap("cloudera-manager", singletonMap("cme_enabled", true))));
        }
    }

    public void decoratePillarWithClouderaManagerDatabase(ClusterView cluster, Map<String, SaltPillarProperties> servicePillar, String cloudPlatform)
            throws CloudbreakOrchestratorFailedException {
        SaltPillarProperties saltPillarProperties = getClouderaManagerDatabasePillarProperties(cluster.getId(), cloudPlatform, cluster.getDatabaseServerCrn());
        servicePillar.put(CM_DATABASE_PILLAR_KEY, saltPillarProperties);
    }

    public SaltPillarProperties getClouderaManagerDatabasePillarProperties(Long clusterId, String cloudPlatform, String databaseServerCrn)
            throws CloudbreakOrchestratorFailedException {
        RdsConfigWithoutCluster clouderaManagerRdsConfig =
                rdsConfigWithoutClusterService.findByClusterIdAndType(clusterId, DatabaseType.CLOUDERA_MANAGER);
        if (clouderaManagerRdsConfig == null) {
            throw new CloudbreakOrchestratorFailedException("Cloudera Manager RDSConfig is missing for stackDto");
        }
        RdsView rdsView = rdsViewProvider.getRdsView(clouderaManagerRdsConfig, cloudPlatform,
                RedbeamsDbServerConfigurer.isRemoteDatabaseRequested(databaseServerCrn));
        SaltPillarProperties saltPillarProperties = new SaltPillarProperties("/cloudera-manager/database.sls",
                singletonMap("cloudera-manager", singletonMap("database", rdsView)));
        return saltPillarProperties;
    }

    private void decoratePillarWithClouderaManagerCommunicationSettings(StackDto stackDto, Map<String, SaltPillarProperties> servicePillar) {
        Boolean autoTls = stackDto.getCluster().getAutoTlsEnabled();
        Map<String, Object> communication = new HashMap<>();
        Optional<String> san = loadBalancerSANProvider.getLoadBalancerSAN(stackDto.getStack().getId(), stackDto.getBlueprint());
        if (san.isPresent()) {
            communication.put("internal_loadbalancer_san", san.get());
        }
        communication.put("port", autoTls ? CM_HTTPS_PORT : CM_HTTP_PORT);
        communication.put("protocol", autoTls ? "https" : "http");
        communication.put("autotls_enabled", autoTls);
        servicePillar.put("cloudera-manager-communication",
                new SaltPillarProperties("/cloudera-manager/communication.sls",
                        singletonMap("cloudera-manager", singletonMap("communication", communication))));
    }

    private void decoratePillarWithClouderaManagerAutoTls(ClusterView cluster, Map<String, SaltPillarProperties> servicePillar) {
        if (cluster.getAutoTlsEnabled()) {
            servicePillar.put("cloudera-manager-autotls", getClouderaManagerAutoTlsPillarProperties(cluster));
        }
    }

    public SaltPillarProperties getClouderaManagerAutoTlsPillarProperties(ClusterView cluster) {
        Map<String, Object> autoTls = new HashMap<>();
        autoTls.put("keystore_password", cluster.getKeyStorePwd());
        autoTls.put("truststore_password", cluster.getTrustStorePwd());
        return new SaltPillarProperties("/cloudera-manager/autotls.sls", singletonMap("cloudera-manager", singletonMap("autotls", autoTls)));
    }

    public Optional<String> decoratePillarWithClouderaManagerLicense(StackView stack, Map<String, SaltPillarProperties> servicePillar) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        Account account = umsClient.getAccountDetails(accountId);
        Optional<String> licenseOpt = Optional.ofNullable(account.getClouderaManagerLicenseKey());
        if (licenseOpt.isPresent() && isNotEmpty(licenseOpt.get())) {
            String license = licenseOpt.get();
            servicePillar.put("cloudera-manager-license",
                    new SaltPillarProperties("/cloudera-manager/license.sls",
                            singletonMap("cloudera-manager",
                                    singletonMap("license", license))));
        }
        return licenseOpt;
    }

    public void decoratePillarWithClouderaManagerRepo(ClouderaManagerRepo repo, Map<String, SaltPillarProperties> servicePillar, Optional<String> license) {
        servicePillar.put("cloudera-manager-repo", new SaltPillarProperties("/cloudera-manager/repo.sls",
                singletonMap("cloudera-manager", createCMRepoPillar(repo, license))));
    }

    private Map<String, Object> createCMRepoPillar(ClouderaManagerRepo clouderaManagerRepo, Optional<String> licenseOpt) {
        Map<String, Object> pillarValues = new HashMap<>();
        pillarValues.put("repo", clouderaManagerRepo);
        licenseOpt.flatMap(cmLicenseParser::parseLicense).ifPresent(jsonLicense -> {
            String username = jsonLicense.getPaywallUsername();
            String password = jsonLicense.getPaywallPassword();
            if (isNotEmpty(username) && isNotEmpty(password)) {
                pillarValues.put("paywall_username", username);
                pillarValues.put("paywall_password", password);
            }
        });
        return pillarValues;
    }

    public Map<String, SaltPillarProperties> createPillarWithClouderaManagerSettings(ClouderaManagerRepo clouderaManagerRepo, StackDto stackDto,
            GatewayConfig primaryGatewayConfig) {
        ServiceLocationMap serviceLocations = clusterApiConnectors.getConnector(stackDto.getCluster()).getServiceLocations();
        String cmVersion = clouderaManagerRepo.getVersion();
        String resourceCrn = stackDto.getStack().getResourceCrn();
        return Map.of("cloudera-manager-settings", new SaltPillarProperties("/cloudera-manager/settings.sls",
                singletonMap("cloudera-manager", Map.of(
                        "settings", Map.of(
                                "heartbeat_interval", cmHeartbeatInterval,
                                "missed_heartbeat_interval", cmMissedHeartbeatInterval,
                                "disable_auto_bundle_collection", true,
                                "set_cdp_env", isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_0_2),
                                "cloud_provider_setup_supported", isCloudProviderSetupSupported(stackDto, cmVersion),
                                "deterministic_uid_gid", isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_1),
                                "cloudera_scm_sudo_access", CMRepositoryVersionUtil.isSudoAccessNeededForHostCertRotation(clouderaManagerRepo)),
                        "mgmt_service_directories", serviceLocations.getAllVolumePath(),
                        "address", primaryGatewayConfig.getPrivateAddress()))));
    }

    private boolean isCloudProviderSetupSupported(StackDto stackDto, String cmVersion) {
        List<String> supportedCloudProviders = List.of(CloudPlatform.AWS.name(), CloudPlatform.GCP.name(), CloudPlatform.AZURE.name());
        return supportedCloudProviders.contains(stackDto.getCloudPlatform()) && isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_6_2);
    }

    private void decoratePillarWithTags(StackView stack, Map<String, SaltPillarProperties> servicePillarConfig) {
        if (stack.getTags() != null && isNotBlank(stack.getTags().getValue())) {
            try {
                StackTags stackTags = stack.getTags().get(StackTags.class);
                Map<String, Object> tags = new HashMap<>(stackTags.getDefaultTags());
                Map<String, Object> applicationTags = new HashMap<>(stackTags.getApplicationTags());
                tags.putAll(applicationTags);
                servicePillarConfig.put("tags", new SaltPillarProperties("/tags/init.sls",
                        Collections.singletonMap("tags", tags)));
            } catch (Exception e) {
                LOGGER.debug("Exception during reading default tags.", e);
            }
        }
    }

    private Map<String, SaltPillarProperties> createUnboundEliminationPillar(DnsResolverType dnsResolverType) {
        return Map.of("unbound-elimination", new SaltPillarProperties("/unbound/elimination.sls", singletonMap("unbound_elimination_supported",
                DnsResolverType.FREEIPA_FOR_ENV.equals(dnsResolverType))));
    }

    @SuppressWarnings("ParameterNumber")
    private Map<String, SaltPillarProperties> createGatewayPillar(GatewayConfig gatewayConfig, StackDto stackDto,
            VirtualGroupRequest virtualGroupRequest, ClusterPreCreationApi connector, KerberosConfig kerberosConfig, Map<String, List<String>> serviceLocations,
            ClouderaManagerRepo clouderaManagerRepo) throws IOException {
        Map<String, Object> gateway = new HashMap<>();
        gateway.put("address", gatewayConfig.getPublicAddress());
        if (gatewayConfig.getPublicAddress() != null) {
            boolean addressIsIp = InetAddresses.isInetAddress(gatewayConfig.getPublicAddress());
            LOGGER.debug("Checking if {} is an ip address. Result: {}", gatewayConfig.getPublicAddress(), addressIsIp);
            gateway.put("address_is_ip", addressIsIp);
        }
        DetailedEnvironmentResponse detailedEnvironmentResponse = environmentService.getByCrn(stackDto.getEnvironmentCrn());
        EncryptionProfileResponse encryptionProfileResponse = encryptionProfileService.getEncryptionProfileByCrnOrDefault(
                detailedEnvironmentResponse, stackDto);
        Set<String> useTlsVersions =
                Optional.ofNullable(encryptionProfileResponse)
                        .map(EncryptionProfileResponse::getTlsVersions)
                        .orElse(null);
        Map<String, List<String>> userCipherSuits =
                Optional.ofNullable(encryptionProfileResponse)
                        .map(EncryptionProfileResponse::getCipherSuites)
                        .orElse(null);
        ClusterView cluster = stackDto.getCluster();
        gateway.put("username", cluster.getUserName());
        gateway.put("password", cluster.getPassword());
        gateway.put("enable_knox_ranger_authorizer", isRangerAuthorizerEnabled(clouderaManagerRepo));
        gateway.put("enable_impala_sticky_session", isImpalaStickySessionEnabled(stackDto));
        gateway.put("enable_ccmv2", stackDto.getTunnel().useCcmV2OrJumpgate());
        gateway.put("enable_ccmv2_jumpgate", stackDto.getTunnel().useCcmV2Jumpgate());
        gateway.put("activation_in_minutes", activationInMinutes);
        gateway.put("tlsv13Enabled", Boolean.FALSE);
        gateway.put("tlsVersionsSpaceSeparated", encryptionProfileProvider.getTlsVersions(useTlsVersions, " "));
        gateway.put("tlsCipherSuites", encryptionProfileProvider.getTlsCipherSuites(userCipherSuits,
                DEFAULT, ":", false));
        gateway.put("tlsCipherSuitesRedHat8", encryptionProfileProvider.getTlsCipherSuites(userCipherSuits,
                CipherSuitesLimitType.REDHAT_VERSION8, ":", false));
        gateway.put("tlsCipherSuitesMinimal", encryptionProfileProvider.getTlsCipherSuites(userCipherSuits,
                MINIMAL, ":", false));
        gateway.putAll(createKnoxRelatedGatewayConfiguration(stackDto, virtualGroupRequest, connector));
        gateway.putAll(createGatewayUserFacingCertAndFqdn(gatewayConfig, stackDto));
        gateway.putAll(createGatewayAlternativeUserFacingCert(gatewayConfig));

        gateway.put("kerberos", kerberosConfig != null);

        addRangerServiceIfAvailable(gatewayConfig, stackDto, serviceLocations);

        addRangerRazServiceIfAvailable(stackDto, serviceLocations, gatewayConfig);

        serviceLocations.put(exposedServiceCollector.getClouderaManagerService().getServiceName(), asList(gatewayConfig.getHostname()));

        gateway.put("location", serviceLocations);
        if (stackDto.getNetwork() != null) {
            gateway.put("cidrBlocks", stackDto.getNetwork().getNetworkCidrs());
        }
        Map<String, Object> loadBalancerProperties = createLoadBalancerProperties(stackDto);
        if (MapUtils.isNotEmpty(loadBalancerProperties)) {
            gateway.putAll(loadBalancerProperties);
        }
        return Map.of("gateway", new SaltPillarProperties("/gateway/init.sls", singletonMap("gateway", gateway)));
    }

    private void addRangerServiceIfAvailable(GatewayConfig gatewayConfig, StackDto stackDto, Map<String, List<String>> serviceLocations) {
        ExposedService rangerService = exposedServiceCollector.getRangerService();
        List<String> rangerLocations = serviceLocations.get(rangerService.getServiceName());
        if (!CollectionUtils.isEmpty(rangerLocations)) {
            List<String> rangerGatewayHosts = getRangerFqdn(stackDto, gatewayConfig.getHostname(), rangerLocations);
            if (CollectionUtils.isEmpty(rangerGatewayHosts)) {
                serviceLocations.put(rangerService.getServiceName(), asList(rangerLocations.iterator().next()));
            } else {
                serviceLocations.put(rangerService.getServiceName(), rangerGatewayHosts);
            }
        }
    }

    private Map<String, Object> createKnoxRelatedGatewayConfiguration(StackDto stackDto, VirtualGroupRequest virtualGroupRequest,
            ClusterPreCreationApi connector) throws IOException {
        GatewayView clusterGateway = gatewayService.getByClusterId(stackDto.getCluster().getId()).orElse(null);
        Map<String, Object> gateway = new HashMap<>();
        if (clusterGateway != null) {
            gateway.put("path", clusterGateway.getPath());
            gateway.put("ssotype", clusterGateway.getSsoType());
            gateway.put("ssoprovider", clusterGateway.getSsoProvider());
            gateway.put("signpub", clusterGateway.getSignPub());
            gateway.put("signcert", clusterGateway.getSignCert());
            gateway.put("signkey", clusterGateway.getSignKey());
            gateway.put("tokencert", clusterGateway.getTokenCert());
            gateway.put("mastersecret", clusterGateway.getKnoxMaster());
            gateway.put("envAccessGroup", virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.ENVIRONMENT_ACCESS));
            gateway.put("knoxAdminGroup", virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.KNOX_ADMIN));
            gateway.put("autoscaleMachineUser", CrnEncoder.generateMd5EncodedAutoscaleMachineUser(stackDto.getEnvironmentCrn(), true));
            List<Map<String, Object>> topologies = getTopologies(clusterGateway, stackDto.getBlueprint().getStackVersion());
            gateway.put("topologies", topologies);
            if (stackDto.getBlueprint() != null) {
                ClusterView cluster = stackDto.getCluster();
                Boolean autoTlsEnabled = cluster.getAutoTlsEnabled();
                Map<String, Integer> servicePorts = connector.getServicePorts(stackDto.getBlueprint(), autoTlsEnabled);
                gateway.put("ports", servicePorts);
                gateway.put("protocol", autoTlsEnabled ? "https" : "http");
            }
            if (SSOType.SSO_PROVIDER_FROM_UMS.equals(clusterGateway.getSsoType())) {
                String accountId = stackDto.getAccountId();
                try {
                    String metadataXml = umsClient.getIdentityProviderMetadataXml(accountId);
                    gateway.put("saml", metadataXml);
                } catch (Exception e) {
                    LOGGER.debug("Could not get SAML metadata file to set up IdP in KNOXSSO.", e);
                    throw new NotFoundException("Could not get SAML metadata file to set up IdP in KNOXSSO: " + e.getMessage());
                }
            }
            gateway.put("knoxGatewaySecurityDir", knoxGatewaySecurityDir);
        } else {
            gateway.put("ssotype", SSOType.NONE);
            LOGGER.debug("Cluster gateway (Knox) is not set. Configure ssotype to 'NONE' for backward compatibility.");
        }
        return gateway;
    }

    private Map<String, Object> createLoadBalancerProperties(StackDto stackDto) {
        Map<String, Object> properties = new HashMap<>();
        Set<LoadBalancer> loadBalancers = loadBalancerFqdnUtil.getLoadBalancersForStack(stackDto.getId());
        List<Map<String, String>> lbDetails = getFrontendMap(loadBalancers);
        if (!CollectionUtils.isEmpty(lbDetails)) {
            properties.put("loadbalancers", Map.of("frontends", lbDetails,
                    "floatingIpEnabled", isFloatingIpEnabled(loadBalancers)));
        }
        return properties;
    }

    private List<Map<String, String>> getFrontendMap(Set<LoadBalancer> loadBalancers) {
        return loadBalancers.stream()
                .filter(lb -> isNotEmpty(lb.getIp()))
                .map(lb -> Map.of("type", lb.getType().name(), "ip", lb.getIp()))
                .collect(Collectors.toList());
    }

    private boolean isFloatingIpEnabled(Set<LoadBalancer> loadBalancers) {
        return loadBalancers.stream().anyMatch(lb -> LoadBalancerType.GATEWAY_PRIVATE == lb.getType())
                && loadBalancers.stream().anyMatch(lb -> LoadBalancerType.PRIVATE == lb.getType());
    }

    private boolean isRangerAuthorizerEnabled(ClouderaManagerRepo clouderaManagerRepo) {
        return isVersionNewerOrEqualThanLimited(
                clouderaManagerRepo.getVersion(), CLOUDERAMANAGER_VERSION_7_2_0);
    }

    private boolean isImpalaStickySessionEnabled(StackDto stackDto) {
        List<String> coordinators = componentLocatorService.getImpalaCoordinatorLocations(stackDto)
                .values()
                .stream()
                .flatMap(List::stream)
                .collect(toList());
        return coordinators.size() > 1;
    }

    private void saveIdBrokerPillar(ClusterView cluster, Map<String, SaltPillarProperties> servicePillar) {
        IdBroker clusterIdBroker = idBrokerService.getByCluster(cluster.getId());
        LOGGER.info("Put idbroker keys/secrets to salt pillar for cluster: " + cluster.getName());
        servicePillar.put(IDBROKER_PILLAR_KEY, getIdBrokerPillarProperties(clusterIdBroker));
    }

    public SaltPillarProperties getIdBrokerPillarProperties(IdBroker clusterIdBroker) {
        Map<String, Object> idbroker = new HashMap<>();
        if (clusterIdBroker != null) {
            idbroker.put("signpub", clusterIdBroker.getSignPub());
            idbroker.put("signcert", clusterIdBroker.getSignCert());
            idbroker.put("signkey", clusterIdBroker.getSignKey());
            idbroker.put("mastersecret", clusterIdBroker.getMasterSecret());
            idbroker.put("knoxIdBrokerSecurityDir", knoxIdBrokerSecurityDir);
        }
        return new SaltPillarProperties("/idbroker/init.sls", singletonMap("idbroker", idbroker));
    }

    private Map<String, Object> createGatewayUserFacingCertAndFqdn(GatewayConfig gatewayConfig, StackDto stackDto) {
        boolean userFacingCertHasBeenGenerated = isNotEmpty(gatewayConfig.getUserFacingCert())
                && isNotEmpty(gatewayConfig.getUserFacingKey());
        Map<String, Object> gateway = new HashMap<>();
        ClusterView cluster = stackDto.getCluster();
        StackView stack = stackDto.getStack();
        if (userFacingCertHasBeenGenerated) {
            gateway.put("userfacingcert_configured", Boolean.TRUE);
            gateway.put("userfacingkey", stackDto.getSecurityConfig().getUserFacingKey());
            gateway.put("userfacingcert", stackDto.getSecurityConfig().getUserFacingCert());
        }

        String fqdn = loadBalancerFqdnUtil.getLoadBalancerUserFacingFQDN(stack.getId());
        fqdn = isEmpty(fqdn) ? cluster.getFqdn() : fqdn;

        if (isNotEmpty(fqdn)) {
            gateway.put("userfacingfqdn", fqdn);
            if (!InetAddressUtils.isIPv4Address(fqdn)) {
                String[] fqdnParts = fqdn.split("\\.", 2);
                if (fqdnParts.length == 2) {
                    gateway.put("userfacingdomain", Pattern.quote(fqdnParts[1]));
                }
            }
        }
        return gateway;
    }

    private Map<String, Object> createGatewayAlternativeUserFacingCert(GatewayConfig gatewayConfig) {
        boolean alternativeUserFacingCertHasBeenGenerated = isNotEmpty(gatewayConfig.getAlternativeUserFacingCert())
                && isNotEmpty(gatewayConfig.getAlternativeUserFacingKey());
        Map<String, Object> gateway = new HashMap<>();
        if (alternativeUserFacingCertHasBeenGenerated) {
            gateway.put("alternativeuserfacingcert_configured", Boolean.TRUE);
            gateway.put("alternativeuserfacingkey", gatewayConfig.getAlternativeUserFacingKey());
            gateway.put("alternativeuserfacingcert", gatewayConfig.getAlternativeUserFacingCert());
        }
        return gateway;
    }

    public Map<String, List<String>> getServiceLocations(StackDto stackDto) {
        Set<String> serviceNames = exposedServiceCollector.getAllServiceNames();
        Map<String, List<String>> componentLocation = componentLocator.getComponentLocation(stackDto, serviceNames);
        ExposedService impalaService = exposedServiceCollector.getImpalaService();
        if (componentLocation.containsKey(impalaService.getServiceName())) {
            // IMPALA_DEBUG_UI role is not a valid role, but we need to distinguish the 2 roles in order to generate the Knox topology file
            componentLocation.put(exposedServiceCollector.getImpalaDebugUIService().getServiceName(),
                    List.copyOf(componentLocation.get(impalaService.getServiceName())));
            Map<String, List<String>> impalaLocations = componentLocator.getImpalaCoordinatorLocations(stackDto);
            List<String> locations = impalaLocations.values().stream().flatMap(List::stream).collect(Collectors.toList());
            if (!locations.isEmpty()) {
                componentLocation.replace(impalaService.getServiceName(), locations);
            }
        }
        return componentLocation;
    }

    private List<String> getRangerFqdn(StackDto stackDto, String primaryGatewayFqdn, List<String> rangerLocations) {
        if (rangerLocations.size() > 1) {
            // SDX HA has multiple ranger instances in different groups, in Knox we only want to expose the ones on the gateway.
            InstanceGroupView gatewayInstanceGroup = stackDto.getPrimaryGatewayGroup();
            String gatewayGroupName = gatewayInstanceGroup.getGroupName();
            List<String> hosts = rangerLocations.stream()
                    .filter(s -> s.contains(gatewayGroupName))
                    .collect(Collectors.toList());
            // If the ranger is not on Gateway node we have to expose other node.
            if (CollectionUtils.isEmpty(hosts)) {
                return rangerLocations;
            }
            return hosts;
        }
        return rangerLocations.contains(primaryGatewayFqdn) ? asList(primaryGatewayFqdn) : asList(rangerLocations.iterator().next());
    }

    private List<String> asList(String value) {
        return List.of(value);
    }

    private List<Map<String, Object>> getTopologies(GatewayView clusterGateway, String version) throws IOException {
        if (!CollectionUtils.isEmpty(clusterGateway.getTopologies())) {
            List<Map<String, Object>> topologyMaps = new ArrayList<>();
            for (GatewayTopology topology : clusterGateway.getTopologies()) {
                Map<String, Object> topologyAndExposed = mapTopologyToMap(topology, Optional.ofNullable(version));
                topologyMaps.add(topologyAndExposed);
            }
            return topologyMaps;
        }
        return Collections.emptyList();
    }

    private Map<String, Object> mapTopologyToMap(GatewayTopology gt, Optional<String> version) throws IOException {
        Map<String, Object> topology = new HashMap<>();
        topology.put("name", gt.getTopologyName());
        Json exposedJson = gt.getExposedServices();
        if (exposedJson != null && isNotEmpty(exposedJson.getValue())) {
            ExposedServices exposedServicesDomain = exposedJson.get(ExposedServices.class);
            Set<String> exposedServices = exposedServiceCollector
                    .getFullServiceListBasedOnList(exposedServicesDomain.getServices(), version);
            topology.put("exposed", exposedServices);
        } else {
            topology.put("exposed", new HashSet<>());
        }
        return topology;
    }

    private void saveLdapPillar(LdapView ldapView, Map<String, SaltPillarProperties> servicePillar) {
        if (ldapView != null) {
            servicePillar.put("ldap", new SaltPillarProperties("/gateway/ldap.sls", singletonMap("ldap", ldapView)));
        }
    }

    public Map<String, String> collectUpscaleCandidates(StackDto stack, Map<String, Integer> hostGroupWithAdjustment, boolean includeCreatedOnly) {
        Map<String, String> hostNames = new HashMap<>();
        for (Map.Entry<String, Integer> entry : hostGroupWithAdjustment.entrySet()) {
            String instanceGroupName = entry.getKey();
            Integer adjustment = entry.getValue();
            InstanceGroupDto instanceGroupDto = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
            if (instanceGroupDto != null) {
                Collection<InstanceMetadataView> instanceMetaDataSet = includeCreatedOnly
                        ? stack.getUnusedHostsInInstanceGroup(instanceGroupName)
                        : stack.getAliveInstancesInInstanceGroup(instanceGroupName);
                instanceMetaDataSet.stream()
                        .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                        .sorted(Comparator.comparing(InstanceMetadataView::getStartDate).reversed())
                        .limit(adjustment.longValue())
                        .forEach(im -> hostNames.put(im.getDiscoveryFQDN(), im.getPrivateIp()));
            }
        }
        return hostNames;
    }

    public Map<String, String> collectUpscaleCandidates(StackDto stackDto, Map<String, Integer> hostGroupWithAdjustment) {
        return collectUpscaleCandidates(stackDto, hostGroupWithAdjustment, true);
    }

    private void decoratePillarWithJdbcConnectors(ClusterView cluster, Map<String, SaltPillarProperties> servicePillar) {
        Set<RdsConfigWithoutCluster> rdsConfigs = rdsConfigWithoutClusterService.findByClusterId(cluster.getId());
        Map<String, Object> connectorJarUrlsByVendor = new HashMap<>();
        rdsConfigs.stream()
                .filter(rds -> isNotEmpty(rds.getConnectorJarUrl()))
                .forEach(rdsConfig -> {
                    connectorJarUrlsByVendor.put("databaseType", rdsConfig.getDatabaseEngine().databaseType());
                    connectorJarUrlsByVendor.put("connectorJarUrl", rdsConfig.getConnectorJarUrl());
                    connectorJarUrlsByVendor.put("connectorJarName", rdsConfig.getDatabaseEngine().connectorJarName());
                });
        if (!connectorJarUrlsByVendor.isEmpty()) {
            Map<String, Object> jdbcConnectors = singletonMap("jdbc_connectors", connectorJarUrlsByVendor);
            servicePillar.put("jdbc-connectors", new SaltPillarProperties("/jdbc/connectors.sls", jdbcConnectors));
        }
    }

    private void modifyStartupMountRole(StackDto stackDto, Set<Node> nodes, GrainOperation operation) throws CloudbreakOrchestratorFailedException {
        OrchestratorGrainRunnerParams stateParams = createStartupMountGrainRunnerParams(stackDto, nodes, operation);
        LOGGER.debug("{} 'startup' role with params {}", operation.name().toLowerCase(Locale.US), stateParams);
        hostOrchestrator.runOrchestratorGrainRunner(stateParams);
    }

    private OrchestratorGrainRunnerParams createStartupMountGrainRunnerParams(StackDto stackDto, Set<Node> nodes, GrainOperation operation) {
        return createOrchestratorGrainRunnerParams(stackDto, nodes, operation);
    }

    private OrchestratorGrainRunnerParams createOrchestratorGrainRunnerParams(StackDto stackDto, Set<Node> nodes, GrainOperation grainOperation) {
        Set<String> reachableHostnames = nodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        OrchestratorGrainRunnerParams grainRunnerParams = new OrchestratorGrainRunnerParams();
        InstanceMetadataView gatewayInstance = stackDto.getPrimaryGatewayInstance();
        StackView stack = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        grainRunnerParams.setPrimaryGatewayConfig(gatewayConfigService.getGatewayConfig(stack, stackDto.getSecurityConfig(), gatewayInstance,
                stackDto.hasGateway()));
        grainRunnerParams.setTargetHostNames(reachableHostnames);
        grainRunnerParams.setAllNodes(nodes);
        grainRunnerParams.setExitCriteriaModel(ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), cluster.getId()));
        grainRunnerParams.setKey("roles");
        grainRunnerParams.setValue("startup_mount");
        grainRunnerParams.setGrainOperation(grainOperation);
        return grainRunnerParams;
    }

    public void createCronForUserHomeCreation(StackDto stackDto, Set<String> candidateHostNames) throws CloudbreakException {
        Set<String> reachableTargets = stackUtil.collectReachableAndUnreachableCandidateNodes(stackDto, candidateHostNames).getReachableHosts();
        ExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stackDto.getId(), stackDto.getCluster().getId());
        List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
        try {
            hostOrchestrator.createCronForUserHomeCreation(allGatewayConfigs, reachableTargets, exitModel);
        } catch (CloudbreakOrchestratorFailedException e) {
            String message = "Creating cron for user home creation failed";
            LOGGER.warn(message, e);
            throw new CloudbreakException(message, e);
        }
    }

    private void addRangerRazServiceIfAvailable(StackDto stackDto, Map<String, List<String>> serviceLocations, GatewayConfig gatewayConfig) {
        TemplatePreparationObject templatePreparationObject = stackToTemplatePreparationObjectConverter.convert(stackDto);
        final Set<String> hostGroups;

        // Determine list of host groups where RAZ service is going to be configured later
        if (StackType.DATALAKE.equals(stackDto.getStack().getType())) {
            hostGroups = rangerRazDatalakeConfigProvider.getHostGroups((CmTemplateProcessor)
                    templatePreparationObject.getBlueprintView().getProcessor(), templatePreparationObject);
        } else if (StackType.WORKLOAD.equals(stackDto.getStack().getType())) {
            hostGroups = rangerRazDatahubConfigProvider.getHostGroups((CmTemplateProcessor)
                    templatePreparationObject.getBlueprintView().getProcessor(), templatePreparationObject);
        } else {
            hostGroups = null;
        }

        if (!CollectionUtils.isEmpty(hostGroups)) {
            LOGGER.info("List of host groups for RAZ service  {} ", hostGroups);
            ExposedService rangerRazService = exposedServiceCollector.getRangerRazService();
            List<String> rangerRazLocations = new ArrayList<>();

            hostGroupService.getByCluster(stackDto.getCluster().getId())
                    .stream()
                    .filter(hg -> hg.getInstanceGroup() != null && hostGroups.contains(hg.getName()))
                    .forEach(host -> {
                        List<String> hosts = stackDto.getAliveInstancesInInstanceGroup(host.getInstanceGroup().getGroupName()).stream()
                                .filter(ig -> ig.isReachable() && ig.getDiscoveryFQDN() != null)
                                .map(InstanceMetadataView::getDiscoveryFQDN)
                                .collect(toList());
                        rangerRazLocations.addAll(hosts);
                    });

            LOGGER.info("Ranger RAZ Locations {} ", rangerRazLocations);

            if (!CollectionUtils.isEmpty(rangerRazLocations)) {
                List<String> rangerRazGatewayHosts = getRangerFqdn(stackDto, gatewayConfig.getHostname(), rangerRazLocations);
                LOGGER.info("Ranger RAZ Gateway Hosts  {} ", rangerRazGatewayHosts);
                serviceLocations.put(rangerRazService.getServiceName(), rangerRazGatewayHosts);
            }

        }
    }
}
