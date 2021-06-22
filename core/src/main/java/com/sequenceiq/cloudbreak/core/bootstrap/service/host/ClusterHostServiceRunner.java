package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import static java.util.Collections.singletonMap;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.CMLicenseParser;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.cluster.model.ServiceLocationMap;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.CsdParcelDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.HostAttributeDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.TelemetryDecorator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MountDisks;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Component
public class ClusterHostServiceRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterHostServiceRunner.class);

    private static final int CM_HTTP_PORT = 7180;

    private static final int CM_HTTPS_PORT = 7183;

    @Value("${cb.cm.heartbeat.interval}")
    private String cmHeartbeatInterval;

    @Value("${cb.cm.missed.heartbeat.interval}")
    private String cmMissedHeartbeatInterval;

    @Value("${cb.cm.kerberos.encryption.type}")
    private String defaultKerberosEncryptionType;

    @Inject
    private StackService stackService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ComponentLocatorService componentLocator;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private ProxyConfigProvider proxyConfigProvider;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

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
    private TelemetryDecorator telemetryDecorator;

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
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private IdBrokerService idBrokerService;

    @Inject
    private CsdParcelDecorator csdParcelDecorator;

    @Inject
    private EntitlementService entitlementService;

    public void runClusterServices(@Nonnull Stack stack, @Nonnull Cluster cluster, Map<String, String> candidateAddresses) {
        try {
            Set<Node> reachableNodes = stackUtil.collectAndCheckReachableNodes(stack, candidateAddresses.keySet());
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            SaltConfig saltConfig = createSaltConfig(stack, cluster, primaryGatewayConfig, gatewayConfigs, reachableNodes);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
            hostOrchestrator.initServiceRun(gatewayConfigs, reachableNodes, saltConfig, exitCriteriaModel);
            if (CollectionUtils.isEmpty(candidateAddresses)) {
                mountDisks.mountAllDisks(stack.getId());
            } else {
                mountDisks.mountDisksOnNewNodes(stack.getId(), new HashSet<>(candidateAddresses.values()), reachableNodes);
            }
            recipeEngine.executePreClusterManagerRecipes(stack, hostGroupService.getRecipesByCluster(cluster.getId()));
            hostOrchestrator.runService(gatewayConfigs, reachableNodes, saltConfig, exitCriteriaModel);
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

    public void updateClusterConfigs(@Nonnull Stack stack, @Nonnull Cluster cluster) {
        try {
            Set<Node> nodes = stackUtil.collectReachableNodesByInstanceStates(stack);
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            SaltConfig saltConfig = createSaltConfig(stack, cluster, primaryGatewayConfig, gatewayConfigs, nodes);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
            hostOrchestrator.initSaltConfig(gatewayConfigs, nodes, saltConfig, exitCriteriaModel);
            hostOrchestrator.runService(gatewayConfigs, nodes, saltConfig, exitCriteriaModel);
        } catch (CloudbreakOrchestratorException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public Map<String, String> addClusterServices(Long stackId, String hostGroupName, Integer scalingAdjustment) {
        Map<String, String> candidates;
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = stack.getCluster();
        candidates = collectUpscaleCandidates(cluster.getId(), hostGroupName, scalingAdjustment);
        runClusterServices(stack, cluster, candidates);
        return candidates;
    }

    public String changePrimaryGateway(Stack stack) throws CloudbreakException {
        GatewayConfig formerPrimaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Optional<GatewayConfig> newPrimaryCandidate = gatewayConfigs.stream().filter(gc -> !gc.isPrimary()).findFirst();
        if (newPrimaryCandidate.isPresent()) {
            GatewayConfig newPrimary = newPrimaryCandidate.get();
            Set<Node> allNodes = stackUtil.collectNodes(stack);
            try {
                hostOrchestrator.changePrimaryGateway(formerPrimaryGatewayConfig, newPrimary, gatewayConfigs,
                        allNodes, clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
                return newPrimary.getHostname();
            } catch (CloudbreakOrchestratorException ex) {
                throw new CloudbreakException(ex);
            }
        } else {
            throw new CloudbreakException("Primary gateway change is not possible because there is no available node for the action");
        }
    }

    public void redeployGatewayCertificate(@Nonnull Stack stack, @Nonnull Cluster cluster) {
        try {
            Set<Node> nodes = stackUtil.collectReachableNodes(stack);
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            SaltConfig saltConfig = createSaltConfig(stack, cluster, primaryGatewayConfig, gatewayConfigs, nodes);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
            hostOrchestrator.uploadGatewayPillar(gatewayConfigs, nodes, exitCriteriaModel, saltConfig);
            hostOrchestrator.runService(gatewayConfigs, nodes, saltConfig, exitCriteriaModel);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private SaltConfig createSaltConfig(Stack stack, Cluster cluster, GatewayConfig primaryGatewayConfig, Iterable<GatewayConfig> gatewayConfigs,
            Set<Node> nodes)
            throws IOException, CloudbreakOrchestratorException {
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId());
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        saveCustomNameservers(stack, kerberosConfig, servicePillar);
        addKerberosConfig(servicePillar, kerberosConfig);
        servicePillar.putAll(hostAttributeDecorator.createHostAttributePillars(stack, nodes));
        servicePillar.put("discovery", new SaltPillarProperties("/discovery/init.sls", singletonMap("platform", stack.cloudPlatform())));
        String virtualGroupsEnvironmentCrn = environmentConfigProvider.getParentEnvironmentCrn(stack.getEnvironmentCrn());
        boolean deployedInChildEnvironment = !virtualGroupsEnvironmentCrn.equals(stack.getEnvironmentCrn());
        Map<String, ? extends Serializable> clusterProperties = Map.of("name", stack.getCluster().getName(),
                "deployedInChildEnvironment", deployedInChildEnvironment);
        servicePillar.put("metadata", new SaltPillarProperties("/metadata/init.sls", singletonMap("cluster", clusterProperties)));
        ClusterPreCreationApi connector = clusterApiConnectors.getConnector(cluster);
        Map<String, List<String>> serviceLocations = getServiceLocations(cluster);
        Optional<LdapView> ldapView = ldapConfigService.get(stack.getEnvironmentCrn(), stack.getName());
        VirtualGroupRequest virtualGroupRequest = getVirtualGroupRequest(virtualGroupsEnvironmentCrn, ldapView);
        saveGatewayPillar(primaryGatewayConfig, cluster, stack, servicePillar, virtualGroupRequest,
                connector, kerberosConfig, serviceLocations, clouderaManagerRepo);
        saveIdBrokerPillar(cluster, servicePillar);
        postgresConfigService.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack, cluster);

        if (blueprintService.isClouderaManagerTemplate(cluster.getBlueprint())) {
            addClouderaManagerConfig(stack, cluster, servicePillar, clouderaManagerRepo);
        }
        ldapView.ifPresent(ldap -> saveLdapPillar(ldap, servicePillar));

        saveSssdAdPillar(servicePillar, kerberosConfig);
        saveSssdIpaPillar(servicePillar, kerberosConfig, serviceLocations);
        saveDockerPillar(cluster.getExecutorType(), servicePillar);

        proxyConfigProvider.decoratePillarWithProxyDataIfNeeded(servicePillar, cluster);

        decoratePillarWithJdbcConnectors(cluster, servicePillar);

        return new SaltConfig(servicePillar, grainPropertiesService.createGrainProperties(gatewayConfigs, cluster, nodes));
    }

    private void addKerberosConfig(Map<String, SaltPillarProperties> servicePillar, KerberosConfig kerberosConfig) throws IOException {
        if (isKerberosNeeded(kerberosConfig)) {
            Map<String, String> kerberosPillarConf = new HashMap<>();
            if (isEmpty(kerberosConfig.getDescriptor())) {
                putIfNotNull(kerberosPillarConf, kerberosConfig.getUrl(), "url");
                putIfNotNull(kerberosPillarConf, kerberosDetailService.resolveHostForKdcAdmin(kerberosConfig, kerberosConfig.getUrl()), "adminUrl");
                putIfNotNull(kerberosPillarConf, kerberosConfig.getRealm(), "realm");
            } else {
                Map<String, Object> properties = kerberosDetailService.getKerberosEnvProperties(kerberosConfig);
                putIfNotNull(kerberosPillarConf, properties.get("kdc_hosts"), "url");
                putIfNotNull(kerberosPillarConf, properties.get("admin_server_host"), "adminUrl");
                putIfNotNull(kerberosPillarConf, properties.get("realm"), "realm");
            }
            putIfNotNull(kerberosPillarConf, defaultKerberosEncryptionType, "encryptionType");
            putIfNotNull(kerberosPillarConf, kerberosConfig.getVerifyKdcTrust().toString(), "verifyKdcTrust");
            servicePillar.put("kerberos", new SaltPillarProperties("/kerberos/init.sls", singletonMap("kerberos", kerberosPillarConf)));
        }
    }

    private boolean isKerberosNeeded(KerberosConfig kerberosConfig) throws IOException {
        return kerberosConfig != null
                && kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)
                && !kerberosDetailService.isClusterManagerManagedKrb5Config(kerberosConfig);
    }

    private void addClouderaManagerConfig(Stack stack, Cluster cluster, Map<String, SaltPillarProperties> servicePillar, ClouderaManagerRepo clouderaManagerRepo)
            throws CloudbreakOrchestratorFailedException {
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stack.getId());
        DataBusCredential dataBusCredential = null;
        if (StringUtils.isNotBlank(cluster.getDatabusCredential())) {
            try {
                dataBusCredential = new Json(cluster.getDatabusCredential()).get(DataBusCredential.class);
            } catch (IOException e) {
                LOGGER.error("Cannot read DataBus secrets from cluster entity. Continue without databus secrets", e);
            }
        }
        telemetryDecorator.decoratePillar(servicePillar, stack, telemetry, dataBusCredential);
        decoratePillarWithTags(stack, servicePillar);
        decorateWithClouderaManagerEntrerpriseDetails(telemetry, servicePillar);
        Optional<String> licenseOpt = decoratePillarWithClouderaManagerLicense(stack.getId(), servicePillar);
        decoratePillarWithClouderaManagerRepo(clouderaManagerRepo, servicePillar, licenseOpt);
        decoratePillarWithClouderaManagerDatabase(cluster, servicePillar);
        decoratePillarWithClouderaManagerCommunicationSettings(cluster, servicePillar);
        decoratePillarWithClouderaManagerAutoTls(cluster, servicePillar);
        csdParcelDecorator.decoratePillarWithCsdParcels(stack, servicePillar);
        decoratePillarWithClouderaManagerSettings(servicePillar, clouderaManagerRepo, stack);
    }

    private void saveSssdAdPillar(Map<String, SaltPillarProperties> servicePillar, KerberosConfig kerberosConfig) {
        if (kerberosDetailService.isAdJoinable(kerberosConfig)) {
            Map<String, Object> sssdConnfig = new HashMap<>();
            sssdConnfig.put("username", kerberosConfig.getPrincipal());
            sssdConnfig.put("domainuppercase", kerberosConfig.getRealm().toUpperCase());
            sssdConnfig.put("domain", kerberosConfig.getRealm().toLowerCase());
            sssdConnfig.put("password", kerberosConfig.getPassword());
            servicePillar.put("sssd-ad", new SaltPillarProperties("/sssd/ad.sls", singletonMap("sssd-ad", sssdConnfig)));
        }
    }

    private VirtualGroupRequest getVirtualGroupRequest(String virtualGroupsEnvironmentCrn, Optional<LdapView> ldapView) {
        String adminGroup = ldapView.isPresent() ? ldapView.get().getAdminGroup() : "";
        return new VirtualGroupRequest(virtualGroupsEnvironmentCrn, adminGroup);
    }

    private void saveSssdIpaPillar(Map<String, SaltPillarProperties> servicePillar, KerberosConfig kerberosConfig,
            Map<String, List<String>> serviceLocations) {
        if (kerberosDetailService.isIpaJoinable(kerberosConfig)) {
            Map<String, Object> sssdConnfig = new HashMap<>();
            sssdConnfig.put("principal", kerberosConfig.getPrincipal());
            sssdConnfig.put("realm", kerberosConfig.getRealm().toUpperCase());
            sssdConnfig.put("domain", kerberosConfig.getDomain());
            sssdConnfig.put("password", kerberosConfig.getPassword());
            sssdConnfig.put("server", kerberosConfig.getUrl());
            sssdConnfig.put("dns_ttl", kerberosDetailService.getDnsTtl());
            // enumeration has performance impacts so it's only enabled if Ranger is installed on the cluster
            // otherwise the usersync does not work with nss
            boolean enumerate = !CollectionUtils.isEmpty(serviceLocations.get("RANGER_ADMIN"))
                    || !CollectionUtils.isEmpty(serviceLocations.get("NIFI_REGISTRY_SERVER"))
                    || !CollectionUtils.isEmpty(serviceLocations.get("NIFI_NODE"));
            sssdConnfig.put("enumerate", enumerate);
            servicePillar.put("sssd-ipa", new SaltPillarProperties("/sssd/ipa.sls", singletonMap("sssd-ipa", sssdConnfig)));
        }
    }

    // Right now we are assuming that CM enterprise is enabled if workload analytics is used
    // In the future that should be enabled based on the license
    private void decorateWithClouderaManagerEntrerpriseDetails(Telemetry telemetry, Map<String, SaltPillarProperties> servicePillar) {
        if (telemetry != null && telemetry.getWorkloadAnalytics() != null) {
            servicePillar.put("cloudera-manager-cme",
                    new SaltPillarProperties("/cloudera-manager/cme.sls", singletonMap("cloudera-manager", singletonMap("cme_enabled", true))));
        }
    }

    private void decoratePillarWithClouderaManagerDatabase(Cluster cluster, Map<String, SaltPillarProperties> servicePillar)
            throws CloudbreakOrchestratorFailedException {
        RDSConfig clouderaManagerRdsConfig = rdsConfigService.findByClusterIdAndType(cluster.getId(), DatabaseType.CLOUDERA_MANAGER);
        if (clouderaManagerRdsConfig == null) {
            throw new CloudbreakOrchestratorFailedException("Cloudera Manager RDSConfig is missing for stack");
        }
        RdsView rdsView = new RdsView(rdsConfigService.resolveVaultValues(clouderaManagerRdsConfig));
        servicePillar.put("cloudera-manager-database",
                new SaltPillarProperties("/cloudera-manager/database.sls", singletonMap("cloudera-manager", singletonMap("database", rdsView))));
    }

    private void decoratePillarWithClouderaManagerCommunicationSettings(Cluster cluster, Map<String, SaltPillarProperties> servicePillar) {
        Boolean autoTls = cluster.getAutoTlsEnabled();
        Map<String, Object> communication = new HashMap<>();
        communication.put("port", autoTls ? CM_HTTPS_PORT : CM_HTTP_PORT);
        communication.put("protocol", autoTls ? "https" : "http");
        communication.put("autotls_enabled", autoTls);
        servicePillar.put("cloudera-manager-communication", new SaltPillarProperties("/cloudera-manager/communication.sls",
                singletonMap("cloudera-manager", singletonMap("communication", communication))));
    }

    private void decoratePillarWithClouderaManagerAutoTls(Cluster cluster, Map<String, SaltPillarProperties> servicePillar) {
        if (cluster.getAutoTlsEnabled()) {
            Map<String, Object> autoTls = new HashMap<>();
            autoTls.put("keystore_password", cluster.getKeyStorePwd());
            autoTls.put("truststore_password", cluster.getTrustStorePwd());
            servicePillar.put("cloudera-manager-autotls", new SaltPillarProperties("/cloudera-manager/autotls.sls",
                    singletonMap("cloudera-manager", singletonMap("autotls", autoTls))));
        }
    }

    public Optional<String> decoratePillarWithClouderaManagerLicense(Long stackId, Map<String, SaltPillarProperties> servicePillar) {
        String userCrn = stackService.get(stackId).getCreator().getUserCrn();
        Account account = umsClient.getAccountDetails(Crn.safeFromString(userCrn).getAccountId(), Optional.empty());
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

    public void decoratePillarWithClouderaManagerSettings(Map<String, SaltPillarProperties> servicePillar, ClouderaManagerRepo clouderaManagerRepo,
            Stack stack) {
        ServiceLocationMap serviceLocations = clusterApiConnectors.getConnector(stack.getCluster()).getServiceLocations();
        String cmVersion = clouderaManagerRepo.getVersion();
        boolean disableAutoBundleCollection = entitlementService.cmAutoBundleCollectionDisabled(Crn.safeFromString(stack.getResourceCrn()).getAccountId());
        servicePillar.put("cloudera-manager-settings", new SaltPillarProperties("/cloudera-manager/settings.sls",
                singletonMap("cloudera-manager", Map.of(
                        "settings", Map.of(
                                "heartbeat_interval", cmHeartbeatInterval,
                                "missed_heartbeat_interval", cmMissedHeartbeatInterval,
                                "disable_auto_bundle_collection", disableAutoBundleCollection,
                                "set_cdp_env", isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_0_2),
                                "deterministic_uid_gid", isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_1),
                                "cloudera_scm_sudo_access", CMRepositoryVersionUtil.isSudoAccessNeededForHostCertRotation(clouderaManagerRepo)),
                        "mgmt_service_directories", serviceLocations.getAllVolumePath()))));
    }

    private void decoratePillarWithTags(Stack stack, Map<String, SaltPillarProperties> servicePillarConfig) {
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

    private void saveCustomNameservers(Stack stack, KerberosConfig kerberosConfig, Map<String, SaltPillarProperties> servicePillar) {
        if (kerberosConfig != null && StringUtils.isNotBlank(kerberosConfig.getDomain()) && StringUtils.isNotBlank(kerberosConfig.getNameServers())) {
            List<String> ipList = Lists.newArrayList(kerberosConfig.getNameServers().split(","));
            servicePillar.put("forwarder-zones", new SaltPillarProperties("/unbound/forwarders.sls",
                    singletonMap("forwarder-zones", singletonMap(kerberosConfig.getDomain(), singletonMap("nameservers", ipList)))));
        } else if (kerberosConfig == null || (kerberosConfig.getType() != KerberosType.FREEIPA && kerberosConfig.getType() != KerberosType.ACTIVE_DIRECTORY)) {
            saveDatalakeNameservers(stack, servicePillar);
        }
    }

    private void saveDatalakeNameservers(Stack stack, Map<String, SaltPillarProperties> servicePillar) {
        Long datalakeResourceId = stack.getDatalakeResourceId();
        if (datalakeResourceId != null) {
            Optional<DatalakeResources> datalakeResource = datalakeResourcesService.findById(datalakeResourceId);
            if (datalakeResource.isPresent() && datalakeResource.get().getDatalakeStackId() != null) {
                Long datalakeStackId = datalakeResource.get().getDatalakeStackId();
                Stack dataLakeStack = stackService.getByIdWithListsInTransaction(datalakeStackId);
                String datalakeDomain = dataLakeStack.getGatewayInstanceMetadata().get(0).getDomain();
                List<String> ipList = dataLakeStack.getGatewayInstanceMetadata().stream().map(InstanceMetaData::getPrivateIp).collect(Collectors.toList());
                servicePillar.put("forwarder-zones", new SaltPillarProperties("/unbound/forwarders.sls",
                        singletonMap("forwarder-zones", singletonMap(datalakeDomain, singletonMap("nameservers", ipList)))));
            }
        }
    }

    @SuppressWarnings("ParameterNumber")
    private void saveGatewayPillar(GatewayConfig gatewayConfig, Cluster cluster, Stack stack,
            Map<String, SaltPillarProperties> servicePillar,
            VirtualGroupRequest virtualGroupRequest,
            ClusterPreCreationApi connector, KerberosConfig kerberosConfig,
            Map<String, List<String>> serviceLocations,
            ClouderaManagerRepo clouderaManagerRepo) throws IOException {
        final boolean enableKnoxRangerAuthorizer = isVersionNewerOrEqualThanLimited(
                clouderaManagerRepo.getVersion(), CLOUDERAMANAGER_VERSION_7_2_0);

        Map<String, Object> gateway = new HashMap<>();
        gateway.put("address", gatewayConfig.getPublicAddress());
        gateway.put("username", cluster.getUserName());
        gateway.put("password", cluster.getPassword());
        gateway.put("enable_knox_ranger_authorizer", enableKnoxRangerAuthorizer);
        gateway.put("enable_ccmv2", stack.getTunnel().useCcmV2());

        // for cloudbreak upgradeability
        gateway.put("ssotype", SSOType.NONE);

        Gateway clusterGateway = cluster.getGateway();

        if (clusterGateway != null) {
            gateway.put("path", clusterGateway.getPath());
            gateway.put("ssotype", clusterGateway.getSsoType());
            gateway.put("ssoprovider", clusterGateway.getSsoProvider());
            gateway.put("signpub", clusterGateway.getSignPub());
            gateway.put("signcert", clusterGateway.getSignCert());
            gateway.put("signkey", clusterGateway.getSignKey());
            gateway.put("tokencert", clusterGateway.getTokenCert());
            gateway.put("mastersecret", clusterGateway.getKnoxMasterSecret());
            gateway.put("envAccessGroup", virtualGroupService.getVirtualGroup(virtualGroupRequest, UmsRight.ENVIRONMENT_ACCESS.getRight()));
            List<Map<String, Object>> topologies = getTopologies(clusterGateway);
            gateway.put("topologies", topologies);
            if (cluster.getBlueprint() != null) {
                Boolean autoTlsEnabled = cluster.getAutoTlsEnabled();
                Map<String, Integer> servicePorts = connector.getServicePorts(cluster.getBlueprint(), autoTlsEnabled);
                gateway.put("ports", servicePorts);
                gateway.put("protocol", autoTlsEnabled ? "https" : "http");
            }
            if (SSOType.SSO_PROVIDER_FROM_UMS.equals(clusterGateway.getSsoType())) {
                String accountId = ThreadBasedUserCrnProvider.getAccountId();
                try {
                    String metadataXml = umsClient.getIdentityProviderMetadataXml(accountId);
                    gateway.put("saml", metadataXml);
                } catch (Exception e) {
                    LOGGER.debug("Could not get SAML metadata file to set up IdP in KNOXSSO.", e);
                    throw new NotFoundException("Could not get SAML metadata file to set up IdP in KNOXSSO: " + e.getMessage());
                }
            }
        }
        addGatewayUserFacingCertAndFqdn(gatewayConfig, cluster, gateway);
        gateway.put("kerberos", kerberosConfig != null);

        ExposedService rangerService = exposedServiceCollector.getRangerService();
        List<String> rangerLocations = serviceLocations.get(rangerService.getServiceName());
        if (!CollectionUtils.isEmpty(rangerLocations)) {
            List<String> rangerGatewayHosts = getRangerFqdn(cluster, gatewayConfig.getHostname(), rangerLocations);
            serviceLocations.put(rangerService.getServiceName(), rangerGatewayHosts);
        }
        serviceLocations.put(exposedServiceCollector.getClouderaManagerService().getServiceName(), asList(gatewayConfig.getHostname()));
        gateway.put("location", serviceLocations);
        if (stack.getNetwork() != null) {
            gateway.put("cidrBlocks", stack.getNetwork().getNetworkCidrs());
        }
        servicePillar.put("gateway", new SaltPillarProperties("/gateway/init.sls", singletonMap("gateway", gateway)));
    }

    private void saveIdBrokerPillar(Cluster cluster, Map<String, SaltPillarProperties> servicePillar) {
        IdBroker clusterIdBroker = idBrokerService.getByCluster(cluster);
        Map<String, Object> idbroker = new HashMap<>();

        if (clusterIdBroker != null) {
            LOGGER.info("Put idbroker keys/secrets to salt pillar for cluster: " + cluster.getName());
            idbroker.put("signpub", clusterIdBroker.getSignPub());
            idbroker.put("signcert", clusterIdBroker.getSignCert());
            idbroker.put("signkey", clusterIdBroker.getSignKey());
            idbroker.put("mastersecret", clusterIdBroker.getMasterSecret());
        }
        servicePillar.put("idbroker", new SaltPillarProperties("/idbroker/init.sls", singletonMap("idbroker", idbroker)));
    }

    private void addGatewayUserFacingCertAndFqdn(GatewayConfig gatewayConfig, Cluster cluster, Map<String, Object> gateway) {
        boolean userFacingCertHasBeenGenerated = isNotEmpty(gatewayConfig.getUserFacingCert())
                && isNotEmpty(gatewayConfig.getUserFacingKey());
        if (userFacingCertHasBeenGenerated) {
            gateway.put("userfacingcert_configured", Boolean.TRUE);
            gateway.put("userfacingkey", cluster.getStack().getSecurityConfig().getUserFacingKey());
            gateway.put("userfacingcert", cluster.getStack().getSecurityConfig().getUserFacingCert());
        }

        String fqdn = loadBalancerConfigService.getLoadBalancerUserFacingFQDN(cluster.getStack().getId());
        fqdn = isEmpty(fqdn) ? cluster.getFqdn() : fqdn;

        if (isNotEmpty(fqdn)) {
            gateway.put("userfacingfqdn", fqdn);
            String[] fqdnParts = fqdn.split("\\.", 2);
            if (fqdnParts.length == 2) {
                gateway.put("userfacingdomain", Pattern.quote(fqdnParts[1]));
            }
        }
    }

    private Map<String, List<String>> getServiceLocations(Cluster cluster) {
        Set<String> serviceNames = exposedServiceCollector.getAllServiceNames();
        Map<String, List<String>> componentLocation = componentLocator.getComponentLocation(cluster, serviceNames);
        ExposedService impalaService = exposedServiceCollector.getImpalaService();
        if (componentLocation.containsKey(impalaService.getServiceName())) {
            // IMPALA_DEBUG_UI role is not a valid role, but we need to distinguish the 2 roles in order to generate the Knox topology file
            componentLocation.put(exposedServiceCollector.getImpalaDebugUIService().getServiceName(),
                    List.copyOf(componentLocation.get(impalaService.getServiceName())));
            Map<String, List<String>> impalaLocations = componentLocator.getImpalaCoordinatorLocations(cluster);
            List<String> locations = impalaLocations.values().stream().flatMap(List::stream).collect(Collectors.toList());
            componentLocation.replace(impalaService.getServiceName(), locations);
        }
        return componentLocation;
    }

    private List<String> getRangerFqdn(Cluster cluster, String primaryGatewayFqdn, List<String> rangerLocations) {
        if (rangerLocations.size() > 1) {
            // SDX HA has multiple ranger instances in different groups, in Knox we only want to expose the ones on the gateway.
            InstanceGroup gatewayInstanceGroup =  instanceGroupService.getPrimaryGatewayInstanceGroupByStackId(cluster.getStack().getId());
            String gatewayGroupName = gatewayInstanceGroup.getGroupName();
            List<String> hosts = rangerLocations.stream()
                    .filter(s -> s.contains(gatewayGroupName))
                    .collect(Collectors.toList());
            return hosts;
        }
        return rangerLocations.contains(primaryGatewayFqdn) ? asList(primaryGatewayFqdn) : asList(rangerLocations.iterator().next());
    }

    private List<String> asList(String value) {
        return List.of(value);
    }

    private List<Map<String, Object>> getTopologies(Gateway clusterGateway) throws IOException {
        if (!CollectionUtils.isEmpty(clusterGateway.getTopologies())) {
            List<Map<String, Object>> topologyMaps = new ArrayList<>();
            for (GatewayTopology topology : clusterGateway.getTopologies()) {
                Map<String, Object> topologyAndExposed = mapTopologyToMap(topology);
                topologyMaps.add(topologyAndExposed);
            }
            return topologyMaps;
        }
        return Collections.emptyList();
    }

    private Map<String, Object> mapTopologyToMap(GatewayTopology gt) throws IOException {
        Map<String, Object> topology = new HashMap<>();
        topology.put("name", gt.getTopologyName());
        Json exposedJson = gt.getExposedServices();
        if (exposedJson != null && isNotEmpty(exposedJson.getValue())) {
            ExposedServices exposedServicesDomain = exposedJson.get(ExposedServices.class);
            Set<String> exposedServices = exposedServiceCollector.getFullServiceListBasedOnList(exposedServicesDomain.getServices());
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

    private void saveDockerPillar(ExecutorType executorType, Map<String, SaltPillarProperties> servicePillar) {
        Map<String, Object> dockerMap = new HashMap<>();

        dockerMap.put("enableContainerExecutor", ExecutorType.CONTAINER.equals(executorType));

        servicePillar.put("docker", new SaltPillarProperties("/docker/init.sls", singletonMap("docker", dockerMap)));
    }

    private Map<String, String> collectUpscaleCandidates(Long clusterId, String hostGroupName, Integer adjustment) {
        HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(clusterId, hostGroupName)
                .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
        if (hostGroup.getInstanceGroup() != null) {
            Long instanceGroupId = hostGroup.getInstanceGroup().getId();
            Map<String, String> hostNames = new HashMap<>();
            instanceMetaDataService.findUnusedHostsInInstanceGroup(instanceGroupId).stream()
                    .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                    .sorted(Comparator.comparing(InstanceMetaData::getStartDate))
                    .limit(adjustment.longValue())
                    .forEach(im -> hostNames.put(im.getDiscoveryFQDN(), im.getPrivateIp()));
            return hostNames;
        }
        return Collections.emptyMap();
    }

    private void putIfNotNull(Map<String, String> context, Object variable, String key) {
        if (variable != null) {
            context.put(key, variable.toString());
        }
    }

    private void decoratePillarWithJdbcConnectors(Cluster cluster, Map<String, SaltPillarProperties> servicePillar) {
        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(cluster.getId());
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
}
