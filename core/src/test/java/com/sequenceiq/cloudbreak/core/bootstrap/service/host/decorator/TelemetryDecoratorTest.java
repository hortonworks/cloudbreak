package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.service.environment.telemetry.AccountTelemetryClientService;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigService;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigView;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigService;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfigService;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfigView;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigView;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class TelemetryDecoratorTest {

    private TelemetryDecorator underTest;

    @Mock
    private DatabusConfigService databusConfigService;

    @Mock
    private FluentConfigService fluentConfigService;

    @Mock
    private MeteringConfigService meteringConfigService;

    @Mock
    private MonitoringConfigService monitoringConfigService;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @Mock
    private AccountTelemetryClientService accountTelemetryClientService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        AltusCredential altusCredential = new AltusCredential("myAccessKey", "mySecretKey".toCharArray());
        given(altusMachineUserService.generateDatabusMachineUserForFluent(any(Stack.class), any(Telemetry.class)))
                .willReturn(Optional.of(altusCredential));
        underTest = new TelemetryDecorator(databusConfigService, fluentConfigService,
                meteringConfigService, monitoringConfigService, altusMachineUserService,
                accountTelemetryClientService, "1.0.0");
    }

    @Test
    public void testS3DecorateWithDefaultPath() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder().withPlatform("AWS").build();
        FluentConfigView fluentConfigView = new FluentConfigView.Builder()
                .withClusterDetails(clusterDetails)
                .withEnabled(true)
                .withS3LogArchiveBucketName("mybucket")
                .withLogFolderName("cluster-logs/datahub/cl1")
                .withProviderPrefix("s3")
                .build();
        DatabusConfigView dataConfigView = new DatabusConfigView.Builder()
                .withAccessKeySecretAlgorithm("RSA")
                .build();
        mockConfigServiceResults(dataConfigView, fluentConfigView, new MeteringConfigView.Builder().build());
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar,
                createStack(), new Telemetry());
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(result, "fluent");
        assertEquals(results.get("providerPrefix"), "s3");
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("logFolderName"), "cluster-logs/datahub/cl1");
        assertEquals(results.get("enabled"), true);
        assertEquals(results.get("platform"), CloudPlatform.AWS.name());
        assertEquals(results.get("user"), "root");
        verify(fluentConfigService, times(1)).createFluentConfigs(any(TelemetryClusterDetails.class),
                anyBoolean(), anyBoolean(), any(Telemetry.class), anyList());
        verify(meteringConfigService, times(1)).createMeteringConfigs(anyBoolean(), anyString(), anyString(), anyString(),
                anyString());
    }

    @Test
    public void testS3DecorateWithOverrides() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("providerPrefix", "s3a");
        TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder().withPlatform("AWS").build();
        FluentConfigView fluentConfigView = new FluentConfigView.Builder()
                .withClusterDetails(clusterDetails)
                .withEnabled(true)
                .withS3LogArchiveBucketName("mybucket")
                .withLogFolderName("cluster-logs/datahub/cl1")
                .withProviderPrefix("s3")
                .withOverrideAttributes(overrides)
                .build();
        DatabusConfigView dataConfigView = new DatabusConfigView.Builder()
                .build();
        mockConfigServiceResults(dataConfigView, fluentConfigView, new MeteringConfigView.Builder().build());
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar,
                createStack(), new Telemetry());
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(result, "fluent");
        assertEquals(results.get("providerPrefix"), "s3a");
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("logFolderName"), "cluster-logs/datahub/cl1");
        assertEquals(results.get("enabled"), true);
    }

    @Test
    public void testDecorateWithMetering() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        MeteringConfigView meteringConfigView = new MeteringConfigView.Builder()
                .withEnabled(true)
                .withPlatform("AWS")
                .withServiceType("DATAHUB")
                .withServiceVersion("1.0.0")
                .withClusterCrn("myClusterCrn")
                .build();
        DatabusConfigView dataConfigView = new DatabusConfigView.Builder()
                .build();
        mockConfigServiceResults(dataConfigView, new FluentConfigView.Builder().build(), meteringConfigView);
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar,
                createStack(), new Telemetry());
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(result, "metering");
        assertEquals(results.get("serviceType"), "DATAHUB");
        assertEquals(results.get("serviceVersion"), "1.0.0");
        assertEquals(results.get("clusterCrn"), "myClusterCrn");
        assertEquals(results.get("enabled"), true);
    }

    @Test
    public void testDecorateWithMonitoring() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        TelemetryClusterDetails clusterDetails = TelemetryClusterDetails.Builder.builder()
                .withCrn("myClusterCrn")
                .withType("datahub")
                .withVersion("1.0.0")
                .build();
        MonitoringConfigView monitoringConfigView = new MonitoringConfigView.Builder()
                .withEnabled(true)
                .withClusterDetails(clusterDetails)
                .build();
        DatabusConfigView dataConfigView = new DatabusConfigView.Builder()
                .build();
        MeteringConfigView meteringConfigView = new MeteringConfigView.Builder().build();
        mockConfigServiceResults(dataConfigView, new FluentConfigView.Builder().build(), meteringConfigView, monitoringConfigView);
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar,
                createStack(), new Telemetry());
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(result, "monitoring");
        assertEquals(results.get("clusterType"), "datahub");
        assertEquals(results.get("clusterVersion"), "1.0.0");
        assertEquals(results.get("clusterCrn"), "myClusterCrn");
        assertEquals(results.get("enabled"), true);
    }

    @Test
    public void testDecorateWithDisabledLogging() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        FluentConfigView fluentConfigView = new FluentConfigView.Builder()
                .build();
        DatabusConfigView dataConfigView = new DatabusConfigView.Builder()
                .build();
        mockConfigServiceResults(dataConfigView, fluentConfigView, new MeteringConfigView.Builder().build());
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar,
                createStack(), new Telemetry());
        // THEN
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDecorateWithDatabus() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        FluentConfigView fluentConfigView = new FluentConfigView.Builder()
                .build();
        DatabusConfigView dataConfigView = new DatabusConfigView.Builder()
                .withEnabled(true)
                .withAccessKeyId("myAccessKeyId")
                .withAccessKeySecret("mySecret".toCharArray())
                .withEndpoint("databusEndpoint")
                .build();
        mockConfigServiceResults(dataConfigView, fluentConfigView, new MeteringConfigView.Builder().build());
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar,
                createStack(), new Telemetry());
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(result, "databus");
        assertEquals(results.get("accessKeyId"), "myAccessKeyId");
        assertEquals(results.get("enabled"), true);
    }

    private Map<String, Object> createMapFromFluentPillars(Map<String, SaltPillarProperties> servicePillar, String pillarType) {
        return (Map<String, Object>) servicePillar.get(pillarType).getProperties().get(pillarType);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        stack.setCloudPlatform("AWS");
        stack.setResourceCrn("stackCrn");
        Cluster cluster = new Cluster();
        cluster.setName("cl1");
        cluster.setCloudbreakClusterManagerMonitoringUser("myUsr");
        cluster.setCloudbreakClusterManagerMonitoringPassword("myPass");
        stack.setCluster(cluster);
        User creator = new User();
        creator.setUserCrn("userCrn");
        stack.setCreator(creator);
        stack.setResourceCrn("crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        return stack;
    }

    private void mockConfigServiceResults(DatabusConfigView databusConfigView, FluentConfigView fluentConfigView,
            MeteringConfigView meteringConfigView) {
        mockConfigServiceResults(databusConfigView, fluentConfigView, meteringConfigView, new MonitoringConfigView.Builder().build());
    }

    private void mockConfigServiceResults(DatabusConfigView databusConfigView, FluentConfigView fluentConfigView,
            MeteringConfigView meteringConfigView, MonitoringConfigView monitoringConfigView) {
        given(databusConfigService.createDatabusConfigs(anyString(), any(), isNull(), isNull()))
                .willReturn(databusConfigView);
        given(fluentConfigService.createFluentConfigs(any(TelemetryClusterDetails.class),
                anyBoolean(), anyBoolean(), any(Telemetry.class), anyList()))
                .willReturn(fluentConfigView);
        given(meteringConfigService.createMeteringConfigs(anyBoolean(), anyString(), anyString(), anyString(),
                anyString())).willReturn(meteringConfigView);
        given(monitoringConfigService.createMonitoringConfig(any(), any(), any()))
                .willReturn(monitoringConfigView);
    }

}