package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.CMLicenseParser;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.cluster.model.ServiceLocation;
import com.sequenceiq.cloudbreak.cluster.model.ServiceLocationMap;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.CsdParcelDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.HostAttributeDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.TelemetryDecorator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.san.LoadBalancerSANProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MountDisks;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.Tunnel;

@ExtendWith(MockitoExtension.class)
class ClusterHostServiceRunnerTest {

    private static final String TEST_CLUSTER_CRN = "crn:cdp:datahub:us-west-1:datahub:cluster:f7563fc1-e8ff-486a-9260-4e54ccabbaa0";

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ComponentLocatorService componentLocator;

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private ProxyConfigProvider proxyConfigProvider;

    @Mock
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private RecipeEngine recipeEngine;

    @Mock
    private DefaultClouderaManagerRepoService clouderaManagerRepoService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private TelemetryDecorator telemetryDecorator;

    @Mock
    private MountDisks mountDisks;

    @Mock
    private VirtualGroupService virtualGroupService;

    @Mock
    private GrainPropertiesService grainPropertiesService;

    @Mock
    private ExposedServiceCollector exposedServiceCollector;

    @Mock
    private EnvironmentConfigProvider environmentConfigProvider;

    @Mock
    private CMLicenseParser cmLicenseParser;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private Stack stack;

    @Mock
    private Cluster cluster;

    @Mock
    private Blueprint blueprint;

    @Mock
    private DatalakeService datalakeService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private HostAttributeDecorator hostAttributeDecorator;

    @Mock
    private LoadBalancerConfigService loadBalancerConfigService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private IdBrokerService idBrokerService;

    @Mock
    private CsdParcelDecorator csdParcelDecorator;

    @Mock
    private LoadBalancerSANProvider loadBalancerSANProvider;

    @InjectMocks
    private ClusterHostServiceRunner underTest;

    @Captor
    private ArgumentCaptor<Set<Node>> allNodesCaptor;

