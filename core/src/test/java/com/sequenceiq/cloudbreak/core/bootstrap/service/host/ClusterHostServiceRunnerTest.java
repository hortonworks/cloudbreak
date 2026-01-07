package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
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
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazDatahubConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazDatalakeConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
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
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.kerberos.KerberosPillarConfigGenerator;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GrainProperties;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.san.LoadBalancerSANProvider;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
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
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.flow.MountDisks;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltPillarDecorator;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.template.views.provider.RdsViewProvider;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class ClusterHostServiceRunnerTest {

    private static final String ACCOUNT_ID = "default";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:datahub:us-west-1:%s:cluster:f7563fc1-e8ff-486a-9260-4e54ccabbaa0", ACCOUNT_ID);

    private static final Map<String, SaltPillarProperties> PAYWALL_PROPERTIES = Map.of("paywall", new SaltPillarProperties("paywall_path", Map.of()));

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:default:environment:f7563fc1-e8ff-486a-9260-4e54ccabbaa0";

    private static final String STACK_NAME = "stackName";

    private static final Long STACK_ID = 123L;

    private static final Long CLUSTER_ID = 123L;

    private static final String KNOX_GATEWAY_SECURITY_DIR = "knoxGatewaySecurityDir";

    private static final String KNOX_IDBROKER_SECURITY_DIR = "knoxIdBrokerSecurityDir";

    private static final String DEFAULT_KERBEROS_CCACHE_SECRET_STORAGE = "defaultKerberosCcacheSecretStorage";

    private static final String KERBEROS_SECRET_LOCATION = "kerberosSecretLocation";

    private static final String EXTENDED_BLUEPRINT_TEXT = "extendedBlueprintText";

    @Mock
    private EnvironmentService environmentService;

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
    private ComponentLocatorService componentLocatorService;

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

    @Mock
    private JavaPillarDecorator javaPillarDecorator;

    @Mock
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @Mock
    private RangerRazDatalakeConfigProvider rangerRazDatalakeConfigProvider;

    @Mock
    private RangerRazDatahubConfigProvider rangerRazDatahubConfigProvider;

    @Mock
    private DetailedEnvironmentResponse environmentResponse;

    @Mock
    private PaywallConfigService paywallConfigService;

    @Mock
    private BackUpDecorator backUpDecorator;

    @Mock
    private EncryptionProfileProvider encryptionProfileProvider;

    @Spy
    private KerberosPillarConfigGenerator kerberosPillarConfigGenerator;

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @BeforeEach
    void setUp() {
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(stack.getStack()).thenReturn(stackView);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(stack.getName()).thenReturn(STACK_NAME);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getBlueprint()).thenReturn(mock(Blueprint.class));
        lenient().when(stackView.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(stackView.getName()).thenReturn(STACK_NAME);
        lenient().when(environmentConfigProvider.getParentEnvironmentCrn(any())).thenReturn(ENV_CRN);
        lenient().when(cluster.getExtendedBlueprintText()).thenReturn(EXTENDED_BLUEPRINT_TEXT);
        lenient().when(blueprint.getBlueprintJsonText()).thenReturn(EXTENDED_BLUEPRINT_TEXT);
        lenient().when(environmentConfigProvider.getEnvironmentByCrn(ENV_CRN)).thenReturn(environmentResponse);
        lenient().when(stack.getStack().getResourceCrn()).thenReturn(TEST_CLUSTER_CRN);
        lenient().when(stack.getStackVersion()).thenReturn("7.3.1");
        ReflectionTestUtils.setField(kerberosPillarConfigGenerator, "kerberosDetailService", kerberosDetailService);
    }

    @Test
    void shouldUsecollectAndCheckReachableNodes() throws NodesUnreachableException {
        try {
            underTest.runClusterServices(stack, Map.of(), true);
            fail();
        } catch (NullPointerException e) {
            verify(stackUtil).collectReachableAndCheckNecessaryNodes(eq(stack), any());
        }
    }

    @Test
    void collectAndCheckReachableNodesThrowsException() throws NodesUnreachableException {
        Set<String> unreachableNodes = new HashSet<>();
        unreachableNodes.add("node1.example.com");
        when(stackUtil.collectReachableAndCheckNecessaryNodes(eq(stack), any())).thenThrow(new NodesUnreachableException("error", unreachableNodes));

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
    void testCreateCloudManagerSettingsWhenCloudProviderTypeSupported() {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        clouderaManagerRepo.setVersion("7.6.2");
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        ClusterPreCreationApi clusterPreCreationApi = mock(ClusterPreCreationApi.class);
        when(stack.getStack().getResourceCrn()).thenReturn(TEST_CLUSTER_CRN);
        ReflectionTestUtils.setField(underTest, "cmHeartbeatInterval", "testString");
        ReflectionTestUtils.setField(underTest, "cmMissedHeartbeatInterval", "testString");
        Set<String> serviceLocations = new HashSet<String>();
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getCluster()).thenReturn(cluster);
        ServiceLocationMap serviceLocationMap = mock(ServiceLocationMap.class);
        when(clusterApiConnectors.getConnector(cluster)).thenReturn(clusterPreCreationApi);
        when(clusterPreCreationApi.getServiceLocations()).thenReturn(serviceLocationMap);
        when(serviceLocationMap.getAllVolumePath()).thenReturn(serviceLocations);
        when(gatewayConfig.getPrivateAddress()).thenReturn("privateAddress");
        Map<String, SaltPillarProperties> clouderaManagerSettings = underTest.createPillarWithClouderaManagerSettings(clouderaManagerRepo, stack, gatewayConfig);
        assertEquals(Boolean.TRUE, extractCloudPlatformSupported(clouderaManagerSettings));
        assertEquals(Boolean.TRUE, extractCmBundleCollection(clouderaManagerSettings));
    }

    @Test
    void testCreateCloudManagerSettingsWhenCloudProviderTypeSupportedForYARNCloud() {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        clouderaManagerRepo.setVersion("7.6.2");
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.YARN.name());
        ClusterPreCreationApi clusterPreCreationApi = mock(ClusterPreCreationApi.class);
        when(stack.getStack().getResourceCrn()).thenReturn(TEST_CLUSTER_CRN);
        ReflectionTestUtils.setField(underTest, "cmHeartbeatInterval", "testString");
        ReflectionTestUtils.setField(underTest, "cmMissedHeartbeatInterval", "testString");
        Set<String> serviceLocations = new HashSet<String>();
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getCluster()).thenReturn(cluster);
        ServiceLocationMap serviceLocationMap = mock(ServiceLocationMap.class);
        when(clusterApiConnectors.getConnector(cluster)).thenReturn(clusterPreCreationApi);
        when(clusterPreCreationApi.getServiceLocations()).thenReturn(serviceLocationMap);
        when(serviceLocationMap.getAllVolumePath()).thenReturn(serviceLocations);
        when(gatewayConfig.getPrivateAddress()).thenReturn("privateAddress");
        Map<String, SaltPillarProperties> clouderaManagerSettings = underTest.createPillarWithClouderaManagerSettings(clouderaManagerRepo, stack, gatewayConfig);
        boolean cloudProviderSetupSupported = extractCloudPlatformSupported(clouderaManagerSettings);
        assertEquals(cloudProviderSetupSupported, Boolean.FALSE);
    }

    private boolean extractCloudPlatformSupported(Map<String, SaltPillarProperties> clouderaManagerSettings) {
        SaltPillarProperties saltPillarProperties = clouderaManagerSettings.get("cloudera-manager-settings");
        Map<String, Object> clouderaManager = (Map<String, Object>) saltPillarProperties.getProperties().get("cloudera-manager");
        Map<Object, Object> settings = (Map<Object, Object>) clouderaManager.get("settings");
        return (boolean) settings.get("cloud_provider_setup_supported");
    }

    private boolean extractCmBundleCollection(Map<String, SaltPillarProperties> clouderaManagerSettings) {
        SaltPillarProperties saltPillarProperties = clouderaManagerSettings.get("cloudera-manager-settings");
        Map<String, Object> clouderaManager = (Map<String, Object>) saltPillarProperties.getProperties().get("cloudera-manager");
        Map<Object, Object> settings = (Map<Object, Object>) clouderaManager.get("settings");
        return (boolean) settings.get("disable_auto_bundle_collection");
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
    void testDecoratePillarWithMountInfoAndTargetedSaltCall() throws CloudbreakOrchestratorException, NodesUnreachableException, IOException {
        setupMocksForRunClusterServices();
        Set<Node> gatewayNodes = Set.of(node("gateway1"), node("gateway2"), node("gateway3"));
        Set<Node> nodes = Sets.union(Set.of(node("fqdn3")), gatewayNodes);
        List<InstanceMetadataView> gwNodes = Lists.newArrayList(createInstanceMetadata("gateway1"), createInstanceMetadata("gateway2"),
                createInstanceMetadata("1.1.3.1"), createInstanceMetadata("1.1.3.2"));
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(gwNodes);
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(stackUtil.collectReachableAndUnreachableCandidateNodes(any(), any())).thenReturn(new NodeReachabilityResult(nodes, Set.of()));
        when(stackUtil.collectReachableAndCheckNecessaryNodes(any(), any())).thenReturn(gatewayNodes);
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().withVerifyKdcTrust(true).build();
        when(kerberosConfigService.get(ENV_CRN, STACK_NAME)).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);

        when(sssdConfigProvider.createSssdAdPillar(kerberosConfig)).thenReturn(Map.of("ad", new SaltPillarProperties("adpath", Map.of())));

        when(sssdConfigProvider.createSssdIpaPillar(eq(kerberosConfig), anyMap(), eq(ENV_CRN), any(), eq(EXTENDED_BLUEPRINT_TEXT)))
                .thenReturn(Map.of("ipa", new SaltPillarProperties("ipapath", Map.of())));
        when(paywallConfigService.createPaywallPillarConfig(stack)).thenReturn(PAYWALL_PROPERTIES);
        when(encryptionProfileProvider.getTlsVersions(any(), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(encryptionProfileProvider.getTlsCipherSuites(any(), any(), any(), anyBoolean()))
                .thenReturn("cipher1,cipher2,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        underTest.runTargetedClusterServices(stack, Map.of("fqdn3", "1.1.1.1"));

        verify(stackUtil, times(1)).collectReachableAndUnreachableCandidateNodes(any(), any());
        verify(stackUtil, times(1)).collectReachableAndCheckNecessaryNodes(any(), any());
        ArgumentCaptor<Set<Node>> reachableCandidates = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).runService(any(), reachableCandidates.capture(), any());
        verify(hostOrchestrator).initServiceRun(any(), any(), any(), any(), saltConfig.capture(), any(), any());
        Set<Node> reachableNodes = reachableCandidates.getValue();
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway1", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway2", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway3", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn3", node.getHostname())));
        assertFalse(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn1", node.getHostname())));
        assertFalse(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn2", node.getHostname())));
        assertTrue(saltConfig.getValue().getServicePillarConfig().keySet().stream().allMatch(Objects::nonNull));
        verifySecretEncryption(false, saltConfig.getValue());
        verifyEncryptionProfile(saltConfig.getValue());

        verifyDefaultKerberosCcacheSecretStorage(DEFAULT_KERBEROS_CCACHE_SECRET_STORAGE, saltConfig.getValue());
        verifyKerberosSecretLocation(KERBEROS_SECRET_LOCATION, saltConfig.getValue());
    }

    @Test
    void testTargetedCallWhenGatewayUnreachable() throws NodesUnreachableException {
        when(stackUtil.collectReachableAndCheckNecessaryNodes(any(), any())).thenThrow(NodesUnreachableException.class);

        assertThrows(CloudbreakServiceException.class, () -> underTest.runTargetedClusterServices(stack, Map.of("fqdn3", "1.1.1.1")));

        verify(stackUtil, times(1)).collectReachableAndCheckNecessaryNodes(any(), any());
        verify(stackUtil, times(0)).collectReachableAndUnreachableCandidateNodes(any(), any());
    }

    @Test
    void testTargetedCallWhenNoReachableNodesFound() throws NodesUnreachableException, CloudbreakOrchestratorException {
        when(stackUtil.collectReachableAndCheckNecessaryNodes(any(), any())).thenReturn(Set.of(node("gateway1")));
        when(stackUtil.collectReachableAndUnreachableCandidateNodes(any(), any())).thenReturn(new NodeReachabilityResult(Set.of(), Set.of()));

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.runTargetedClusterServices(stack, Map.of("fqdn3", "1.1.1.1")));

        assertEquals("No reachable candidates found.", cloudbreakServiceException.getMessage());
        verify(stackUtil, times(1)).collectReachableAndCheckNecessaryNodes(any(), any());
        verify(stackUtil, times(1)).collectReachableAndUnreachableCandidateNodes(any(), any());
        verify(hostOrchestrator, never()).initServiceRun(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testDecoratePillarWithMountInfo() throws CloudbreakOrchestratorException, NodesUnreachableException, CloudbreakException, IOException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("gateway3"));
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        when(stackUtil.collectReachableAndCheckNecessaryNodes(any(), any())).thenReturn(nodes);
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().withVerifyKdcTrust(true).build();
        when(kerberosConfigService.get(ENV_CRN, STACK_NAME)).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);
        when(sssdConfigProvider.createSssdAdPillar(kerberosConfig)).thenReturn(Map.of("ad", new SaltPillarProperties("adpath", Map.of())));
        when(sssdConfigProvider.createSssdIpaPillar(eq(kerberosConfig), anyMap(), eq(ENV_CRN), any(), eq(EXTENDED_BLUEPRINT_TEXT)))
                .thenReturn(Map.of("ipa", new SaltPillarProperties("ipapath", Map.of())));
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(paywallConfigService.createPaywallPillarConfig(stack)).thenReturn(PAYWALL_PROPERTIES);
        when(encryptionProfileProvider.getTlsVersions(any(), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(encryptionProfileProvider.getTlsCipherSuites(any(), any(), any(), anyBoolean()))
                .thenReturn("cipher1,cipher2,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        underTest.runClusterServices(stack, Map.of(), true);

        ArgumentCaptor<Set<Node>> reachableCandidates = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).runService(any(), reachableCandidates.capture(), any());
        verify(hostOrchestrator).initServiceRun(any(), any(), any(), any(), saltConfig.capture(), any(), any());
        verify(recipeEngine).executePreServiceDeploymentRecipes(any(), anyMap(), anySet());
        Set<Node> reachableNodes = reachableCandidates.getValue();
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway1", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway3", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn1", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn2", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn3", node.getHostname())));
        assertFalse(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway2", node.getHostname())));
        assertTrue(saltConfig.getValue().getServicePillarConfig().keySet().stream().allMatch(Objects::nonNull));
        verifySecretEncryption(false, saltConfig.getValue());
        verifyDefaultKerberosCcacheSecretStorage(DEFAULT_KERBEROS_CCACHE_SECRET_STORAGE, saltConfig.getValue());
        verifyKerberosSecretLocation(KERBEROS_SECRET_LOCATION, saltConfig.getValue());
        verifyNoInteractions(backUpDecorator);
        verifyEncryptionProfile(saltConfig.getValue());
    }

    @Test
    void testDecoratePillarWithMountInfoWithoutRecipeVerification()
            throws CloudbreakOrchestratorException, NodesUnreachableException, CloudbreakException, IOException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("gateway3"));
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());
        when(stackUtil.collectReachableAndCheckNecessaryNodes(any(), any())).thenReturn(nodes);
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().withVerifyKdcTrust(true).build();
        when(kerberosConfigService.get(ENV_CRN, STACK_NAME)).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);
        when(sssdConfigProvider.createSssdAdPillar(kerberosConfig)).thenReturn(Map.of("ad", new SaltPillarProperties("adpath", Map.of())));
        when(sssdConfigProvider.createSssdIpaPillar(eq(kerberosConfig), anyMap(), eq(ENV_CRN), any(), eq(EXTENDED_BLUEPRINT_TEXT)))
                .thenReturn(Map.of("ipa", new SaltPillarProperties("ipapath", Map.of())));
        when(paywallConfigService.createPaywallPillarConfig(stack)).thenReturn(PAYWALL_PROPERTIES);
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(encryptionProfileProvider.getTlsVersions(any(), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(encryptionProfileProvider.getTlsCipherSuites(any(), any(), any(), anyBoolean()))
                .thenReturn("cipher1,cipher2,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        underTest.runClusterServices(stack, Map.of(), false);

        ArgumentCaptor<Set<Node>> reachableCandidates = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).runService(any(), reachableCandidates.capture(), any());
        verify(hostOrchestrator).initServiceRun(any(), any(), any(), any(), saltConfig.capture(), any(), any());
        verifyNoInteractions(recipeEngine);
        Set<Node> reachableNodes = reachableCandidates.getValue();
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway1", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway3", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn1", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn2", node.getHostname())));
        assertTrue(reachableNodes.stream().anyMatch(node -> StringUtils.equals("fqdn3", node.getHostname())));
        assertFalse(reachableNodes.stream().anyMatch(node -> StringUtils.equals("gateway2", node.getHostname())));
        assertTrue(saltConfig.getValue().getServicePillarConfig().keySet().stream().allMatch(Objects::nonNull));
        verifySecretEncryption(false, saltConfig.getValue());
        verifyDefaultKerberosCcacheSecretStorage(DEFAULT_KERBEROS_CCACHE_SECRET_STORAGE, saltConfig.getValue());
        verifyKerberosSecretLocation(KERBEROS_SECRET_LOCATION, saltConfig.getValue());
        verifyNoInteractions(backUpDecorator);
        verifyEncryptionProfile(saltConfig.getValue());
    }

    @Test
    void testRedeployGatewayCertificate() throws CloudbreakOrchestratorException {
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("gateway3"));
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(stackUtil.collectNodes(any())).thenReturn(nodes);
        when(stackUtil.collectReachableNodes(any())).thenReturn(nodes);
        List<GatewayConfig> gwConfigs = List.of(
                GatewayConfig.builder()
                        .withConnectionAddress("addr")
                        .withPublicAddress("endpoint")
                        .withPrivateAddress("privateAddr")
                        .withGatewayPort(123)
                        .withInstanceId("instanceId")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gwConfigs);
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(encryptionProfileProvider.getTlsVersions(any(), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(encryptionProfileProvider.getTlsCipherSuites(any(), any(), any(), anyBoolean()))
                .thenReturn("cipher1,cipher2,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        setupMocksForRunClusterServices();

        underTest.redeployGatewayCertificate(stack);

        verify(hostOrchestrator, times(1)).initServiceRun(eq(stack), eq(gwConfigs), eq(nodes), eq(nodes), any(), any(), eq(CloudPlatform.AWS.name()));
        verify(hostOrchestrator).runService(eq(gwConfigs), eq(nodes), any());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testRedeployGatewayPillarOnly() throws CloudbreakOrchestratorFailedException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("gateway3"));
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(stackUtil.collectNodes(any())).thenReturn(nodes);

        underTest.redeployGatewayPillarOnly(stack, Set.of());
        ArgumentCaptor<SaltConfig> saltConfigCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfigCaptor.capture());
        Set<Node> allNodes = allNodesCaptor.getValue();
        assertEquals(5, allNodes.size());
        SaltConfig saltConfig = saltConfigCaptor.getValue();
        assertTrue(saltConfig.getServicePillarConfig().keySet().stream().allMatch(Objects::nonNull));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testRedeployGatewayPillarOnlyWithOneRemoveableNode() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices();
        Node gateway3 = node("gateway3");
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), gateway3);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(stackUtil.collectNodes(any())).thenReturn(nodes);
        when(stackUtil.collectReachableNodes(any())).thenReturn(nodes);
        GrainProperties grainProperties = new GrainProperties();
        grainProperties.put("gateway1", Map.of("roles", "knox"));
        grainProperties.put("gateway3", Map.of("roles", "knox"));
        grainProperties.put("fqdn1", Map.of("roles", "solr"));
        grainProperties.put("fqdn2", Map.of("roles", "solr"));
        grainProperties.put("fqdn3", Map.of("roles", "solr"));
        when(grainPropertiesService.createGrainProperties(any(), any(), any())).thenReturn(List.of(grainProperties));

        underTest.redeployGatewayPillarOnly(stack, Set.of("gateway1", "fqdn1"));
        ArgumentCaptor<SaltConfig> saltConfigCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfigCaptor.capture());
        Set<Node> allNodes = allNodesCaptor.getValue();
        assertEquals(5, allNodes.size());
        SaltConfig saltConfig = saltConfigCaptor.getValue();
        assertTrue(saltConfig.getServicePillarConfig().keySet().stream().allMatch(Objects::nonNull));
        verify(hostOrchestrator).runService(any(), eq(Set.of(gateway3)), any());
    }

    @Test
    void testRedeployStates() throws CloudbreakOrchestratorException {
        List<GatewayConfig> gwConfigs = List.of(
                GatewayConfig.builder()
                        .withConnectionAddress("addr")
                        .withPublicAddress("endpoint")
                        .withPrivateAddress("privateAddr")
                        .withGatewayPort(123)
                        .withInstanceId("instanceId")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gwConfigs);
        underTest.redeployStates(stack);
        verify(hostOrchestrator).uploadStates(gatewayConfigsCaptor.capture(), any());
        List<GatewayConfig> gatewayConfigs = gatewayConfigsCaptor.getValue();
        assertEquals(gwConfigs, gatewayConfigs);
    }

    @Test
    void testCreateCronForUserHomeCreation() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        List<GatewayConfig> gatewayConfigs = List.of(
                GatewayConfig.builder()
                        .withConnectionAddress("addr")
                        .withPublicAddress("endpoint")
                        .withPrivateAddress("privateAddr")
                        .withGatewayPort(123)
                        .withInstanceId("instanceId")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        when(stackUtil.collectReachableAndUnreachableCandidateNodes(eq(stack), any()))
                .thenReturn(new NodeReachabilityResult(Set.of(TestUtil.node()), new HashSet<>()));
        underTest.createCronForUserHomeCreation(stack, Set.of("fqdn"));
        verify(hostOrchestrator).createCronForUserHomeCreation(eq(gatewayConfigs), eq(Set.of("fqdn")), any());
    }

    @Test
    void testAddJavaPillarToSaltConfig() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices();

        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AWS_DEFAULT_VARIANT.value());
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(encryptionProfileProvider.getTlsVersions(any(), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(encryptionProfileProvider.getTlsCipherSuites(any(), any(), any(), anyBoolean()))
                .thenReturn("cipher1,cipher2,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        underTest.runClusterServices(stack, Collections.emptyMap(), false);

        verify(hostOrchestrator).runService(any(), any(), any());
        verify(javaPillarDecorator).createJavaPillars(eq(stack), any());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testAddRangerRazPillarForDataLakeToRedeployGateway() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices();
        setupMockforRangerRaz(StackType.DATALAKE, true);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(encryptionProfileProvider.getTlsVersions(anySet(), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(stack.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(componentLocatorService.getImpalaCoordinatorLocations(any()))
                .thenReturn(Map.of("ip1", List.of("ip1", "ip2")));

        underTest.redeployGatewayPillarOnly(stack, Set.of());

        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfig.capture());

        Map<String, List<String>> locations = (Map<String, List<String>>) getGatewayProperties(saltConfig.getValue()).get("location");
        assertTrue(locations.containsKey("RANGERRAZ"));
        assertEquals(locations.get("RANGERRAZ"), List.of("fqdn1"));
        assertEquals(getGatewayProperties(saltConfig.getValue()).get("enable_impala_sticky_session"), true);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testAddRangerRazPillarForDataLakeRazNotEnabledToRedeployGateway() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices();
        setupMockforRangerRaz(StackType.DATALAKE, false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        underTest.redeployGatewayPillarOnly(stack, Set.of());
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfig.capture());

        assertFalse(((Map<String, List<String>>) getGatewayProperties(saltConfig.getValue()).get("location")).containsKey("RANGERRAZ"));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testAddRangerRazPillarForDataLakeNodesNotAvailableToRedeployGateway() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices();
        setupMockforRangerRaz(StackType.DATALAKE, true);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(stack.getAliveInstancesInInstanceGroup(anyString())).thenReturn(Collections.emptyList());

        underTest.redeployGatewayPillarOnly(stack, Set.of());
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfig.capture());

        assertFalse(((Map<String, List<String>>) getGatewayProperties(saltConfig.getValue()).get("location")).containsKey("RANGERRAZ"));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testAddRangerRazPillarForDataHubToRedeployGateway() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices();
        setupMockforRangerRaz(StackType.WORKLOAD, true);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        underTest.redeployGatewayPillarOnly(stack, Set.of());
        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfig.capture());

        assertTrue(((Map<String, List<String>>) getGatewayProperties(saltConfig.getValue()).get("location")).containsKey("RANGERRAZ"));
        assertEquals(((Map<String, List<String>>) getGatewayProperties(saltConfig.getValue()).get("location")).get("RANGERRAZ"), List.of("fqdn1"));
    }

    private Map<String, Object> getGatewayProperties(SaltConfig saltConfig) {
        return (Map<String, Object>) saltConfig.getServicePillarConfig().get("gateway").getProperties().get("gateway");
    }

    private Map<String, Object> getClusterProperties(SaltConfig saltConfig) {
        return (Map<String, Object>) saltConfig.getServicePillarConfig().get("metadata").getProperties().get("cluster");
    }

    private Map<String, Object> getIDBrokerProperties(SaltConfig saltConfig) {
        return (Map<String, Object>) saltConfig.getServicePillarConfig().get("idbroker").getProperties().get("idbroker");
    }

    private Map<String, Object> getKerberosProperties(SaltConfig saltConfig) {
        return (Map<String, Object>) saltConfig.getServicePillarConfig().get("kerberos").getProperties().get("kerberos");
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
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(loadBalancerFqdnUtil.getLoadBalancersForStack(STACK_ID)).thenReturn(loadBalancers);

        underTest.redeployGatewayPillarOnly(stack, Set.of());

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

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testEDLRangerToplogy() throws IOException, CloudbreakOrchestratorFailedException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("master1"));
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(stackUtil.collectNodes(any())).thenReturn(nodes);
        when(exposedServiceCollector.getAllServiceNames()).thenReturn(Set.of("RANGER"));
        Map<String, List<String>> locations = new HashMap<>();
        locations.put("RANGER", List.of("master1", "master2"));
        when(componentLocator.getComponentLocation(any(), any())).thenReturn(locations);
        StackView stackViewMock = mock(StackView.class);
        when(stackViewMock.getId()).thenReturn(STACK_ID);
        ReflectionTestUtils.setField(stack, "stack", stackViewMock);
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        when(instanceMetadataView.getInstanceMetadataType()).thenReturn(InstanceMetadataType.GATEWAY_PRIMARY);
        when(instanceGroupView.getGroupName()).thenReturn("GATEWAY");
        Map<String, InstanceGroupDto> instanceGroups = new HashMap<>();
        InstanceGroupDto instanceGroupDto = new InstanceGroupDto(instanceGroupView, List.of(instanceMetadataView));
        instanceGroups.put("GATEWAY", instanceGroupDto);
        String edlBP = FileReaderUtils.readFileFromClasspath("cdp-sdx-enterprise.bp");
        Blueprint bp = new Blueprint();
        bp.setBlueprintText(edlBP);
        ReflectionTestUtils.setField(stack, "instanceGroups", instanceGroups);
        ReflectionTestUtils.setField(stack, "blueprint", bp);
        underTest.redeployGatewayPillarOnly(stack, Set.of());
        ArgumentCaptor<SaltConfig> saltConfigCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfigCaptor.capture());
        Set<Node> allNodes = allNodesCaptor.getValue();
        assertEquals(5, allNodes.size());
        SaltConfig saltConfig = saltConfigCaptor.getValue();
        SaltPillarProperties gatewayPillarProperties = saltConfig.getServicePillarConfig().get("gateway");
        Map<String, Object> props = (HashMap<String, Object>) gatewayPillarProperties.getProperties().get("gateway");
        Map<String, List<String>> location = (HashMap<String, List<String>>) props.get("location");
        List<String> ranger = location.get("RANGER");
        assertTrue(ranger.contains("master1"));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testMDRangerToplogy() throws IOException, CloudbreakOrchestratorFailedException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("master1"));
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(stackUtil.collectNodes(any())).thenReturn(nodes);
        when(exposedServiceCollector.getAllServiceNames()).thenReturn(Set.of("RANGER"));
        Map<String, List<String>> locations = new HashMap<>();
        locations.put("RANGER", List.of("master1", "master2"));
        when(componentLocator.getComponentLocation(any(), any())).thenReturn(locations);
        StackView stackViewMock = mock(StackView.class);
        when(stackViewMock.getId()).thenReturn(STACK_ID);
        ReflectionTestUtils.setField(stack, "stack", stackViewMock);
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        when(instanceMetadataView.getInstanceMetadataType()).thenReturn(InstanceMetadataType.GATEWAY_PRIMARY);
        when(instanceGroupView.getGroupName()).thenReturn("GATEWAY");
        Map<String, InstanceGroupDto> instanceGroups = new HashMap<>();
        InstanceGroupDto instanceGroupDto = new InstanceGroupDto(instanceGroupView, List.of(instanceMetadataView));
        instanceGroups.put("GATEWAY", instanceGroupDto);
        String mediumDutyBP = FileReaderUtils.readFileFromClasspath("cdp-sdx-medium-ha.bp");
        Blueprint bp = new Blueprint();
        bp.setBlueprintText(mediumDutyBP);
        ReflectionTestUtils.setField(stack, "instanceGroups", instanceGroups);
        ReflectionTestUtils.setField(stack, "blueprint", bp);

        underTest.redeployGatewayPillarOnly(stack, Set.of());

        ArgumentCaptor<SaltConfig> saltConfigCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfigCaptor.capture());
        Set<Node> allNodes = allNodesCaptor.getValue();
        assertEquals(5, allNodes.size());
        SaltConfig saltConfig = saltConfigCaptor.getValue();
        SaltPillarProperties gatewayPillarProperties = saltConfig.getServicePillarConfig().get("gateway");
        Map<String, Object> props = (HashMap<String, Object>) gatewayPillarProperties.getProperties().get("gateway");
        Map<String, List<String>> location = (HashMap<String, List<String>>) props.get("location");
        List<String> ranger = location.get("RANGER");
        assertTrue(ranger.contains("master1"));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testLDRangerToplogy() throws IOException, CloudbreakOrchestratorFailedException {
        setupMocksForRunClusterServices();
        Set<Node> nodes = Sets.newHashSet(node("fqdn1"), node("fqdn2"), node("fqdn3"),
                node("gateway1"), node("master1"));
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(stackUtil.collectNodes(any())).thenReturn(nodes);
        when(exposedServiceCollector.getAllServiceNames()).thenReturn(Set.of("RANGER"));
        Map<String, List<String>> locations = new HashMap<>();
        locations.put("RANGER", List.of("master1", "master2"));
        when(componentLocator.getComponentLocation(any(), any())).thenReturn(locations);
        StackView stackViewMock = mock(StackView.class);
        when(stackViewMock.getId()).thenReturn(STACK_ID);
        ReflectionTestUtils.setField(stack, "stack", stackViewMock);
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        when(instanceMetadataView.getInstanceMetadataType()).thenReturn(InstanceMetadataType.GATEWAY_PRIMARY);
        when(instanceGroupView.getGroupName()).thenReturn("GATEWAY");
        Map<String, InstanceGroupDto> instanceGroups = new HashMap<>();
        InstanceGroupDto instanceGroupDto = new InstanceGroupDto(instanceGroupView, List.of(instanceMetadataView));
        instanceGroups.put("GATEWAY", instanceGroupDto);
        String lightDutyBP = FileReaderUtils.readFileFromClasspath("cdp-sdx.bp");
        Blueprint bp = new Blueprint();
        bp.setBlueprintText(lightDutyBP);
        ReflectionTestUtils.setField(stack, "instanceGroups", instanceGroups);
        ReflectionTestUtils.setField(stack, "blueprint", bp);

        underTest.redeployGatewayPillarOnly(stack, Set.of());

        ArgumentCaptor<SaltConfig> saltConfigCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).uploadGatewayPillar(any(), allNodesCaptor.capture(), any(), saltConfigCaptor.capture());
        Set<Node> allNodes = allNodesCaptor.getValue();
        assertEquals(5, allNodes.size());
        SaltConfig saltConfig = saltConfigCaptor.getValue();
        SaltPillarProperties gatewayPillarProperties = saltConfig.getServicePillarConfig().get("gateway");
        Map<String, Object> props = (HashMap<String, Object>) gatewayPillarProperties.getProperties().get("gateway");
        Map<String, List<String>> location = (HashMap<String, List<String>>) props.get("location");
        List<String> ranger = location.get("RANGER");
        assertTrue(ranger.contains("master1"));
        assertTrue(ranger.contains("master2"));
    }

    @Test
    void removeSecurityConfigFromCMTest() throws CloudbreakOrchestratorException {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(1L);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getId()).thenReturn(2L);
        when(stackDto.getCluster()).thenReturn(cluster);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stackDto)).thenReturn(gatewayConfig);
        Node node1 = new Node("192.168.1.1", "192.168.1.1", "i-1", "type", "fqdn1", "compute");
        Node node2 = new Node("192.168.1.2", "192.168.1.2", "i-2", "type", "fqdn2", "compute");
        Node node3 = new Node("192.168.1.3", "192.168.1.3", "i-3", "type", "fqdn3", "worker");
        underTest.removeSecurityConfigFromCMAgentsConfig(stackDto, Set.of(node1, node2, node3));
        ArgumentCaptor<Set<String>> setArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(hostOrchestrator).removeSecurityConfigFromCMAgentsConfig(eq(gatewayConfig), setArgumentCaptor.capture());
        assertThat(setArgumentCaptor.getValue()).containsExactlyInAnyOrder(node1.getHostname(), node2.getHostname(), node3.getHostname());
        ArgumentCaptor<ExitCriteriaModel> exitCriteriaModelArgumentCaptor = ArgumentCaptor.forClass(ExitCriteriaModel.class);
        verify(hostOrchestrator).restartClusterManagerAgents(eq(gatewayConfig), setArgumentCaptor.capture(), exitCriteriaModelArgumentCaptor.capture());
        assertThat(setArgumentCaptor.getValue()).containsExactlyInAnyOrder(node1.getHostname(), node2.getHostname(), node3.getHostname());
        ClusterDeletionBasedExitCriteriaModel exitCriteriaModel =
                (ClusterDeletionBasedExitCriteriaModel) exitCriteriaModelArgumentCaptor.getValue();
        assertEquals(1L, exitCriteriaModel.getStackId().get());
        assertEquals(2L, exitCriteriaModel.getClusterId().get());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testKnoxSecurityConfigToRunClusterServices() throws CloudbreakOrchestratorException, IOException {
        setupMocksForRunClusterServices();
        GatewayView clusterGateway = mock(GatewayView.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        when(gatewayService.getByClusterId(CLUSTER_ID)).thenReturn(Optional.of(clusterGateway));
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AWS_DEFAULT_VARIANT.value());
        when(environmentResponse.isEnableSecretEncryption()).thenReturn(true);
        when(idBrokerService.getByCluster(CLUSTER_ID)).thenReturn(mock(IdBroker.class));
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().withVerifyKdcTrust(true).build();
        when(kerberosConfigService.get(ENV_CRN, STACK_NAME)).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);
        when(entitlementService.isTlsv13Enabled(ACCOUNT_ID)).thenReturn(true);
        when(encryptionProfileProvider.getTlsVersions(any(), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(encryptionProfileProvider.getTlsCipherSuites(any(), any(), any(), anyBoolean()))
                .thenReturn("cipher1,cipher2,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        underTest.runClusterServices(stack, Collections.emptyMap(), false);

        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).initServiceRun(any(), any(), any(), any(), saltConfig.capture(), any(), any());

        verifySecretEncryption(true, saltConfig.getValue());
        verifyEncryptionProfile(saltConfig.getValue());

        String knoxGatewaySecurityDir = (String) getGatewayProperties(saltConfig.getValue()).get("knoxGatewaySecurityDir");
        assertEquals(KNOX_GATEWAY_SECURITY_DIR, knoxGatewaySecurityDir);

        String knoxIdBrokerSecurityDir = (String) getIDBrokerProperties(saltConfig.getValue()).get("knoxIdBrokerSecurityDir");
        assertEquals(KNOX_IDBROKER_SECURITY_DIR, knoxIdBrokerSecurityDir);

        verifyDefaultKerberosCcacheSecretStorage(DEFAULT_KERBEROS_CCACHE_SECRET_STORAGE, saltConfig.getValue());
        verifyKerberosSecretLocation(KERBEROS_SECRET_LOCATION, saltConfig.getValue());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testCdpLuksVolumeBackUpToRunClusterServices() throws CloudbreakOrchestratorException, IOException {
        setupMocksForRunClusterServices();
        GatewayView clusterGateway = mock(GatewayView.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        when(gatewayService.getByClusterId(CLUSTER_ID)).thenReturn(Optional.of(clusterGateway));
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AWS_DEFAULT_VARIANT.value());
        when(environmentResponse.isEnableSecretEncryption()).thenReturn(true);
        when(idBrokerService.getByCluster(CLUSTER_ID)).thenReturn(mock(IdBroker.class));
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().withVerifyKdcTrust(true).build();
        when(kerberosConfigService.get(ENV_CRN, STACK_NAME)).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);
        when(entitlementService.isTlsv13Enabled(ACCOUNT_ID)).thenReturn(true);
        when(encryptionProfileProvider.getTlsVersions(any(), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(encryptionProfileProvider.getTlsCipherSuites(any(), any(), any(), anyBoolean()))
                .thenReturn("cipher1,cipher2,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        underTest.runClusterServices(stack, Collections.emptyMap(), false);

        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).initServiceRun(any(), any(), any(), any(), saltConfig.capture(), any(), any());

        verifySecretEncryption(true, saltConfig.getValue());
        verifyEncryptionProfile(saltConfig.getValue());
        verifyCmVersionSupportsTlsSetup(saltConfig.getValue(), false);
        verify(backUpDecorator).decoratePillarWithBackup(eq(stack), eq(environmentResponse), any());
    }

    @Test
    void testCmVersionSupportsTlsSetup() throws CloudbreakOrchestratorException, IOException {
        setupMocksForRunClusterServices("7.13.2.0");
        GatewayView clusterGateway = mock(GatewayView.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        when(gatewayService.getByClusterId(CLUSTER_ID)).thenReturn(Optional.of(clusterGateway));
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AWS_DEFAULT_VARIANT.value());
        when(environmentResponse.isEnableSecretEncryption()).thenReturn(true);
        when(idBrokerService.getByCluster(CLUSTER_ID)).thenReturn(mock(IdBroker.class));
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().withVerifyKdcTrust(true).build();
        when(kerberosConfigService.get(ENV_CRN, STACK_NAME)).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.areClusterManagerManagedKerberosPackages(kerberosConfig)).thenReturn(true);
        when(encryptionProfileProvider.getTlsVersions(any(), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(encryptionProfileProvider.getTlsCipherSuites(any(), any(), any(), anyBoolean()))
                .thenReturn("cipher1,cipher2,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);

        underTest.runClusterServices(stack, Collections.emptyMap(), false);

        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);

        verify(hostOrchestrator).initServiceRun(any(), any(), any(), any(), saltConfig.capture(), any(), any());
        verifyEncryptionProfile(saltConfig.getValue());
        verifyCmVersionSupportsTlsSetup(saltConfig.getValue(), true);
        verifyTlsAdvancedControl(saltConfig.getValue(), true);
    }

    @Test
    void testAlternativeUserfacingCert() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices("7.13.2.0");
        GatewayView clusterGateway = mock(GatewayView.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        when(gatewayService.getByClusterId(CLUSTER_ID)).thenReturn(Optional.of(clusterGateway));
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AWS_DEFAULT_VARIANT.value());
        when(encryptionProfileProvider.getTlsVersions(any(), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(encryptionProfileProvider.getTlsCipherSuites(any(), any(), any(), anyBoolean()))
                .thenReturn("cipher1,cipher2,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getPrivateAddress()).thenReturn("1.2.3.4");
        when(gatewayConfig.getHostname()).thenReturn("hostname");
        when(gatewayConfig.getAlternativeUserFacingCert()).thenReturn("cert");
        when(gatewayConfig.getAlternativeUserFacingKey()).thenReturn("key");
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);

        underTest.runClusterServices(stack, Collections.emptyMap(), false);

        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).initServiceRun(any(), any(), any(), any(), saltConfig.capture(), any(), any());
        verifyEncryptionProfile(saltConfig.getValue());
        verifyAlternativeUserfacingCertAndKey(saltConfig.getValue(), true, "cert", "key");
    }

    @Test
    void testCustomEncryptionProfile() throws CloudbreakOrchestratorException {
        setupMocksForRunClusterServices("7.13.2.0");
        GatewayView clusterGateway = mock(GatewayView.class);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mock(DetailedEnvironmentResponse.class);
        EncryptionProfileResponse encryptionProfileResponse = mock(EncryptionProfileResponse.class);
        Set<String> tlsVersions = Set.of("TLSv1.2", "TLSv1.3");
        Map<String, List<String>> cipherSuites = Map.of("TLSv1.2",
                List.of("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"),
                "TLSv1.3",
                List.of("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"));

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        when(gatewayService.getByClusterId(CLUSTER_ID)).thenReturn(Optional.of(clusterGateway));
        when(stackView.getPlatformVariant()).thenReturn(AwsConstants.AWS_DEFAULT_VARIANT.value());
        when(encryptionProfileService.getEncryptionProfileByCrnOrDefault(any(), any())).thenReturn(encryptionProfileResponse);
        when(encryptionProfileResponse.getTlsVersions()).thenReturn(tlsVersions);
        when(encryptionProfileResponse.getCipherSuites()).thenReturn(cipherSuites);
        when(encryptionProfileProvider.getTlsVersions(eq(tlsVersions), any())).thenReturn("TLSv1.2,TLSv1.3");
        when(encryptionProfileProvider.getTlsCipherSuites(eq(cipherSuites), any(), any(), anyBoolean()))
                .thenReturn("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        underTest.runClusterServices(stack, Collections.emptyMap(), false);

        ArgumentCaptor<SaltConfig> saltConfig = ArgumentCaptor.forClass(SaltConfig.class);
        verify(hostOrchestrator).initServiceRun(any(), any(), any(), any(), saltConfig.capture(), any(), any());
        verifyEncryptionProfile(saltConfig.getValue());
    }

    private void setupMocksForRunClusterServices() {
        setupMocksForRunClusterServices("7.2.2");
    }

    private void setupMocksForRunClusterServices(String cmVersion) {
        when(umsClient.getAccountDetails(any())).thenReturn(UserManagementProto.Account.getDefaultInstance());
        when(stackView.getTunnel()).thenReturn(Tunnel.DIRECT);
        when(stackView.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        when(stackView.getResourceCrn()).thenReturn(TEST_CLUSTER_CRN);
        when(cluster.getName()).thenReturn("clustername");
        when(componentLocator.getComponentLocation(any(), any())).thenReturn(new HashMap<>());
        when(exposedServiceCollector.getImpalaService()).thenReturn(mock(ExposedService.class));
        when(environmentConfigProvider.getParentEnvironmentCrn(any())).thenReturn("crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com");
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clouderaManagerRepo.getVersion()).thenReturn(cmVersion);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        lenient().when(gatewayConfig.getPrivateAddress()).thenReturn("1.2.3.4");
        lenient().when(gatewayConfig.getHostname()).thenReturn("hostname");
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        ExposedService rangerMock = mock(ExposedService.class);
        when(rangerMock.getServiceName()).thenReturn("RANGER");
        when(exposedServiceCollector.getRangerService()).thenReturn(rangerMock);
        ExposedService cmExposedService = mock(ExposedService.class);
        when(cmExposedService.getServiceName()).thenReturn("CM");
        ExposedService rangerRazExposedService = mock(ExposedService.class);
        lenient().when(rangerRazExposedService.getServiceName()).thenReturn("RANGERRAZ");
        when(exposedServiceCollector.getClouderaManagerService()).thenReturn(cmExposedService);
        lenient().when(exposedServiceCollector.getRangerRazService()).thenReturn(rangerRazExposedService);

        Template template = new Template();
        template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES);

        List<InstanceGroupDto> instanceGroups = new ArrayList<>();
        createInstanceGroup(template, instanceGroups, "fqdn1", null, "1.1.1.1", "1.1.1.2");
        createInstanceGroup(template, instanceGroups, "fqdn2", null, "1.1.2.1", "1.1.2.2");

        when(stack.getInstanceGroupDtos()).thenReturn(instanceGroups);
        RdsConfigWithoutCluster rdsConfigWithoutCluster = mock(RdsConfigWithoutCluster.class);
        RdsView rdsView = mock(RdsView.class);
        when(rdsConfigWithoutClusterService.findByClusterIdAndType(any(), eq(DatabaseType.CLOUDERA_MANAGER))).thenReturn(rdsConfigWithoutCluster);
        when(rdsViewProvider.getRdsView(any(RdsConfigWithoutCluster.class), anyString(), anyBoolean())).thenReturn(rdsView);
        when(loadBalancerSANProvider.getLoadBalancerSAN(anyLong(), any())).thenReturn(Optional.empty());
        ClusterPreCreationApi clusterPreCreationApi = mock(ClusterPreCreationApi.class);
        when(clusterApiConnectors.getConnector(cluster)).thenReturn(clusterPreCreationApi);
        ServiceLocationMap serviceLocationMap = new ServiceLocationMap();
        serviceLocationMap.add(new ServiceLocation("serv", "paath"));
        when(clusterPreCreationApi.getServiceLocations()).thenReturn(serviceLocationMap);
        ReflectionTestUtils.setField(underTest, "cmHeartbeatInterval", "1");
        ReflectionTestUtils.setField(underTest, "cmMissedHeartbeatInterval", "1");
        ReflectionTestUtils.setField(underTest, "knoxGatewaySecurityDir", KNOX_GATEWAY_SECURITY_DIR);
        ReflectionTestUtils.setField(underTest, "knoxIdBrokerSecurityDir", KNOX_IDBROKER_SECURITY_DIR);
        ReflectionTestUtils.setField(kerberosPillarConfigGenerator, "defaultKerberosCcacheSecretLocation", DEFAULT_KERBEROS_CCACHE_SECRET_STORAGE);
        ReflectionTestUtils.setField(kerberosPillarConfigGenerator, "kerberosSecretLocation", KERBEROS_SECRET_LOCATION);

        TemplatePreparationObject templatePreparationObject = TemplatePreparationObject.Builder.builder()
                .withBlueprintView(new BlueprintView())
                .build();

        when(stackToTemplatePreparationObjectConverter.convert(any())).thenReturn(templatePreparationObject);
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

    private void setupMockforRangerRaz(StackType stackType, boolean razEnabled) {
        when(stackView.getType()).thenReturn(stackType);
        when(rangerRazDatalakeConfigProvider.getHostGroups(any(), any())).thenReturn(Set.of("master"));
        when(rangerRazDatahubConfigProvider.getHostGroups(any(), any())).thenReturn(Set.of("master"));
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        HostGroup hostGroup = new HostGroup();
        hostGroup.setInstanceGroup(instanceGroup);
        hostGroup.setName("master");

        when(hostGroupService.getByCluster(anyLong())).thenReturn(razEnabled ? Set.of(hostGroup) : Collections.emptySet());

        List<InstanceMetadataView> instanceMetaDataSet = new ArrayList<>();
        instanceMetaDataSet.add(createInstanceMetadata("fqdn1", instanceGroup, "0.0.0.0"));
        when(stack.getAliveInstancesInInstanceGroup(anyString())).thenReturn(instanceMetaDataSet);
    }

    private void verifySecretEncryption(boolean expectedValue, SaltConfig saltConfig) {
        boolean secretEncryptionEnabled = (Boolean) getClusterProperties(saltConfig).get("secretEncryptionEnabled");
        assertEquals(expectedValue, secretEncryptionEnabled);
    }

    private void verifyEncryptionProfile(SaltConfig saltConfig) {
        String tlsChipherSuites = (String) getClusterProperties(saltConfig).get("tlsCipherSuitesJavaIntermediate");
        assertTrue(tlsChipherSuites.contains("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"));
    }

    private void verifyAlternativeUserfacingCertAndKey(SaltConfig saltConfig, Boolean alternativeCertConfigured, String alternativeCert,
            String alternativeKey) {
        Boolean alternativeUserfacingCertConfigured = (Boolean) getGatewayProperties(saltConfig).get("alternativeuserfacingcert_configured");
        String alternativeUserfacingCert = (String) getGatewayProperties(saltConfig).get("alternativeuserfacingcert");
        String alternativeUserfacingKey = (String) getGatewayProperties(saltConfig).get("alternativeuserfacingkey");
        assertEquals(alternativeCertConfigured, alternativeUserfacingCertConfigured);
        assertEquals(alternativeCert, alternativeUserfacingCert);
        assertEquals(alternativeKey, alternativeUserfacingKey);
    }

    private void verifyTlsAdvancedControl(SaltConfig saltConfig, Boolean tlsAdvancedControlEnabled) {
        Boolean tlsAdvancedControl = (Boolean) getClusterProperties(saltConfig).get("tlsAdvancedControl");
        assertEquals(tlsAdvancedControlEnabled, tlsAdvancedControl);
    }

    private void verifyDefaultKerberosCcacheSecretStorage(String expectedValue, SaltConfig saltConfig) {
        String defaultKerberosCcacheSecretStorage = (String) getKerberosProperties(saltConfig).get("cCacheSecretLocation");
        assertEquals(expectedValue, defaultKerberosCcacheSecretStorage);
    }

    private void verifyKerberosSecretLocation(String expectedValue, SaltConfig saltConfig) {
        String kerberosSecretLocation = (String) getKerberosProperties(saltConfig).get("kerberosSecretLocation");
        assertEquals(expectedValue, kerberosSecretLocation);
    }

    private void verifyCmVersionSupportsTlsSetup(SaltConfig saltConfig, Boolean cmSupported) {
        Boolean cmVersionSupportsTlsSetup = (Boolean) getClusterProperties(saltConfig).get("cmVersionSupportsTlsSetup");
        assertEquals(cmSupported, cmVersionSupportsTlsSetup);
    }
}
