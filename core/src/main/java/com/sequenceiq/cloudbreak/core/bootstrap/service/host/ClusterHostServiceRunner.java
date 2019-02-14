package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService.AMBARI_VERSION_2_7_0_0;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.clusterdefinition.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackViewRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.SmartSenseCredentialConfigService;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariSecurityConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.template.views.LdapView;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class ClusterHostServiceRunner {

    @Inject
    private StackService stackService;

    @Inject
    private StackViewRepository stackViewRepository;

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ComponentLocatorService componentLocator;

    @Inject
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Inject
    private SmartSenseCredentialConfigService smartSenseCredentialConfigService;

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
    private AmbariBlueprintPortConfigCollector ambariBlueprintPortConfigCollector;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private DefaultClouderaManagerRepoService clouderaManagerRepoService;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    public void runClusterServices(Stack stack, Cluster cluster) {
        try {
            Set<Node> nodes = stackUtil.collectNodes(stack);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            SaltConfig saltConfig = createSaltConfig(stack, cluster, primaryGatewayConfig, gatewayConfigs);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
            boolean clouderaManager = clusterDefinitionService.isClouderaManagerBlueprint(cluster.getClusterDefinition());
            hostOrchestrator.initServiceRun(gatewayConfigs, nodes, saltConfig, exitCriteriaModel, clouderaManager);
            recipeEngine.executePreClusterManagerRecipes(stack, hostGroupService.getByCluster(cluster.getId()));
            hostOrchestrator.runService(gatewayConfigs, nodes, saltConfig, exitCriteriaModel);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException | IOException | CloudbreakException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public Map<String, String> addClusterServices(Long stackId, String hostGroupName, Integer scalingAdjustment) throws CloudbreakException {
        Map<String, String> candidates;
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = stack.getCluster();
        candidates = collectUpscaleCandidates(cluster.getId(), hostGroupName, scalingAdjustment);
        runClusterServices(stack, cluster);
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
                hostOrchestratorResolver.get(stack.getOrchestrator().getType()).changePrimaryGateway(formerPrimaryGatewayConfig, newPrimary, gatewayConfigs,
                        allNodes, clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()));
                return newPrimary.getHostname();
            } catch (CloudbreakOrchestratorException ex) {
                throw new CloudbreakException(ex);
            }
        } else {
            throw new CloudbreakException("Primary gateway change is not possible because there is no available node for the action");
        }
    }

    private SaltConfig createSaltConfig(Stack stack, Cluster cluster, GatewayConfig primaryGatewayConfig, Iterable<GatewayConfig> gatewayConfigs)
            throws IOException, CloudbreakOrchestratorException {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        saveCustomNameservers(stack, cluster.getKerberosConfig(), servicePillar);
        if (cluster.getKerberosConfig() != null
                && kerberosDetailService.isAmbariManagedKerberosPackages(cluster.getKerberosConfig())
                && !kerberosDetailService.isAmbariManagedKrb5Conf(cluster.getKerberosConfig())) {
            Map<String, String> kerberosPillarConf = new HashMap<>();
            KerberosConfig kerberosConfig = cluster.getKerberosConfig();
            if (StringUtils.isEmpty(kerberosConfig.getDescriptor())) {
                putIfNotNull(kerberosPillarConf, kerberosConfig.getUrl(), "url");
                putIfNotNull(kerberosPillarConf, kerberosDetailService.resolveHostForKdcAdmin(kerberosConfig, kerberosConfig.getUrl()), "adminUrl");
                putIfNotNull(kerberosPillarConf, kerberosConfig.getRealm(), "realm");
            } else {
                Map<String, Object> properties = kerberosDetailService.getKerberosEnvProperties(kerberosConfig);
                putIfNotNull(kerberosPillarConf, properties.get("kdc_hosts"), "url");
                putIfNotNull(kerberosPillarConf, properties.get("admin_server_host"), "adminUrl");
                putIfNotNull(kerberosPillarConf, properties.get("realm"), "realm");
            }
            putIfNotNull(kerberosPillarConf, kerberosConfig.getVerifyKdcTrust().toString(), "verifyKdcTrust");
            servicePillar.put("kerberos", new SaltPillarProperties("/kerberos/init.sls", singletonMap("kerberos", kerberosPillarConf)));
        }
        servicePillar.put("disk", new SaltPillarProperties("/disk/disk.sls", singletonMap("disk", diskMountParameters(stack))));
        servicePillar.put("discovery", new SaltPillarProperties("/discovery/init.sls", singletonMap("platform", stack.cloudPlatform())));
        servicePillar.put("metadata", new SaltPillarProperties("/metadata/init.sls",
                singletonMap("cluster", singletonMap("name", stack.getCluster().getName()))));
        saveGatewayPillar(primaryGatewayConfig, cluster, servicePillar);

        postgresConfigService.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack, cluster);

        if (clusterDefinitionService.isClouderaManagerBlueprint(cluster.getClusterDefinition())) {
            decoratePillarWithClouderaManagerRepo(stack.getId(), servicePillar);
            decoratePillarWithClouderaManagerDatabase(cluster, servicePillar);
        } else {
            AmbariRepo ambariRepo = clusterComponentConfigProvider.getAmbariRepo(cluster.getId());
            if (ambariRepo != null) {
                Map<String, Object> ambariRepoMap = ambariRepo.asMap();
                String clusterDefinitionText = cluster.getClusterDefinition().getClusterDefinitionText();
                Json clusterDefinition = new Json(clusterDefinitionText);
                ambariRepoMap.put("stack_version", clusterDefinition.getValue("Blueprints.stack_version"));
                ambariRepoMap.put("stack_type", clusterDefinition.getValue("Blueprints.stack_name").toString().toLowerCase());
                servicePillar.put("ambari-repo", new SaltPillarProperties("/ambari/repo.sls", singletonMap("ambari", singletonMap("repo", ambariRepoMap))));
                boolean setupLdapAndSsoOnApi = ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(ambariRepo::getVersion, AMBARI_VERSION_2_7_0_0);
                servicePillar.put("setup-ldap-and-sso-on-api", new SaltPillarProperties("/ambari/config.sls",
                        singletonMap("ambari", singletonMap("setup_ldap_and_sso_on_api", setupLdapAndSsoOnApi))));
            }
            servicePillar.put("ambari-gpl-repo", new SaltPillarProperties("/ambari/gpl.sls", singletonMap("ambari", singletonMap("gpl", singletonMap("enabled",
                    clusterComponentConfigProvider.getHDPRepo(cluster.getId()).isEnableGplRepo())))));
            decoratePillarWithAmbariDatabase(cluster, servicePillar);
        }

        if (cluster.getLdapConfig() != null) {
            LdapConfig ldapConfig = cluster.getLdapConfig().copyWithoutWorkspace();
            saveLdapPillar(ldapConfig, servicePillar);
        }
        saveSssdAdPillar(cluster, servicePillar);
        saveSssdIpaPillar(cluster, servicePillar);
        saveDockerPillar(cluster.getExecutorType(), servicePillar);
        saveHDPPillar(cluster.getId(), servicePillar);
        saveLdapsAdPillar(cluster, servicePillar);
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("username", ambariSecurityConfigProvider.getCloudbreakAmbariUserName(stack.getCluster()));
        credentials.put("password", ambariSecurityConfigProvider.getCloudbreakAmbariPassword(stack.getCluster()));
        credentials.put("securityMasterKey", ambariSecurityConfigProvider.getAmbariSecurityMasterKey(cluster));
        servicePillar.put("ambari-credentials", new SaltPillarProperties("/ambari/credentials.sls", singletonMap("ambari", credentials)));

        if (smartSenseCredentialConfigService.areCredentialsSpecified()) {
            Map<String, Object> smartSenseCredentials = smartSenseCredentialConfigService.getCredentials();
            servicePillar.put("smartsense-credentials", new SaltPillarProperties("/smartsense/credentials.sls", smartSenseCredentials));
        }

        proxyConfigProvider.decoratePillarWithProxyDataIfNeeded(servicePillar, cluster);

        decoratePillarWithJdbcConnectors(cluster, servicePillar);

        return new SaltConfig(servicePillar, createGrainProperties(gatewayConfigs));
    }

    private void saveSssdAdPillar(Cluster cluster, Map<String, SaltPillarProperties> servicePillar) {
        if (cluster.isAdJoinable()) {
            KerberosConfig kerberosConfig = cluster.getKerberosConfig();
            Map<String, Object> sssdConnfig = new HashMap<>();
            sssdConnfig.put("username", kerberosConfig.getPrincipal());
            sssdConnfig.put("domainuppercase", kerberosConfig.getRealm().toUpperCase());
            sssdConnfig.put("domain", kerberosConfig.getRealm().toLowerCase());
            sssdConnfig.put("password", kerberosConfig.getPassword());
            servicePillar.put("sssd-ad", new SaltPillarProperties("/sssd/ad.sls", singletonMap("sssd-ad", sssdConnfig)));
        }
    }

    private void saveLdapsAdPillar(Cluster cluster, Map<String, SaltPillarProperties> servicePillar) {
        if (Objects.nonNull(cluster.getLdapConfig()) && Objects.nonNull(cluster.getLdapConfig().getCertificate())) {
            Map<String, Object> ldapsProperties = new HashMap<>();
            ldapsProperties.put("certPath", ambariSecurityConfigProvider.getCertPath());
            ldapsProperties.put("keystorePassword", ambariSecurityConfigProvider.getKeystorePassword());
            ldapsProperties.put("keystorePath", ambariSecurityConfigProvider.getKeystorePath());
            servicePillar.put("ldaps-ad", new SaltPillarProperties("/ambari/ldaps.sls", singletonMap("ambari", singletonMap("ldaps", ldapsProperties))));
        }
    }

    private Map<String, String> diskMountParameters(Stack stack) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("cloudPlatform", stack.cloudPlatform());

        PlatformParameters platformParameters = connector.getPlatformParameters(stack);
        parameters.put("platformDiskStartLabel", platformParameters.scriptParams().getStartLabel().toString());
        parameters.put("platformDiskPrefix", platformParameters.scriptParams().getDiskPrefix());
        return parameters;
    }

    private void saveSssdIpaPillar(Cluster cluster, Map<String, SaltPillarProperties> servicePillar) {
        if (cluster.isIpaJoinable()) {
            KerberosConfig kerberosConfig = cluster.getKerberosConfig();
            Map<String, Object> sssdConnfig = new HashMap<>();
            sssdConnfig.put("principal", kerberosConfig.getPrincipal());
            sssdConnfig.put("realm", kerberosConfig.getRealm().toUpperCase());
            sssdConnfig.put("domain", kerberosConfig.getDomain());
            sssdConnfig.put("password", kerberosConfig.getPassword());
            sssdConnfig.put("server", kerberosConfig.getUrl());
            servicePillar.put("sssd-ipa", new SaltPillarProperties("/sssd/ipa.sls", singletonMap("sssd-ipa", sssdConnfig)));
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

    private void decoratePillarWithClouderaManagerRepo(Long stackId, Map<String, SaltPillarProperties> servicePillar)
            throws CloudbreakOrchestratorFailedException {
        try {
            String osType = componentConfigProvider.getImage(stackId).getOsType();
            servicePillar.put("cloudera-manager-repo", new SaltPillarProperties("/cloudera-manager/repo.sls",
                    singletonMap("cloudera-manager", singletonMap("repo", clouderaManagerRepoService.getDefault(osType)))));
        } catch (CloudbreakImageNotFoundException e) {
            throw new CloudbreakOrchestratorFailedException("Cannot determine image of stack, thus osType and repository informations cannot be provided.");
        }
    }

    private void decoratePillarWithAmbariDatabase(Cluster cluster, Map<String, SaltPillarProperties> servicePillar)
            throws CloudbreakOrchestratorFailedException {
        RDSConfig ambariRdsConfig = rdsConfigService.findByClusterIdAndType(cluster.getId(), DatabaseType.AMBARI);
        if (ambariRdsConfig == null) {
            throw new CloudbreakOrchestratorFailedException("Ambari Database is missing for stack");
        }
        RdsView ambariRdsView = new RdsView(rdsConfigService.resolveVaultValues(ambariRdsConfig));
        servicePillar.put("ambari-database", new SaltPillarProperties("/ambari/database.sls", singletonMap("ambari", singletonMap("database", ambariRdsView))));
    }

    private Map<String, Map<String, String>> createGrainProperties(Iterable<GatewayConfig> gatewayConfigs) {
        Map<String, Map<String, String>> grainProperties = new HashMap<>();
        for (GatewayConfig gatewayConfig : gatewayConfigs) {
            Map<String, String> hostGrain = new HashMap<>();
            hostGrain.put("gateway-address", gatewayConfig.getPublicAddress());
            grainProperties.put(gatewayConfig.getPrivateAddress(), hostGrain);
        }
        return grainProperties;
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
            Optional<DatalakeResources> datalakeResource = datalakeResourcesService.getDatalakeResourcesById(datalakeResourceId);
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

    private StackView getStackView(Long datalakeId) {
        return stackViewRepository.findById(datalakeId).orElseThrow(notFound("Stack view", datalakeId));
    }

    private void saveGatewayPillar(GatewayConfig gatewayConfig, Cluster cluster, Map<String, SaltPillarProperties> servicePillar) throws IOException {
        Map<String, Object> gateway = new HashMap<>();
        gateway.put("address", gatewayConfig.getPublicAddress());
        gateway.put("username", cluster.getUserName());
        gateway.put("password", cluster.getPassword());

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
            List<Map<String, Object>> topologies = getTopologies(clusterGateway);
            gateway.put("topologies", topologies);
            if (cluster.getClusterDefinition() != null) {
                Map<String, Integer> servicePorts = ambariBlueprintPortConfigCollector.getServicePorts(cluster.getClusterDefinition());
                gateway.put("ports", servicePorts);
            }
        }

        gateway.put("kerberos", cluster.getKerberosConfig() != null);

        if (clusterDefinitionService.isAmbariBlueprint(cluster.getClusterDefinition())) {
            Map<String, List<String>> serviceLocation = componentLocator.getComponentLocation(cluster, new HashSet<>(ExposedService.getAllServiceName()));

            List<String> rangerLocations = serviceLocation.get(ExposedService.RANGER.getServiceName());
            if (rangerLocations != null && !rangerLocations.isEmpty()) {
                serviceLocation.put(ExposedService.RANGER.getServiceName(), getSingleRangerFqdn(gatewayConfig.getHostname(), rangerLocations));
            }

            gateway.put("location", serviceLocation);
            servicePillar.put("gateway", new SaltPillarProperties("/gateway/init.sls", singletonMap("gateway", gateway)));
        }
    }

    private List<String> getSingleRangerFqdn(String primaryGatewayFqdn, List<String> rangerLocations) {
        return rangerLocations.contains(primaryGatewayFqdn) ? List.of(primaryGatewayFqdn) : List.of(rangerLocations.iterator().next());
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
        if (exposedJson != null && StringUtils.isNoneEmpty(exposedJson.getValue())) {
            List<String> exposedServices = exposedJson.get(ExposedServices.class).getServices();
            topology.put("exposed", exposedServices);
        } else {
            topology.put("exposed", new ArrayList<>());
        }
        return topology;
    }

    private void saveLdapPillar(LdapConfig ldapConfig, Map<String, SaltPillarProperties> servicePillar) {
        if (ldapConfig != null) {
            LdapView ldapView = new LdapView(ldapConfig, ldapConfig.getBindDn(), ldapConfig.getBindPassword());
            servicePillar.put("ldap", new SaltPillarProperties("/gateway/ldap.sls", singletonMap("ldap", ldapView)));
        }
    }

    private void saveDockerPillar(ExecutorType executorType, Map<String, SaltPillarProperties> servicePillar) {
        Map<String, Object> dockerMap = new HashMap<>();

        dockerMap.put("enableContainerExecutor", ExecutorType.CONTAINER.equals(executorType));

        servicePillar.put("docker", new SaltPillarProperties("/docker/init.sls", singletonMap("docker", dockerMap)));
    }

    private void saveHDPPillar(Long clusterId, Map<String, SaltPillarProperties> servicePillar) {
        StackRepoDetails hdprepo = clusterComponentConfigProvider.getHDPRepo(clusterId);
        servicePillar.put("hdp", new SaltPillarProperties("/hdp/repo.sls", singletonMap("hdp", hdprepo)));
    }

    private Map<String, String> collectUpscaleCandidates(Long clusterId, String hostGroupName, Integer adjustment) {
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(clusterId, hostGroupName);
        if (hostGroup.getConstraint().getInstanceGroup() != null) {
            Long instanceGroupId = hostGroup.getConstraint().getInstanceGroup().getId();
            Map<String, String> hostNames = new HashMap<>();
            instanceMetaDataRepository.findUnusedHostsInInstanceGroup(instanceGroupId).stream()
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
                .filter(rds -> StringUtils.isNoneEmpty(rds.getConnectorJarUrl()))
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
