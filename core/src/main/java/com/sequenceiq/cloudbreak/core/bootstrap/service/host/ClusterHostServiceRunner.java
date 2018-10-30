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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
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
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.SmartSenseCredentialConfigService;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariSecurityConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class ClusterHostServiceRunner {

    @Inject
    private StackService stackService;

    @Inject
    private StackViewRepository stackViewRepository;

    @Inject
    private ClusterService clusterService;

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
    private BlueprintProcessorFactory blueprintProcessorFactory;

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
    private BlueprintPortConfigCollector blueprintPortConfigCollector;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Inject
    private VaultService vaultService;

    public void runAmbariServices(Stack stack, Cluster cluster) {
        try {
            Set<Node> nodes = stackUtil.collectNodes(stack);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            SaltConfig saltConfig = createSaltConfig(stack, cluster, primaryGatewayConfig, gatewayConfigs);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
            hostOrchestrator.initServiceRun(gatewayConfigs, nodes, saltConfig, exitCriteriaModel);
            recipeEngine.executePreAmbariStartRecipes(stack, hostGroupService.getByCluster(cluster.getId()));
            hostOrchestrator.runService(gatewayConfigs, nodes, saltConfig, exitCriteriaModel);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException | IOException | CloudbreakException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public Map<String, String> addAmbariServices(Long stackId, String hostGroupName, Integer scalingAdjustment) throws CloudbreakException {
        Map<String, String> candidates;
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = stack.getCluster();
        candidates = collectUpscaleCandidates(cluster.getId(), hostGroupName, scalingAdjustment);
        runAmbariServices(stack, cluster);
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
        saveDatalakeNameservers(stack, servicePillar);
        saveSharedRangerService(stack, servicePillar);
        if (cluster.isSecure() && kerberosDetailService.isAmbariManagedKerberosPackages(cluster.getKerberosConfig())) {
            Map<String, String> kerberosPillarConf = new HashMap<>();
            KerberosConfig kerberosConfig = cluster.getKerberosConfig();
            putIfNotNull(kerberosPillarConf, vaultService.resolveSingleValue(kerberosConfig.getMasterKey()), "masterKey");
            putIfNotNull(kerberosPillarConf, vaultService.resolveSingleValue(kerberosConfig.getAdmin()), "user");
            putIfNotNull(kerberosPillarConf, vaultService.resolveSingleValue(kerberosConfig.getPassword()), "password");
            if (StringUtils.isEmpty(vaultService.resolveSingleValue(kerberosConfig.getDescriptor()))) {
                putIfNotNull(kerberosPillarConf, kerberosConfig.getUrl(), "url");
                putIfNotNull(kerberosPillarConf, kerberosDetailService.resolveHostForKdcAdmin(kerberosConfig, kerberosConfig.getUrl()), "adminUrl");
                putIfNotNull(kerberosPillarConf, kerberosConfig.getRealm(), "realm");
            } else {
                Map<String, Object> properties = kerberosDetailService.getKerberosEnvProperties(kerberosConfig);
                putIfNotNull(kerberosPillarConf, properties.get("kdc_hosts"), "url");
                putIfNotNull(kerberosPillarConf, properties.get("admin_server_host"), "adminUrl");
                putIfNotNull(kerberosPillarConf, properties.get("realm"), "realm");
            }
            putIfNotNull(kerberosPillarConf, vaultService.resolveSingleValue(cluster.getCloudbreakAmbariUser()), "clusterUser");
            putIfNotNull(kerberosPillarConf, vaultService.resolveSingleValue(cluster.getCloudbreakAmbariPassword()), "clusterPassword");
            servicePillar.put("kerberos", new SaltPillarProperties("/kerberos/init.sls", singletonMap("kerberos", kerberosPillarConf)));
        }
        servicePillar.put("discovery", new SaltPillarProperties("/discovery/init.sls", singletonMap("platform", stack.cloudPlatform())));
        servicePillar.put("metadata", new SaltPillarProperties("/metadata/init.sls",
                singletonMap("cluster", singletonMap("name", stack.getCluster().getName()))));
        saveGatewayPillar(primaryGatewayConfig, cluster, servicePillar);
        AmbariRepo ambariRepo = clusterComponentConfigProvider.getAmbariRepo(cluster.getId());
        if (ambariRepo != null) {
            Map<String, Object> ambariRepoMap = ambariRepo.asMap();
            Json blueprint = new Json(cluster.getBlueprint().getBlueprintText());
            ambariRepoMap.put("stack_version", blueprint.getValue("Blueprints.stack_version"));
            ambariRepoMap.put("stack_type", blueprint.getValue("Blueprints.stack_name").toString().toLowerCase());
            servicePillar.put("ambari-repo", new SaltPillarProperties("/ambari/repo.sls", singletonMap("ambari", singletonMap("repo", ambariRepoMap))));
            boolean setupLdapAndSsoOnApi = ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(ambariRepo::getVersion, AMBARI_VERSION_2_7_0_0);
            servicePillar.put("setup-ldap-and-sso-on-api", new SaltPillarProperties("/ambari/config.sls",
                    singletonMap("ambari", singletonMap("setup_ldap_and_sso_on_api", setupLdapAndSsoOnApi))));
        }
        servicePillar.put("ambari-gpl-repo", new SaltPillarProperties("/ambari/gpl.sls", singletonMap("ambari", singletonMap("gpl", singletonMap("enabled",
                clusterComponentConfigProvider.getHDPRepo(cluster.getId()).isEnableGplRepo())))));

        postgresConfigService.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack, cluster);

        decoratePillarWithAmbariDatabase(cluster, servicePillar);

        if (cluster.getLdapConfig() != null) {
            LdapConfig ldapConfig = cluster.getLdapConfig().copyWithoutWorkspace();
            saveLdapPillar(ldapConfig, servicePillar);
        }
        saveSssdAdPillar(cluster, servicePillar);
        saveDockerPillar(cluster.getExecutorType(), servicePillar);
        saveHDPPillar(cluster.getId(), servicePillar);
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("username", ambariSecurityConfigProvider.getAmbariUserName(stack.getCluster()));
        credentials.put("password", ambariSecurityConfigProvider.getAmbariPassword(stack.getCluster()));
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

    private void decoratePillarWithAmbariDatabase(Cluster cluster, Map<String, SaltPillarProperties> servicePillar)
            throws CloudbreakOrchestratorFailedException {
        RDSConfig ambariRdsConfig = rdsConfigService.findByClusterIdAndType(cluster.getId(), RdsType.AMBARI);
        if (ambariRdsConfig == null) {
            throw new CloudbreakOrchestratorFailedException("Ambari RDSConfig is missing for stack");
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

    /**
     * In order to be able to connect an ephemeral cluster to a datalake, the ephemeral cluster needs to know some of the datalake nameservers to resolve
     * the custom hostnames.
     */
    private void saveDatalakeNameservers(Stack stack, Map<String, SaltPillarProperties> servicePillar) {
        Long datalakeId = stack.getDatalakeId();
        if (datalakeId != null) {
            Stack dataLakeStack = stackService.getByIdWithListsInTransaction(datalakeId);
            String datalakeDomain = dataLakeStack.getGatewayInstanceMetadata().get(0).getDomain();
            List<String> ipList = dataLakeStack.getGatewayInstanceMetadata().stream().map(InstanceMetaData::getPrivateIp).collect(Collectors.toList());
            servicePillar.put("forwarder-zones", new SaltPillarProperties("/unbound/forwarders.sls",
                    singletonMap("forwarder-zones", singletonMap(datalakeDomain, singletonMap("nameservers", ipList)))));
        }
    }

    private void saveSharedRangerService(Stack stack, Map<String, SaltPillarProperties> servicePillar) {
        Long datalakeId = stack.getDatalakeId();
        if (datalakeId != null) {
            StackView dataLakeStack = getStackView(datalakeId);
            Cluster dataLakeCluster = clusterService.findOneWithLists(dataLakeStack.getClusterView().getId());
            BlueprintTextProcessor blueprintTextProcessor = blueprintProcessorFactory.get(dataLakeCluster.getBlueprint().getBlueprintText());

            Set<String> groupNames = blueprintTextProcessor.getHostGroupsWithComponent("RANGER_ADMIN");
            List<HostGroup> groups = dataLakeCluster.getHostGroups().stream().filter(hg -> groupNames.contains(hg.getName())).collect(Collectors.toList());
            Set<String> hostNames = new HashSet<>();
            groups.forEach(hg -> hostNames.addAll(hostGroupService.getByClusterIdAndName(dataLakeCluster.getId(), hg.getName())
                    .getHostMetadata().stream().map(HostMetadata::getHostName).collect(Collectors.toList())));

            Map<String, String> rangerAdminConfigs = blueprintTextProcessor.getConfigurationEntries().getOrDefault("ranger-admin-site", new HashMap<>());
            String rangerPort = rangerAdminConfigs.getOrDefault("ranger.service.http.port", "6080");

            Map<String, Object> rangerMap = new HashMap<>();
            rangerMap.put("servers", hostNames);
            rangerMap.put("port", rangerPort);
            servicePillar.put("datalake-services", new SaltPillarProperties("/datalake/init.sls",
                    singletonMap("datalake-services", singletonMap("ranger", rangerMap))));
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
            if (cluster.getBlueprint() != null) {
                Map<String, Integer> servicePorts = blueprintPortConfigCollector.getServicePorts(cluster.getBlueprint());
                gateway.put("ports", servicePorts);
            }
        }

        gateway.put("kerberos", cluster.isSecure());
        Map<String, List<String>> serviceLocation = componentLocator.getComponentLocation(cluster, new HashSet<>(ExposedService.getAllServiceName()));

        List<String> rangerLocations = serviceLocation.get(ExposedService.RANGER.getServiceName());
        if (rangerLocations != null && !rangerLocations.isEmpty()) {
            serviceLocation.put(ExposedService.RANGER.getServiceName(), getSingleRangerFqdn(gatewayConfig.getHostname(), rangerLocations));
        }

        gateway.put("location", serviceLocation);
        servicePillar.put("gateway", new SaltPillarProperties("/gateway/init.sls", singletonMap("gateway", gateway)));
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
            ldapConfig.setBindDn(vaultService.resolveSingleValue(ldapConfig.getBindDn()));
            ldapConfig.setBindPassword(vaultService.resolveSingleValue(ldapConfig.getBindPassword()));
            servicePillar.put("ldap", new SaltPillarProperties("/gateway/ldap.sls", singletonMap("ldap", ldapConfig)));
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
