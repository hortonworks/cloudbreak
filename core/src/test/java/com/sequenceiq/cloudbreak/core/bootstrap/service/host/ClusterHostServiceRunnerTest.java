package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.CMLicenseParser;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
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
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.NameserverPillarDecorator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
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
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerFqdnUtil;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.flow.MountDisks;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltPillarDecorator;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.template.views.provider.RdsViewProvider;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.Tunnel;

@ExtendWith(MockitoExtension.class)
class ClusterHostServiceRunnerTest {

    private static final String TEST_CLUSTER_CRN = "crn:cdp:datahub:us-west-1:datahub:cluster:f7563fc1-e8ff-486a-9260-4e54ccabbaa0";

    private static final String ENV_CRN = "envCrn";

    private static final String STACK_NAME = "stackName";

    private static final Long STACK_ID = 123L;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ComponentLocatorService componentLocator;

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
    private TelemetrySaltPillarDecorator telemetrySaltPillarDecorator;

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

    @Spy
    private StackDto stack;

    @Mock
    private StackView stackView;

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
    private LoadBalancerFqdnUtil loadBalancerFqdnUtil;

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

    @Mock
    private GatewayService gatewayService;

    @Mock
    private SssdConfigProvider sssdConfigProvider;

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private NameserverPillarDecorator nameserverPillarDecorator;

    @Mock
    private RdsViewProvider rdsViewProvider;

    @BeforeEach
    void setUp() {
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(stack.getStack()).thenReturn(stackView);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(stack.getName()).thenReturn(STACK_NAME);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(environmentConfigProvider.getParentEnvironmentCrn(any())).thenReturn(ENV_CRN);
    }

    @Test
    void shouldUsecollectAndCheckReachableNodes() throws NodesUnreachableException {
        try {
            underTest.runClusterServices(stack, Map.of(), true);
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
                () -> underTest.runClusterServices(stack, Map.of(), true));
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
        Set<Node> nodes = Sets.newHashSet(node("fqdn3"), node("gateway1"), node("gateway2"), node("gateway3"));
        List<InstanceMetadataView> gwNodes = Lists.newArrayList(createInstanceMetadata("gateway1"), createInstanceMetadata("gateway2"),
                createInstanceMetadata("1.1.3.1"), createInstanceMetadata("1.1.3.2"));
        when(stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(gwNodes);
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(stackUtil.collectReachableAndUnreachableCandidateNodes(any(), any())).thenReturn(new NodeReachabilityResult(nodes, Set.of()));
        KerberosConfig kerberosConfig = new KerberosConfig();
        when(kerberosConfigService.get(ENV_CRN, STACK_NAME)).thenReturn(Optional.of(kerberosConfig));
        when(sssdConfigProvider.createSssdAdPillar(kerberosConfig)).thenReturn(Map.of("ad", new SaltPillarProperties("adpath", Map.of())));
        when(sssdConfigProvider.createSssdIpaPillar(eq(kerberosConfig), anyMap(), eq(ENV_CRN)))
                .thenReturn(Map.of("ipa", new SaltPillarProperties("ipapath", Map.of())));

        underTest.runTargetedClusterServices(stack, Map.of("fqdn3", "1.1.1.1"));

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
    void testDecoratePillarWithMountInfo() throws CloudbreakOrchestratorException, NodesUnreachableException, CloudbreakException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("gateway3"));
        when(stackUtil.collectAndCheckReachableNodes(any(), any())).thenReturn(nodes);
        KerberosConfig kerberosConfig = new KerberosConfig();
        when(kerberosConfigService.get(ENV_CRN, STACK_NAME)).thenReturn(Optional.of(kerberosConfig));
        when(sssdConfigProvider.createSssdAdPillar(kerberosConfig)).thenReturn(Map.of("ad", new SaltPillarProperties("adpath", Map.of())));
        when(sssdConfigProvider.createSssdIpaPillar(eq(kerberosConfig), anyMap(), eq(ENV_CRN)))
                .thenReturn(Map.of("ipa", new SaltPillarProperties("ipapath", Map.of())));
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());

        underTest.runClusterServices(stack, Map.of(), true);