    @Captor
    private ArgumentCaptor<List<GatewayConfig>> gatewayConfigsCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(stack.getEnvironmentCrn()).thenReturn("envCrn");
        lenient().when(environmentConfigProvider.getParentEnvironmentCrn(any())).thenReturn("envCrn");
    }

    @Test
    void shouldUsecollectAndCheckReachableNodes() throws NodesUnreachableException {
        try {
            underTest.runClusterServices(stack, cluster, Map.of());
            fail();
        } catch (NullPointerException e) {
            verify(stackUtil).collectAndCheckReachableNodes(eq(stack), any());
        }
    }

    @Test
    void collectAndCheckReachableNodesThrowsException() throws NodesUnreachableException {
        Set<String> unreachableNodes = new HashSet<>();
        unreachableNodes.add("node1.example.com");
        when(stackUtil.collectAndCheckReachableNodes(eq(stack), any())).thenThrow(new NodesUnreachableException("error", unreachableNodes));

        CloudbreakServiceException cloudbreakServiceException = Assertions.assertThrows(CloudbreakServiceException.class,
                () -> underTest.runClusterServices(stack, cluster, Map.of()));
        assertEquals("Can not run cluster services on new nodes because the configuration management service is not responding on these nodes: " +
                "[node1.example.com]", cloudbreakServiceException.getMessage());
    }

    @Test
    void testDecoratePillarWithClouderaManagerRepo() throws IOException, CloudbreakOrchestratorFailedException {
        String license = FileReaderUtils.readFileFromClasspath("cm-license.txt");
        when(cmLicenseParser.parseLicense(license)).thenCallRealMethod();
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.2.0");
        clouderaManagerRepo.setBaseUrl("https://archive.cloudera.com/cm/7.2.0/");

        Map<String, SaltPillarProperties> pillar = new HashMap<>();
        underTest.decoratePillarWithClouderaManagerRepo(clouderaManagerRepo, pillar, Optional.of(license));

        SaltPillarProperties resultPillar = pillar.get("cloudera-manager-repo");
        Map<String, Object> properties = resultPillar.getProperties();
        Map<String, Object> values = (Map<String, Object>) properties.get("cloudera-manager");
        assertEquals("7.2.0", ((ClouderaManagerRepo) values.get("repo")).getVersion());
        assertEquals("https://archive.cloudera.com/cm/7.2.0/", ((ClouderaManagerRepo) values.get("repo")).getBaseUrl());
        assertEquals("d2834876-30fe-4000-ba85-6e99e537897e", values.get("paywall_username"));
        assertEquals("db5d119ac130", values.get("paywall_password"));
    }

    @Test
    void testDecoratePillarWithClouderaManagerRepoWithNoJsonLicense() throws IOException, CloudbreakOrchestratorFailedException {
        String license = FileReaderUtils.readFileFromClasspath("cm-license-nojson.txt");
        when(cmLicenseParser.parseLicense(license)).thenCallRealMethod();
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.2.0");
        clouderaManagerRepo.setBaseUrl("https://archive.cloudera.com/cm/7.2.0/");

        Map<String, SaltPillarProperties> pillar = new HashMap<>();
        underTest.decoratePillarWithClouderaManagerRepo(clouderaManagerRepo, pillar, Optional.of(license));

        SaltPillarProperties resultPillar = pillar.get("cloudera-manager-repo");
        Map<String, Object> properties = resultPillar.getProperties();
        Map<String, Object> values = (Map<String, Object>) properties.get("cloudera-manager");
        assertEquals("7.2.0", ((ClouderaManagerRepo) values.get("repo")).getVersion());
        assertEquals("https://archive.cloudera.com/cm/7.2.0/", ((ClouderaManagerRepo) values.get("repo")).getBaseUrl());
        assertNull(values.get("paywall_username"));
        assertNull(values.get("paywall_password"));
    }

    @Test
    void testDecoratePillarWithClouderaManagerRepoWithEmptyLicense() throws IOException, CloudbreakOrchestratorFailedException {
        String license = FileReaderUtils.readFileFromClasspath("cm-license-empty.txt");
        when(cmLicenseParser.parseLicense(license)).thenCallRealMethod();
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.2.0");
        clouderaManagerRepo.setBaseUrl("https://archive.cloudera.com/cm/7.2.0/");

        Map<String, SaltPillarProperties> pillar = new HashMap<>();
        underTest.decoratePillarWithClouderaManagerRepo(clouderaManagerRepo, pillar, Optional.of(license));

        SaltPillarProperties resultPillar = pillar.get("cloudera-manager-repo");
        Map<String, Object> properties = resultPillar.getProperties();
        Map<String, Object> values = (Map<String, Object>) properties.get("cloudera-manager");
        assertEquals("7.2.0", ((ClouderaManagerRepo) values.get("repo")).getVersion());
        assertEquals("https://archive.cloudera.com/cm/7.2.0/", ((ClouderaManagerRepo) values.get("repo")).getBaseUrl());
        assertNull(values.get("paywall_username"));
        assertNull(values.get("paywall_password"));
    }

    @Test
    void testDecoratePillarWithMountInfoAndTargetedSaltCall() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn3"), node("gateway1"), node("gateway3"));
        when(stackUtil.collectReachableAndUnreachableCandidateNodes(any(), any())).thenReturn(new NodeReachabilityResult(nodes, Set.of()));
        underTest.runTargetedClusterServices(stack, cluster, Map.of("fqdn3", "1.1.1.1"));

        ArgumentCaptor<Set<Node>> reachableCandidates = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).runService(any(), reachableCandidates.capture(), saltConfig.capture(), any());
        Set<Node> reachableNodes = reachableCandidates.getValue();
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway1", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway2", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway3", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn3", node.getHostname())));
        assertFalse(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn1", node.getHostname())));
        assertFalse(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn2", node.getHostname())));
        assertTrue(saltConfig.getValue().getServicePillarConfig().keySet().stream().allMatch(Objects::nonNull));
    }

    @Test
    void testDecoratePillarWithMountInfo() throws CloudbreakOrchestratorException, NodesUnreachableException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("gateway3"));
        when(stackUtil.collectAndCheckReachableNodes(any(), any())).thenReturn(nodes);
        underTest.runClusterServices(stack, cluster, Map.of());

        ArgumentCaptor<Set<Node>> reachableCandidates = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).runService(any(), reachableCandidates.capture(), saltConfig.capture(), any());
        Set<Node> reachableNodes = reachableCandidates.getValue();
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway1", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway3", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn1", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn2", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn3", node.getHostname())));
        assertFalse(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway2", node.getHostname())));
        assertTrue(saltConfig.getValue().getServicePillarConfig().keySet().stream().allMatch(Objects::nonNull));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testRedeployGatewayPillarOnly() throws CloudbreakOrchestratorFailedException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("gateway3"));
        when(stackUtil.collectNodes(any())).thenReturn(nodes);

        underTest.redeployGatewayPillarOnly(stack, cluster);
        ArgumentCaptor<SaltConfig> saltConfigCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfigCaptor.capture());
        Set<Node> allNodes = allNodesCaptor.getValue();
        assertEquals(5, allNodes.size());
        SaltConfig saltConfig = saltConfigCaptor.getValue();
        assertTrue(saltConfig.getServicePillarConfig().keySet().stream().allMatch(Objects::nonNull));
    }

    @Test
    void testRedeployStates() throws CloudbreakOrchestratorException {
        List<GatewayConfig> gwConfigs = List.of(new GatewayConfig("addr", "endpoint", "privateAddr", 123, "instance", false));
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gwConfigs);
        underTest.redeployStates(stack, cluster);
        verify(hostOrchestrator).uploadStates(gatewayConfigsCaptor.capture(), any());
        List<GatewayConfig> gatewayConfigs = gatewayConfigsCaptor.getValue();
        assertEquals(gwConfigs, gatewayConfigs);
    }

    private void setupMocksForRunClusterServices() {
        when(umsClient.getAccountDetails(any(), any())).thenReturn(UserManagementProto.Account.getDefaultInstance());
        when(stackService.get(any())).thenReturn(stack);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getTunnel()).thenReturn(Tunnel.DIRECT);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        when(stack.getResourceCrn()).thenReturn(TEST_CLUSTER_CRN);
        when(cluster.getName()).thenReturn("clustername");
        when(cluster.getStack()).thenReturn(stack);
        when(componentLocator.getComponentLocation(any(), any())).thenReturn(new HashMap<>());
        when(exposedServiceCollector.getImpalaService()).thenReturn(mock(ExposedService.class));
        when(environmentConfigProvider.getParentEnvironmentCrn(any())).thenReturn("crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com");
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clouderaManagerRepo.getVersion()).thenReturn("7.2.2");
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getPrivateAddress()).thenReturn("1.2.3.4");
        when(gatewayConfig.getHostname()).thenReturn("hostname");
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(any())).thenReturn(clouderaManagerRepo);
        when(exposedServiceCollector.getRangerService()).thenReturn(mock(ExposedService.class));
        ExposedService cmExposedService = mock(ExposedService.class);
        when(cmExposedService.getServiceName()).thenReturn("CM");
        when(exposedServiceCollector.getClouderaManagerService()).thenReturn(cmExposedService);

        Template template = new Template();
        template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES);

        Set<InstanceGroup> instanceGroups = new HashSet<>();
        createInstanceGroup(template, instanceGroups, "fqdn1", null, "1.1.1.1", "1.1.1.2");
        createInstanceGroup(template, instanceGroups, "fqdn2", null, "1.1.2.1", "1.1.2.2");
        InstanceGroup gwIg = createInstanceGroup(template, instanceGroups, "gateway1", "gateway2", "1.1.3.1", "1.1.3.2");

        lenient().when(stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(Lists.newArrayList(gwIg.getAllInstanceMetaData()));

        when(stack.getInstanceGroups()).thenReturn(instanceGroups);
        RdsConfigWithoutCluster rdsConfigWithoutCluster = mock(RdsConfigWithoutCluster.class);
        when(rdsConfigWithoutClusterService.findByClusterIdAndType(any(), eq(DatabaseType.CLOUDERA_MANAGER))).thenReturn(rdsConfigWithoutCluster);
        when(rdsConfigWithoutCluster.getType()).thenReturn("asdf");
        when(rdsConfigWithoutCluster.getConnectionURL()).thenReturn("jdbc:postgresql:subname://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger");
        when(rdsConfigWithoutCluster.getConnectionUserName()).thenReturn(new Secret("username"));
        when(rdsConfigWithoutCluster.getConnectionPassword()).thenReturn(new Secret("password"));
        when(loadBalancerSANProvider.getLoadBalancerSAN(stack)).thenReturn(Optional.empty());
        ClusterPreCreationApi clusterPreCreationApi = mock(ClusterPreCreationApi.class);
        when(clusterApiConnectors.getConnector(cluster)).thenReturn(clusterPreCreationApi);
        ServiceLocationMap serviceLocationMap = new ServiceLocationMap();
        serviceLocationMap.add(new ServiceLocation("serv", "paath"));
        when(clusterPreCreationApi.getServiceLocations()).thenReturn(serviceLocationMap);
        ReflectionTestUtils.setField(underTest, "cmHeartbeatInterval", "1");
        ReflectionTestUtils.setField(underTest, "cmMissedHeartbeatInterval", "1");
    }

    private InstanceGroup createInstanceGroup(Template template, Set<InstanceGroup> instanceGroups, String fqdn1, String fqdn2,
            String privateIp1, String privateIp2) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        instanceGroup.setGroupName("group");
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        instanceMetaDataSet.add(createInstanceMetadata(fqdn1, instanceGroup, privateIp1));
        instanceMetaDataSet.add(createInstanceMetadata(fqdn2, instanceGroup, privateIp2));
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
        instanceGroups.add(instanceGroup);
        return instanceGroup;
    }

    private InstanceMetaData createInstanceMetadata(String fqdn, InstanceGroup instanceGroup, String privateIp) {
        InstanceMetaData imd1 = new InstanceMetaData();
        imd1.setDiscoveryFQDN(fqdn);
        imd1.setInstanceGroup(instanceGroup);
        imd1.setPrivateIp(privateIp);
        return imd1;
    }

    private Node node(String fqdn) {
        return new Node(null, null, null, null, fqdn, null);
    }
}
