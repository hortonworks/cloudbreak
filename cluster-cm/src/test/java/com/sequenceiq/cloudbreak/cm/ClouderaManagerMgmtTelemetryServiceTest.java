package com.sequenceiq.cloudbreak.cm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.MgmtRoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.metering.MeteringServiceFieldResolver;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.monitoring.ExporterConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerMgmtTelemetryServiceTest {

    private static final Integer EXPORTER_PORT = 61010;

    private static final String SDX_STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final String SDX_CONTEXT_NAME = "mystack";

    private static final String DATABUS_ENDPOINT = "https://dbusapi.sigma-dev.cloudera.com";

    @InjectMocks
    private ClouderaManagerMgmtTelemetryService underTest;

    @Mock
    private ClouderaManagerExternalAccountService externalAccountService;

    @Mock
    private ClouderaManagerDatabusService clouderaManagerDatabusService;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerResourceApi cmResourceApi;

    @Mock
    private MgmtRoleConfigGroupsResourceApi mgmtRoleConfigGroupsResourceApi;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @Mock
    private ExporterConfiguration cmMonitoringConfiguration;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private MeteringServiceFieldResolver meteringServiceFieldResolver;

    @Captor
    private ArgumentCaptor<ApiConfigList> apiConfigListCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> accountConfigsMapCaptor;

    private ApiClient apiClient;

    private Stack stack;

    private Telemetry telemetry;

    private ProxyConfig proxyConfig;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        User user = new User();
        user.setUserCrn("crn:cdp:iam:us-west-1:accountId:user:name");
        stack.setCreator(user);
        stack.setName("mystack");
        stack.setEnvironmentCrn("crn:cdp:environments:us-west-1:accountId:environment:name");
        stack.setDatalakeCrn("crn:cdp:datalake:us-west-1:accountId:datalake:name");
        stack.setCloudPlatform("AWS");
        stack.setRegion("us-west-1");
        Cluster cluster = new Cluster();
        Blueprint blueprint = mock(Blueprint.class);
        lenient().when(blueprint.getBlueprintJsonText()).thenReturn("blueprint");
        cluster.setBlueprint(blueprint);
        cluster.setCloudbreakClusterManagerMonitoringUser("monitoringUser");
        cluster.setCloudbreakClusterManagerMonitoringPassword("monitoringPassword");
        stack.setCluster(cluster);

        apiClient = new ApiClient();

        telemetry = new Telemetry();
        WorkloadAnalytics workloadAnalytics = new WorkloadAnalytics();
        workloadAnalytics.setDatabusEndpoint(DATABUS_ENDPOINT);
        workloadAnalytics.setAttributes(Map.of(
                "test.attribute.1", "value1",
                "test.attribute.2", "value2"
        ));
        telemetry.setWorkloadAnalytics(workloadAnalytics);
        Monitoring monitoring = new Monitoring();
        monitoring.setRemoteWriteUrl("url");
        telemetry.setMonitoring(monitoring);
        Features features = new Features();
        features.addMonitoring(true);
        telemetry.setFeatures(features);

        proxyConfig = ProxyConfig.builder()
                .withServerHost("proxyHost")
                .withServerPort(1234)
                .withProxyAuthentication(ProxyAuthentication.builder()
                        .withUserName("user")
                        .withPassword("password")
                        .build())
                .build();
    }

    private static Stream<Arguments> testSetupTelemetryRoleArguments() {
        return Stream.of(
                Arguments.of("crn:cdp:datalake:us-west-1:accountId:datalake:name", StackType.DATALAKE, false),
                Arguments.of("crn:cdp:datahub:us-west-1:accountId:cluster:name", StackType.WORKLOAD, false),
                Arguments.of("crn:cdp:datalake:us-west-1:accountId:datalake:name", StackType.DATALAKE, true),
                Arguments.of("crn:cdp:datahub:us-west-1:accountId:cluster:name", StackType.WORKLOAD, true)
        );
    }

    @ParameterizedTest
    @MethodSource("testSetupTelemetryRoleArguments")
    void testSetupTelemetryRole(String resourceCrn, StackType stackType, boolean govCloud) throws ApiException {
        stack.setResourceCrn(resourceCrn);
        stack.setType(stackType);
        if (govCloud) {
            stack.setPlatformVariant("govCloud");
        }
        ApiHostRef cmHostRef = new ApiHostRef();
        ApiRoleList mgmtRoles = new ApiRoleList();
        if (!govCloud) {
            AltusCredential credential = new AltusCredential("accessKey", "secretKey".toCharArray());
            when(entitlementService.useDataBusCNameEndpointEnabled("accountId")).thenReturn(false);
            when(dataBusEndpointProvider.getDataBusEndpoint(DATABUS_ENDPOINT, false)).thenReturn(DATABUS_ENDPOINT);
            when(clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient)).thenReturn(cmResourceApi);
            when(clouderaManagerDatabusService.getAltusCredential(stack, SDX_STACK_CRN)).thenReturn(credential);

            underTest.setupTelemetryRole(stack, apiClient, cmHostRef, mgmtRoles, telemetry, SDX_STACK_CRN);

            verify(cmResourceApi).updateConfig(eq("Adding telemetry settings."), apiConfigListCaptor.capture());
            verify(externalAccountService).createExternalAccount(eq("cb-altus-access"), eq("cb-altus-access"), eq("ALTUS_ACCESS_KEY_AUTH"),
                    accountConfigsMapCaptor.capture(), eq(apiClient));
            ApiConfigList apiConfigList = apiConfigListCaptor.getValue();
            assertTrue(containsConfigWithValue(apiConfigList, "telemetry_master", "true"));
            assertTrue(containsConfigWithValue(apiConfigList, "telemetry_wa", "true"));
            assertTrue(containsConfigWithValue(apiConfigList, "telemetry_collect_job_logs", "true"));
            assertTrue(containsConfigWithValue(apiConfigList, "telemetry_altus_account", "cb-altus-access"));
            assertTrue(containsConfigWithValue(apiConfigList, "telemetry_altus_url", DATABUS_ENDPOINT));
            assertThat(accountConfigsMapCaptor.getValue())
                    .containsEntry("access_key_id", "accessKey")
                    .containsEntry("private_key", "secretKey");
            assertThat(mgmtRoles.getItems()).anyMatch(apiRole -> "TELEMETRYPUBLISHER".equals(apiRole.getName())
                    && "TELEMETRYPUBLISHER".equals(apiRole.getType()) && cmHostRef.equals(apiRole.getHostRef()));
        } else {
            underTest.setupTelemetryRole(stack, apiClient, cmHostRef, mgmtRoles, telemetry, SDX_STACK_CRN);

            verify(entitlementService, never()).useDataBusCNameEndpointEnabled(anyString());
            verify(dataBusEndpointProvider, never()).getDataBusEndpoint(anyString(), anyBoolean());
            verify(clouderaManagerApiFactory, never()).getClouderaManagerResourceApi(any(ApiClient.class));
            verify(clouderaManagerDatabusService, never()).getAltusCredential(any(StackDtoDelegate.class), anyString());
            verify(externalAccountService, never()).createExternalAccount(anyString(), anyString(), anyString(), anyMap(), any(ApiClient.class));
        }
    }

    @Test
    void testSetupTelemetryRoleWorkloadAnalyticsDisabled() throws ApiException {
        stack.setType(StackType.WORKLOAD);
        telemetry.setWorkloadAnalytics(null);

        underTest.setupTelemetryRole(stack, apiClient, null, null, telemetry, SDX_STACK_CRN);

        verify(entitlementService, never()).useDataBusCNameEndpointEnabled(anyString());
        verify(dataBusEndpointProvider, never()).getDataBusEndpoint(anyString(), anyBoolean());
        verify(clouderaManagerApiFactory, never()).getClouderaManagerResourceApi(any(ApiClient.class));
        verify(clouderaManagerDatabusService, never()).getAltusCredential(any(StackDtoDelegate.class), anyString());
        verify(externalAccountService, never()).createExternalAccount(anyString(), anyString(), anyString(), anyMap(), any(ApiClient.class));
    }

    private static Stream<Arguments> testUpdateTelemetryConfigsArguments() {
        return Stream.of(
                Arguments.of("crn:cdp:datalake:us-west-1:accountId:datalake:name", StackType.DATALAKE, false),
                Arguments.of("crn:cdp:datahub:us-west-1:accountId:cluster:name", StackType.WORKLOAD, false),
                Arguments.of("crn:cdp:datalake:us-west-1:accountId:datalake:name", StackType.DATALAKE, true),
                Arguments.of("crn:cdp:datahub:us-west-1:accountId:cluster:name", StackType.WORKLOAD, true)
        );
    }

    @ParameterizedTest
    @MethodSource("testUpdateTelemetryConfigsArguments")
    void testUpdateTelemetryConfigs(String resourceCrn, StackType stackType, boolean govCloud) throws ApiException {
        stack.setResourceCrn(resourceCrn);
        stack.setType(stackType);
        if (govCloud) {
            stack.setPlatformVariant("govCloud");
        }
        if (!govCloud) {
            when(clouderaManagerApiFactory.getMgmtRoleConfigGroupsResourceApi(apiClient)).thenReturn(mgmtRoleConfigGroupsResourceApi);
            CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
            if (StackType.WORKLOAD.equals(stackType)) {
                when(cmTemplateProcessorFactory.get("blueprint")).thenReturn(cmTemplateProcessor);
                when(meteringServiceFieldResolver.resolveServiceFeature(cmTemplateProcessor)).thenReturn("CLO");
            }

            underTest.updateTelemetryConfigs(stack, apiClient, telemetry, SDX_CONTEXT_NAME, SDX_STACK_CRN, proxyConfig);

            verify(mgmtRoleConfigGroupsResourceApi).updateConfig(eq("MGMT-TELEMETRYPUBLISHER-BASE"), eq("Set configs for Telemetry publisher by CB"),
                    apiConfigListCaptor.capture());
            ApiConfigList apiConfigList = apiConfigListCaptor.getValue();
            assertTrue(containsSafatyValveWithValue(apiConfigList, "databus.header.sdx.name", SDX_CONTEXT_NAME));
            assertTrue(containsSafatyValveWithValue(apiConfigList, "databus.header.sdx.id", "mystack"));
            assertTrue(containsSafatyValveWithValue(apiConfigList, "databus.header.environment.crn",
                    "crn:cdp:environments:us-west-1:accountId:environment:name"));
            assertTrue(containsSafatyValveWithValue(apiConfigList, "databus.header.cloudprovider.name", "AWS"));
            assertTrue(containsSafatyValveWithValue(apiConfigList, "databus.header.cloudprovider.region", "us-west-1"));
            assertTrue(containsSafatyValveWithValue(apiConfigList, "test.attribute.1", "value1"));
            assertTrue(containsSafatyValveWithValue(apiConfigList, "test.attribute.2", "value2"));
            assertTrue(containsSafatyValveWithValue(apiConfigList, "telemetry.upload.job.logs", "true"));
            if (StackType.DATALAKE.equals(stackType)) {
                assertTrue(containsSafatyValveWithValue(apiConfigList, "cluster.type", "DATALAKE_RUNTIME"));
                assertTrue(containsSafatyValveWithValue(apiConfigList, "extractor.metric.enabled", "false"));
                assertTrue(containsSafatyValveWithValue(apiConfigList, "extractor.event.enabled", "false"));
                assertTrue(containsSafatyValveWithValue(apiConfigList, "databus.header.datalake.crn", resourceCrn));
            } else if (StackType.WORKLOAD.equals(stackType)) {
                assertTrue(containsSafatyValveWithValue(apiConfigList, "cluster.type", "DATALAKE"));
                assertTrue(containsSafatyValveWithValue(apiConfigList, "extractor.hms.metadata.enabled", "true"));
                assertTrue(containsSafatyValveWithValue(apiConfigList, "databus.header.datahub.crn", resourceCrn));
                assertTrue(containsSafatyValveWithValue(apiConfigList, "databus.header.datahub.name", "mystack"));
                assertTrue(containsSafatyValveWithValue(apiConfigList, "databus.header.datahub.type", "CLO"));
                assertTrue(containsSafatyValveWithValue(apiConfigList, "databus.header.datalake.crn", "crn:cdp:datalake:us-west-1:accountId:datalake:name"));
            }
            assertTrue(containsConfigWithValue(apiConfigList, "telemetrypublisher_proxy_enabled", "true"));
            assertTrue(containsConfigWithValue(apiConfigList, "telemetrypublisher_proxy_server", "proxyHost"));
            assertTrue(containsConfigWithValue(apiConfigList, "telemetrypublisher_proxy_port", "1234"));
            assertTrue(containsConfigWithValue(apiConfigList, "telemetrypublisher_proxy_user", "user"));
            assertTrue(containsConfigWithValue(apiConfigList, "telemetrypublisher_proxy_password", "password"));
        } else {
            underTest.updateTelemetryConfigs(stack, apiClient, telemetry, SDX_CONTEXT_NAME, SDX_STACK_CRN, proxyConfig);

            verify(clouderaManagerApiFactory, never()).getMgmtRoleConfigGroupsResourceApi(any(ApiClient.class));
            verify(cmTemplateProcessorFactory, never()).get(anyString());
            verify(meteringServiceFieldResolver, never()).resolveServiceFeature(any(CmTemplateProcessor.class));
            verify(mgmtRoleConfigGroupsResourceApi, never()).updateConfig(anyString(), anyString(), any(ApiConfigList.class));
        }
    }

    @Test
    void testUpdateServiceMonitorConfigs() throws ApiException {
        stack.setResourceCrn("crn:cdp:datahub:us-west-1:accountId:cluster:name");
        given(mgmtRoleConfigGroupsResourceApi.readConfig(any(), anyString())).willReturn(new ApiConfigList()
                .addItemsItem(new ApiConfig().name("prometheus_metrics_endpoint_port"))
                .addItemsItem(new ApiConfig().name("prometheus_metrics_endpoint_username"))
                .addItemsItem(new ApiConfig().name("prometheus_metrics_endpoint_password"))
                .addItemsItem(new ApiConfig().name("prometheus_adapter_enabled")));
        given(monitoringConfiguration.getClouderaManagerExporter()).willReturn(cmMonitoringConfiguration);
        given(cmMonitoringConfiguration.getPort()).willReturn(EXPORTER_PORT);
        given(clouderaManagerApiFactory.getMgmtRoleConfigGroupsResourceApi(apiClient)).willReturn(mgmtRoleConfigGroupsResourceApi);

        underTest.updateServiceMonitorConfigs(stack, apiClient, telemetry);

        verify(mgmtRoleConfigGroupsResourceApi).readConfig("MGMT-SERVICEMONITOR-BASE", "FULL");
        verify(mgmtRoleConfigGroupsResourceApi).updateConfig(eq("MGMT-SERVICEMONITOR-BASE"),
                eq("Set service monitoring configs for CM metrics exporter by CB"), apiConfigListCaptor.capture());
        ApiConfigList apiConfigList = apiConfigListCaptor.getValue();
        assertTrue(containsConfigWithValue(apiConfigList, "prometheus_metrics_endpoint_username", "monitoringUser"));
        assertTrue(containsConfigWithValue(apiConfigList, "prometheus_metrics_endpoint_password", "monitoringPassword"));
        assertTrue(containsConfigWithValue(apiConfigList, "prometheus_metrics_endpoint_port", String.valueOf(EXPORTER_PORT)));
        assertTrue(containsConfigWithValue(apiConfigList, "prometheus_adapter_enabled", "true"));
    }

    private boolean containsConfigWithValue(ApiConfigList configList, String configKey, String configValue) {
        return configList.getItems().stream()
                .anyMatch(c -> configKey.equals(c.getName()) && configValue.equals(c.getValue()));
    }

    private boolean containsSafatyValveWithValue(ApiConfigList configList, String configKey, String configValue) {
        String safetyValve = configList.getItems()
                .stream()
                .filter(config -> "telemetrypublisher_safety_valve".equals(config.getName()))
                .map(ApiConfig::getValue)
                .findFirst()
                .orElse("");

        return Arrays.stream(safetyValve.split("\n"))
                .map(line -> line.split("="))
                .filter(parts -> parts.length == 2)
                .anyMatch(parts -> parts[0].equals(configKey) && parts[1].equals(configValue));
    }
}