        ArgumentCaptor<Set<Node>> reachableCandidates = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).runService(any(), reachableCandidates.capture(), saltConfig.capture(), any());
        verify(recipeEngine).executePreServiceDeploymentRecipes(any(), anyMap(), anySet());
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
    void testDecoratePillarWithMountInfoWithoutRecipeVerification()
            throws CloudbreakOrchestratorException, NodesUnreachableException, CloudbreakException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("gateway3"));
        when(stackUtil.collectAndCheckReachableNodes(any(), any())).thenReturn(nodes);
        KerberosConfig kerberosConfig = new KerberosConfig();
        when(kerberosConfigService.get(ENV_CRN, STACK_NAME)).thenReturn(Optional.of(kerberosConfig));
        when(sssdConfigProvider.createSssdAdPillar(kerberosConfig)).thenReturn(Map.of("ad", new SaltPillarProperties("adpath", Map.of())));
        when(sssdConfigProvider.createSssdIpaPillar(eq(kerberosConfig), anyMap(), eq(ENV_CRN)))
                .thenReturn(Map.of("ipa", new SaltPillarProperties("ipapath", Map.of())));
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());

        underTest.runClusterServices(stack, Map.of(), false);

        ArgumentCaptor<Set<Node>> reachableCandidates = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).runService(any(), reachableCandidates.capture(), saltConfig.capture(), any());
        verifyNoInteractions(recipeEngine);
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
    void testRedeployGatewayCertificate() throws CloudbreakOrchestratorException {
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("gateway3"));
        when(stackUtil.collectNodes(any())).thenReturn(nodes);
        when(stackUtil.collectReachableNodes(any())).thenReturn(nodes);
        List<GatewayConfig> gwConfigs = List.of(new GatewayConfig("addr", "endpoint", "privateAddr", 123, "instance", false));
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gwConfigs);
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());

        setupMocksForRunClusterServices();
        underTest.redeployGatewayCertificate(stack);
        verify(hostOrchestrator, times(1)).initServiceRun(eq(stack), eq(gwConfigs), eq(nodes), eq(nodes), any(), any(), eq(CloudPlatform.AWS.name()));
        verify(hostOrchestrator).runService(eq(gwConfigs), eq(nodes), any(), any());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testRedeployGatewayPillarOnly() throws CloudbreakOrchestratorFailedException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("gateway3"));
        when(stackUtil.collectNodes(any())).thenReturn(nodes);

        underTest.redeployGatewayPillarOnly(stack);
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
        underTest.redeployStates(stack);
        verify(hostOrchestrator).uploadStates(gatewayConfigsCaptor.capture(), any());
        List<GatewayConfig> gatewayConfigs = gatewayConfigsCaptor.getValue();
        assertEquals(gwConfigs, gatewayConfigs);
    }

    @Test
    void testCreateCronForUserHomeCreation() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        List<GatewayConfig> gatewayConfigs = List.of(new GatewayConfig("addr", "endpoint", "privateAddr", 123, "instance", false));
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        when(stackUtil.collectReachableAndUnreachableCandidateNodes(eq(stack), any()))
                .thenReturn(new NodeReachabilityResult(Set.of(TestUtil.node()), new HashSet<>()));
        underTest.createCronForUserHomeCreation(stack, Set.of("fqdn"));
        verify(hostOrchestrator).createCronForUserHomeCreation(eq(gatewayConfigs), eq(Set.of("fqdn")), any());
    }

    @Test
    void testAddJavaPillarToSaltConfig() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices();
        when(stackView.getJavaVersion()).thenReturn(11);
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AWS_DEFAULT_VARIANT.value());

        underTest.runClusterServices(stack, Collections.emptyMap(), false);
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).runService(any(), any(), saltConfig.capture(), any());

        assertEquals(getJavaProperties(saltConfig.getValue()).get("version"), 11);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testAddJavaPillarToRedeployGateway() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices();
        when(stackView.getJavaVersion()).thenReturn(11);

        underTest.redeployGatewayPillarOnly(stack);
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfig.capture());

        assertEquals(getJavaProperties(saltConfig.getValue()).get("version"), 11);
    }

    private Map<String, Object> getJavaProperties(SaltConfig saltConfig) {
        return (Map<String, Object>) saltConfig.getServicePillarConfig().get("java").getProperties().get("java");
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testFloatinIpLoadBalancers() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices();
        LoadBalancer lbGateway = new LoadBalancer();
        lbGateway.setIp("ip1");
        lbGateway.setType(LoadBalancerType.GATEWAY_PRIVATE);
        LoadBalancer lbPrivate = new LoadBalancer();
        lbPrivate.setIp("ip2");
        lbPrivate.setType(LoadBalancerType.PRIVATE);
        Set<LoadBalancer> loadBalancers = Set.of(lbGateway, lbPrivate);
        when(loadBalancerFqdnUtil.getLoadBalancersForStack(STACK_ID)).thenReturn(loadBalancers);
        underTest.redeployGatewayPillarOnly(stack);
        ArgumentCaptor<SaltConfig> saltConfigCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfigCaptor.capture());
        SaltConfig saltConfig = saltConfigCaptor.getValue();
        Map<String, Object> pillarMap = (Map<String, Object>) saltConfig.getServicePillarConfig().get("gateway").getProperties().get("gateway");
        assertThat(pillarMap).containsKey("loadbalancers");
        Map<String, Object> loadbalancerMap = (Map<String, Object>) pillarMap.get("loadbalancers");
        assertThat(loadbalancerMap).hasSize(2)
                .containsKey("frontends")
                .containsEntry("floatingIpEnabled", true);
        List<Map<String, Object>> frontends = (List<Map<String, Object>>) loadbalancerMap.get("frontends");
        assertThat(frontends).hasSize(2);
        assertThat(frontends).anyMatch(f -> "GATEWAY_PRIVATE".equals(f.get("type")) && "ip1".equals(f.get("ip")))
                .anyMatch(f -> "PRIVATE".equals(f.get("type")) && "ip2".equals(f.get("ip")));
    }

    private void setupMocksForRunClusterServices() {
        when(umsClient.getAccountDetails(any(), any())).thenReturn(UserManagementProto.Account.getDefaultInstance());
        when(stackView.getTunnel()).thenReturn(Tunnel.DIRECT);
        when(stackView.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        when(stackView.getResourceCrn()).thenReturn(TEST_CLUSTER_CRN);
        when(cluster.getName()).thenReturn("clustername");
        when(componentLocator.getComponentLocation(any(), any())).thenReturn(new HashMap<>());
        when(exposedServiceCollector.getImpalaService()).thenReturn(mock(ExposedService.class));
        when(environmentConfigProvider.getParentEnvironmentCrn(any())).thenReturn("crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com");
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clouderaManagerRepo.getVersion()).thenReturn("7.2.2");
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getPrivateAddress()).thenReturn("1.2.3.4");
        when(gatewayConfig.getHostname()).thenReturn("hostname");
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        when(exposedServiceCollector.getRangerService()).thenReturn(mock(ExposedService.class));
        ExposedService cmExposedService = mock(ExposedService.class);
        when(cmExposedService.getServiceName()).thenReturn("CM");
        when(exposedServiceCollector.getClouderaManagerService()).thenReturn(cmExposedService);

        Template template = new Template();
        template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES);

        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        createInstanceGroup(template, instanceGroups, "fqdn1", null, "1.1.1.1", "1.1.1.2");
        createInstanceGroup(template, instanceGroups, "fqdn2", null, "1.1.2.1", "1.1.2.2");

        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        RdsConfigWithoutCluster rdsConfigWithoutCluster = mock(RdsConfigWithoutCluster.class);
        RdsView rdsView = mock(RdsView.class);
        when(rdsConfigWithoutClusterService.findByClusterIdAndType(any(), eq(DatabaseType.CLOUDERA_MANAGER))).thenReturn(rdsConfigWithoutCluster);
        when(rdsViewProvider.getRdsView(any(RdsConfigWithoutCluster.class))).thenReturn(rdsView);
        when(loadBalancerSANProvider.getLoadBalancerSAN(anyLong(), any())).thenReturn(Optional.empty());
        ClusterPreCreationApi clusterPreCreationApi = mock(ClusterPreCreationApi.class);
        when(clusterApiConnectors.getConnector(cluster)).thenReturn(clusterPreCreationApi);
        ServiceLocationMap serviceLocationMap = new ServiceLocationMap();
        serviceLocationMap.add(new ServiceLocation("serv", "paath"));
        when(clusterPreCreationApi.getServiceLocations()).thenReturn(serviceLocationMap);
        ReflectionTestUtils.setField(underTest, "cmHeartbeatInterval", "1");
        ReflectionTestUtils.setField(underTest, "cmMissedHeartbeatInterval", "1");
    }

    private void createInstanceGroup(Template template, List<InstanceGroupDto> instanceGroups, String fqdn1, String fqdn2,
            String privateIp1, String privateIp2) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        instanceGroup.setGroupName("group");
        List<InstanceMetadataView> instanceMetaDataSet = new ArrayList<>();
        instanceMetaDataSet.add(createInstanceMetadata(fqdn1, instanceGroup, privateIp1));
        instanceMetaDataSet.add(createInstanceMetadata(fqdn2, instanceGroup, privateIp2));
        instanceGroups.add(new InstanceGroupDto(instanceGroup, instanceMetaDataSet));
    }

    private InstanceMetaData createInstanceMetadata(String fqdn, InstanceGroup instanceGroup, String privateIp) {
        InstanceMetaData imd1 = new InstanceMetaData();
        imd1.setDiscoveryFQDN(fqdn);
        imd1.setInstanceGroup(instanceGroup);
        imd1.setPrivateIp(privateIp);
        return imd1;
    }

    private InstanceMetaData createInstanceMetadata(String fqdn) {
        InstanceMetaData imd1 = new InstanceMetaData();
        imd1.setDiscoveryFQDN(fqdn);
        return imd1;
    }

    private Node node(String fqdn) {
        return new Node(null, null, null, null, fqdn, null);
    }
}
