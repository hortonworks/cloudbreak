package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.CMLicenseParser;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.CsdParcelDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.HostAttributeDecorator;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.TelemetryDecorator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MountDisks;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(MockitoJUnitRunner.class)
public class ClusterHostServiceRunnerTest {

    private static final Long CLUSTER_ID = 1L;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
    private RdsConfigService rdsConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private RecipeEngine recipeEngine;

    @Mock
    private BlueprintService blueprintService;

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

    @InjectMocks
    private ClusterHostServiceRunner underTest;

    @Before
    public void setUp() {
        initMocks(this);
        when(stack.getEnvironmentCrn()).thenReturn("envCrn");
        when(environmentConfigProvider.getParentEnvironmentCrn(any())).thenReturn("envCrn");
    }

    @Test
    public void shouldUsecollectAndCheckReachableNodes() throws NodesUnreachableException {
        try {
            expectedException.expect(NullPointerException.class);
            underTest.runClusterServices(stack, cluster, Map.of());
        } catch (Exception e) {
            verify(stackUtil).collectAndCheckReachableNodes(eq(stack), any());
            throw e;
        }
    }

    @Test
    public void collectAndCheckReachableNodesThrowsException() throws NodesUnreachableException {
        Set<String> unreachableNodes = new HashSet<>();
        unreachableNodes.add("node1.example.com");
        when(stackUtil.collectAndCheckReachableNodes(eq(stack), any())).thenThrow(new NodesUnreachableException("error", unreachableNodes));

        CloudbreakServiceException cloudbreakServiceException = Assertions.assertThrows(CloudbreakServiceException.class,
                () -> underTest.runClusterServices(stack, cluster, Map.of()));
        assertEquals("Can not run cluster services on new nodes because the configuration management service is not responding on these nodes: " +
                "[node1.example.com]", cloudbreakServiceException.getMessage());
    }

    @Test
    public void testDecoratePillarWithClouderaManagerRepo() throws IOException, CloudbreakOrchestratorFailedException {
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
    public void testDecoratePillarWithClouderaManagerRepoWithNoJsonLicense() throws IOException, CloudbreakOrchestratorFailedException {
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
    public void testDecoratePillarWithClouderaManagerRepoWithEmptyLicense() throws IOException, CloudbreakOrchestratorFailedException {
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
}
