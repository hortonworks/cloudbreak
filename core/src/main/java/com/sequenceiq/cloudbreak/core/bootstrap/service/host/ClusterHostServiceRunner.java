package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.TelemetryDecorator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
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
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MountDisks;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Component
public class ClusterHostServiceRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterHostServiceRunner.class);

    private static final int CM_HTTP_PORT = 7180;

    private static final int CM_HTTPS_PORT = 7183;

    @Value("${cb.cm.heartbeat.interval}")
    private String cmHeartbeatInterval;

    @Inject
    private StackService stackService;

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

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
    private DefaultClouderaManagerRepoService clouderaManagerRepoService;

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
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private TelemetryDecorator telemetryDecorator;

    @Inject
    private MountDisks mountDisks;

    @Inject
    private VirtualGroupService virtualGroupService;

    public void runClusterServices(@Nonnull Stack stack, @Nonnull Cluster cluster, List<String> candidateAddresses) {
        try {
            Set<Node> nodes = stackUtil.collectNodes(stack);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            SaltConfig saltConfig = createSaltConfig(stack, cluster, primaryGatewayConfig, gatewayConfigs);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
            boolean clouderaManager = blueprintService.isClouderaManagerTemplate(cluster.getBlueprint());
            hostOrchestrator.initServiceRun(gatewayConfigs, nodes, saltConfig, exitCriteriaModel, clouderaManager);
            if (CollectionUtils.isEmpty(candidateAddresses)) {
                mountDisks.mountAllDisks(stack.getId());
            } else {
                mountDisks.mountDisksOnNewNodes(stack.getId(), new HashSet<>(candidateAddresses));
            }
            recipeEngine.executePreClusterManagerRecipes(stack, hostGroupService.getRecipesByCluster(cluster.getId()));
            hostOrchestrator.runService(gatewayConfigs, nodes, saltConfig, exitCriteriaModel);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException | IOException | CloudbreakException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public Map<String, String> addClusterServices(Long stackId, String hostGroupName, Integer scalingAdjustment) {
        Map<String, String> candidates;
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = stack.getCluster();
        candidates = collectUpscaleCandidates(cluster.getId(), hostGroupName, scalingAdjustment);
        runClusterServices(stack, cluster, new ArrayList<>(candidates.values()));
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
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        saveCustomNameservers(stack, kerberosConfig, servicePillar);
        addKerberosConfig(servicePillar, kerberosConfig);
        servicePillar.put("discovery", new SaltPillarProperties("/discovery/init.sls", singletonMap("platform", stack.cloudPlatform())));
        servicePillar.put("metadata", new SaltPillarProperties("/metadata/init.sls",
                singletonMap("cluster", singletonMap("name", stack.getCluster().getName()))));
        ClusterPreCreationApi connector = clusterApiConnectors.getConnector(cluster);
        Map<String, List<String>> serviceLocations = getServiceLocations(cluster);
        Optional<LdapView> ldapView = ldapConfigService.get(stack.getEnvironmentCrn(), stack.getName());
        saveGatewayPillar(primaryGatewayConfig, cluster, servicePillar, ldapView, connector, kerberosConfig, serviceLocations);

        postgresConfigService.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack, cluster);

        if (blueprintService.isClouderaManagerTemplate(cluster.getBlueprint())) {
            addClouderaManagerConfig(stack, cluster, servicePillar);
        } else {
            addAmbariConfig(cluster, servicePillar, connector);
        }

        ldapView.ifPresent(ldap -> saveLdapPillar(ldap, servicePillar));

        saveSssdAdPillar(servicePillar, kerberosConfig);
        saveSssdIpaPillar(servicePillar, kerberosConfig, serviceLocations);
        saveDockerPillar(cluster.getExecutorType(), servicePillar);
        saveHDPPillar(cluster.getId(), servicePillar);
        ldapView.ifPresent(ldap -> saveLdapsAdPillar(ldap, servicePillar, connector));
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("username", connector.getCloudbreakClusterUserName(cluster));
        credentials.put("password", connector.getCloudbreakClusterPassword(cluster));
        credentials.put("securityMasterKey", connector.getMasterKey(cluster));
        servicePillar.put("ambari-credentials", new SaltPillarProperties("/ambari/credentials.sls", singletonMap("ambari", credentials)));

        proxyConfigProvider.decoratePillarWithProxyDataIfNeeded(servicePillar, cluster);

        decoratePillarWithJdbcConnectors(cluster, servicePillar);

        return new SaltConfig(servicePillar, createGrainProperties(gatewayConfigs, cluster));
    }

    private void addKerberosConfig(Map<String, SaltPillarProperties> servicePillar, KerberosConfig kerberosConfig) throws IOException {
        if (isKerberosNeeded(kerberosConfig)) {
            Map<String, String> kerberosPillarConf = new HashMap<>();
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
    }

    private boolean isKerberosNeeded(KerberosConfig kerberosConfig) throws IOException {
        return kerberosConfig != null
                && kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)
                && !kerberosDetailService.isClusterManagerManagedKrb5Config(kerberosConfig);
    }

    private void addClouderaManagerConfig(Stack stack, Cluster cluster, Map<String, SaltPillarProperties> servicePillar)
            throws CloudbreakOrchestratorFailedException {
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stack.getId());
        telemetryDecorator.decoratePillar(servicePillar, stack, telemetry);
        decorateWithClouderaManagerEntrerpriseDetails(telemetry, servicePillar);
        decoratePillarWithClouderaManagerLicense(stack.getId(), servicePillar);
        decoratePillarWithClouderaManagerRepo(stack.getId(), cluster.getId(), servicePillar);
        decoratePillarWithClouderaManagerDatabase(cluster, servicePillar);
        decoratePillarWithClouderaManagerCommunicationSettings(cluster, servicePillar);
        decoratePillarWithClouderaManagerAutoTls(cluster, servicePillar);
        decoratePillarWithClouderaManagerCsds(cluster, servicePillar);
        decoratePillarWithClouderaManagerSettings(servicePillar);
    }

    private void addAmbariConfig(Cluster cluster, Map<String, SaltPillarProperties> servicePillar, ClusterPreCreationApi connector)
            throws CloudbreakOrchestratorFailedException {
        AmbariRepo ambariRepo = clusterComponentConfigProvider.getAmbariRepo(cluster.getId());
        if (ambariRepo != null) {
            Map<String, Object> ambariRepoMap = ambariRepo.asMap();
            String blueprintText = cluster.getBlueprint().getBlueprintText();
            Json blueprint = new Json(blueprintText);
            ambariRepoMap.put("stack_version", blueprint.getValue("Blueprints.stack_version"));
            ambariRepoMap.put("stack_type", blueprint.getValue("Blueprints.stack_name").toString().toLowerCase());
            servicePillar.put("ambari-repo", new SaltPillarProperties("/ambari/repo.sls", singletonMap("ambari", singletonMap("repo", ambariRepoMap))));
            boolean setupLdapAndSsoOnApi = connector.isLdapAndSSOReady(ambariRepo);
            servicePillar.put("setup-ldap-and-sso-on-api", new SaltPillarProperties("/ambari/config.sls",
                    singletonMap("ambari", singletonMap("setup_ldap_and_sso_on_api", setupLdapAndSsoOnApi))));
        }
        servicePillar.put("ambari-gpl-repo", new SaltPillarProperties("/ambari/gpl.sls", singletonMap("ambari", singletonMap("gpl", singletonMap("enabled",
                clusterComponentConfigProvider.getHDPRepo(cluster.getId()).isEnableGplRepo())))));
        decoratePillarWithAmbariDatabase(cluster, servicePillar);
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

    private void saveLdapsAdPillar(LdapView ldapView, Map<String, SaltPillarProperties> servicePillar, ClusterPreCreationApi connector) {
        if (ldapView.getCertificate() != null) {
            Map<String, Object> ldapsProperties = new HashMap<>();
            ldapsProperties.put("certPath", connector.getCertPath());
            ldapsProperties.put("keystorePassword", connector.getKeystorePassword());
            ldapsProperties.put("keystorePath", connector.getKeystorePath());
            servicePillar.put("ldaps-ad", new SaltPillarProperties("/ambari/ldaps.sls", singletonMap("ambari", singletonMap("ldaps", ldapsProperties))));
        }
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
            // enumeration has performance impacts so it's only enabled if Ranger is installed on the cluster
            // otherwise the usersync does not work with nss
            sssdConnfig.put("enumerate", !CollectionUtils.isEmpty(serviceLocations.get("RANGER_ADMIN")));
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
            autoTls.put("keystore_password", cluster.getCloudbreakAmbariPassword());
            autoTls.put("truststore_password", cluster.getCloudbreakAmbariPassword());
            servicePillar.put("cloudera-manager-autotls", new SaltPillarProperties("/cloudera-manager/autotls.sls",
                    singletonMap("cloudera-manager", singletonMap("autotls", autoTls))));
        }
    }

    private void decoratePillarWithClouderaManagerLicense(Long stackId, Map<String, SaltPillarProperties> servicePillar) {
        String userCrn = stackService.get(stackId).getCreator().getUserCrn();

        Account account = umsClient.getAccountDetails(userCrn, Crn.safeFromString(userCrn).getAccountId(), Optional.empty());

        if (StringUtils.isNotEmpty(account.getClouderaManagerLicenseKey())) {
            LOGGER.debug("Got license key from UMS: {}", account.getClouderaManagerLicenseKey());
            servicePillar.put("cloudera-manager-license",
                    new SaltPillarProperties("/cloudera-manager/license.sls",
                            singletonMap("cloudera-manager",
                                    singletonMap("license", account.getClouderaManagerLicenseKey()))));
        }
    }

    private void decoratePillarWithClouderaManagerRepo(Long stackId, Long clusterId, Map<String, SaltPillarProperties> servicePillar)
            throws CloudbreakOrchestratorFailedException {
        try {
            String osType = componentConfigProviderService.getImage(stackId).getOsType();
            ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(clusterId);

            servicePillar.put("cloudera-manager-repo", new SaltPillarProperties("/cloudera-manager/repo.sls",
                    singletonMap("cloudera-manager", singletonMap("repo", Objects.nonNull(clouderaManagerRepo)
                            ? clouderaManagerRepo : clouderaManagerRepoService.getDefault(osType)))));
        } catch (CloudbreakImageNotFoundException e) {
            throw new CloudbreakOrchestratorFailedException("Cannot determine image of stack, thus osType and repository information cannot be provided.");
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

    private void decoratePillarWithClouderaManagerCsds(Cluster cluster, Map<String, SaltPillarProperties> servicePillar) {
        List<String> csdUrls = getCsdUrlList(cluster);
        servicePillar.put("csd-downloader", new SaltPillarProperties("/cloudera-manager/csd.sls",
                singletonMap("cloudera-manager",
                        singletonMap("csd-urls", csdUrls))));
    }

    private void decoratePillarWithClouderaManagerSettings(Map<String, SaltPillarProperties> servicePillar) {
        servicePillar.put("cloudera-manager-settings", new SaltPillarProperties("/cloudera-manager/settings.sls",
                singletonMap("cloudera-manager", singletonMap("settings", singletonMap("heartbeat_interval", cmHeartbeatInterval)))));
    }

    private Map<String, Map<String, String>> createGrainProperties(Iterable<GatewayConfig> gatewayConfigs, Cluster cluster) {
        Map<String, Map<String, String>> grainProperties = new HashMap<>();
        for (GatewayConfig gatewayConfig : gatewayConfigs) {
            Map<String, String> hostGrain = new HashMap<>();
            hostGrain.put("gateway-address", gatewayConfig.getPublicAddress());
            grainProperties.put(gatewayConfig.getHostname(), hostGrain);
        }
        addNameNodeRoleForHosts(grainProperties, cluster);
        addKnoxRoleForHosts(grainProperties, cluster);
        return grainProperties;
    }

    private void addNameNodeRoleForHosts(Map<String, Map<String, String>> grainProperties, Cluster cluster) {
        Map<String, List<String>> nameNodeServiceLocations = getComponentLocationByHostname(cluster, ExposedService.NAMENODE.getServiceName());
        nameNodeServiceLocations.getOrDefault(ExposedService.NAMENODE.getServiceName(), List.of())
                .forEach(nmn -> grainProperties.computeIfAbsent(nmn, s -> new HashMap<>()).put("roles", "namenode"));
    }

    private void addKnoxRoleForHosts(Map<String, Map<String, String>> grainProperties, Cluster cluster) {
        Map<String, List<String>> knoxServiceLocations = getComponentLocationByHostname(cluster, "KNOX_GATEWAY");
        knoxServiceLocations.getOrDefault("KNOX_GATEWAY", List.of())
                .forEach(nmn -> grainProperties.computeIfAbsent(nmn, s -> new HashMap<>()).put("roles", "knox"));
    }

    private Map<String, List<String>> getComponentLocationByHostname(Cluster cluster, String componentName) {
        return componentLocator.getComponentLocationByHostname(cluster, List.of(componentName));
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

    private void saveGatewayPillar(GatewayConfig gatewayConfig, Cluster cluster, Map<String, SaltPillarProperties> servicePillar, Optional<LdapView> ldapView,
            ClusterPreCreationApi connector, KerberosConfig kerberosConfig, Map<String, List<String>> serviceLocations) throws IOException {
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
            String adminGroup = ldapView.isPresent() ? ldapView.get().getAdminGroup() : "";
            VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(cluster.getEnvironmentCrn(), adminGroup);
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
                String accountId = threadBasedUserCrnProvider.getAccountId();
                String actorCrn = threadBasedUserCrnProvider.getUserCrn();
                try {
                    String metadataXml = umsClient.getIdentityProviderMetadataXml(accountId, actorCrn);
                    gateway.put("saml", metadataXml);
                } catch (Exception e) {
                    LOGGER.debug("Could not get SAML metadata file to set up IdP in KNOXSSO.", e);
                    throw new NotFoundException("Could not get SAML metadata file to set up IdP in KNOXSSO: " + e.getMessage());
                }
            }
        }
        addGatewayUserFacingCertAndFqdn(gatewayConfig, cluster, gateway);
        gateway.put("kerberos", kerberosConfig != null);

        List<String> rangerLocations = serviceLocations.get(ExposedService.RANGER.getServiceName());
        if (!CollectionUtils.isEmpty(rangerLocations)) {
            serviceLocations.put(ExposedService.RANGER.getServiceName(), getSingleRangerFqdn(gatewayConfig.getHostname(), rangerLocations));
        }
        serviceLocations.put(ExposedService.CLOUDERA_MANAGER.getServiceName(), asList(gatewayConfig.getHostname()));
        gateway.put("location", serviceLocations);
        servicePillar.put("gateway", new SaltPillarProperties("/gateway/init.sls", singletonMap("gateway", gateway)));
    }

    private void addGatewayUserFacingCertAndFqdn(GatewayConfig gatewayConfig, Cluster cluster, Map<String, Object> gateway) {
        boolean userFacingCertHasBeenGenerated = StringUtils.isNotEmpty(gatewayConfig.getUserFacingCert())
                && StringUtils.isNotEmpty(gatewayConfig.getUserFacingKey());
        if (userFacingCertHasBeenGenerated) {
            gateway.put("userfacingcert_configured", Boolean.TRUE);
            gateway.put("userfacingkey", cluster.getStack().getSecurityConfig().getUserFacingKey());
            gateway.put("userfacingcert", cluster.getStack().getSecurityConfig().getUserFacingCert());
        }
        String fqdn = cluster.getFqdn();
        if (StringUtils.isNotEmpty(fqdn)) {
            gateway.put("userfacingfqdn", fqdn);
            String[] fqdnParts = fqdn.split("\\.", 2);
            if (fqdnParts.length == 2) {
                gateway.put("userfacingdomain", Pattern.quote(fqdnParts[1]));
            }
        }
    }

    private Map<String, List<String>> getServiceLocations(Cluster cluster) {
        List<String> serviceNames = ExposedService.getAllServiceName();
        Map<String, List<String>> componentLocation = componentLocator.getComponentLocation(cluster, serviceNames);
        if (componentLocation.containsKey(ExposedService.IMPALA.getServiceName())) {
            // IMPALA_DEBUG_UI role is not a valid role, but we need to distinguish the 2 roles in order to generate the Knox topology file
            componentLocation.put(ExposedService.IMPALA_DEBUG_UI.getServiceName(), List.copyOf(componentLocation.get(ExposedService.IMPALA.getServiceName())));
            Map<String, List<String>> impalaLocations = componentLocator.getImpalaCoordinatorLocations(cluster);
            List<String> locations = impalaLocations.values().stream().flatMap(List::stream).collect(Collectors.toList());
            componentLocation.replace(ExposedService.IMPALA.getServiceName(), locations);
        }
        return componentLocation;
    }

    private List<String> getSingleRangerFqdn(String primaryGatewayFqdn, List<String> rangerLocations) {
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
        if (exposedJson != null && StringUtils.isNotEmpty(exposedJson.getValue())) {
            List<String> exposedServices = exposedJson.get(ExposedServices.class).getFullServiceList();
            topology.put("exposed", exposedServices);
        } else {
            topology.put("exposed", new ArrayList<>());
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

    private void saveHDPPillar(Long clusterId, Map<String, SaltPillarProperties> servicePillar) {
        StackRepoDetails hdprepo = clusterComponentConfigProvider.getHDPRepo(clusterId);
        servicePillar.put("hdp", new SaltPillarProperties("/hdp/repo.sls", singletonMap("hdp", hdprepo)));
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
                .filter(rds -> StringUtils.isNotEmpty(rds.getConnectorJarUrl()))
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

    private List<String> getCsdUrlList(Cluster cluster) {
        List<ClouderaManagerProduct> product = clusterComponentConfigProvider.getClouderaManagerProductDetails(cluster.getId());
        return product
                .stream()
                .map(ClouderaManagerProduct::getCsd)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
