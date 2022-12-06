package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class TelemetryDecoratorTest {

    private TelemetryDecorator underTest;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @Mock
    private VmLogsService vmLogsService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private Telemetry telemetry;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private Logging logging;

    @Mock
    private Monitoring monitoring;

    @Before
    public void setUp() {
        initMocks();
        underTest = new TelemetryDecorator(altusMachineUserService, vmLogsService, entitlementService,
                dataBusEndpointProvider, monitoringConfiguration, componentConfigProviderService, clusterComponentConfigProvider, "1.0.0");
    }

    @Test
    public void testCreateTelemetryContext() {
        // GIVEN
        given(telemetry.isAnyDataBusBasedFeatureEnablred()).willReturn(true);
        given(telemetry.isMeteringFeatureEnabled()).willReturn(true);
        given(telemetry.isComputeMonitoringEnabled()).willReturn(true);
        given(telemetry.getLogging()).willReturn(logging);
        given(telemetry.isCloudStorageLoggingEnabled()).willReturn(true);
        given(telemetry.isClusterLogsCollectionEnabled()).willReturn(true);
        given(logging.getS3()).willReturn(new S3CloudStorageV1Parameters());
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        given(telemetry.getMonitoring()).willReturn(monitoring);
        given(monitoring.getRemoteWriteUrl()).willReturn("https://remotewrite:80/api/v1/write");
        given(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).willReturn("2.65.0-b62");
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertEquals(FluentClusterType.DATAHUB, result.getClusterType());
        assertTrue(result.getDatabusContext().isEnabled());
        assertTrue(result.getLogShipperContext().isEnabled());
        assertTrue(result.getMeteringContext().isEnabled());
        assertTrue(result.getMonitoringContext().isEnabled());
        assertTrue(result.getNodeStatusContext().isSaltPingEnabled());
        verify(altusMachineUserService, times(1)).storeDataBusCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
        verify(altusMachineUserService, times(1)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    public void testCreateTelemetryContextWithDatalakeMeteringDisabled() {
        // GIVEN
        given(telemetry.isAnyDataBusBasedFeatureEnablred()).willReturn(true);
        given(telemetry.isMeteringFeatureEnabled()).willReturn(true);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack(StackType.DATALAKE));
        // THEN
        assertEquals(FluentClusterType.DATALAKE, result.getClusterType());
        assertTrue(result.getDatabusContext().isEnabled());
        assertFalse(result.getMeteringContext().isEnabled());
    }

    @Test
    public void testCreateTelemetryContextWithMonitoringOnly() {
        // GIVEN
        given(telemetry.isAnyDataBusBasedFeatureEnablred()).willReturn(false);
        given(telemetry.isComputeMonitoringEnabled()).willReturn(true);
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        given(telemetry.getMonitoring()).willReturn(monitoring);
        given(monitoring.getRemoteWriteUrl()).willReturn("https://remotewrite:80/api/v1/write");
        given(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).willReturn("2.65.0-b62");
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertFalse(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertFalse(result.getMeteringContext().isEnabled());
        assertTrue(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(1)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    public void testCreateTelemetryContextWithMonitoringOnlyAndMajorVersionChanged() {
        // GIVEN
        given(telemetry.isAnyDataBusBasedFeatureEnablred()).willReturn(false);
        given(telemetry.isComputeMonitoringEnabled()).willReturn(true);
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        given(telemetry.getMonitoring()).willReturn(monitoring);
        given(monitoring.getRemoteWriteUrl()).willReturn("https://remotewrite:80/api/v1/write");
        given(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).willReturn("3.65.0-b620000");
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertFalse(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertFalse(result.getMeteringContext().isEnabled());
        assertTrue(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(1)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    public void testMonitoringIsTurnedOffIfEntitlementIsGrantedButSaltVersionIsTooOld() {
        // GIVEN
        given(telemetry.isAnyDataBusBasedFeatureEnablred()).willReturn(false);
        given(telemetry.isComputeMonitoringEnabled()).willReturn(false);
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        given(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).willReturn("2.65.0-b61");
        telemetry.setMonitoring(monitoring);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertFalse(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertFalse(result.getMeteringContext().isEnabled());
        assertFalse(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(0)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    public void testMonitoringIsTurnedOffIfEntitlementIsGrantedButSaltVersionIsVeryOld() {
        // GIVEN
        given(telemetry.isAnyDataBusBasedFeatureEnablred()).willReturn(false);
        given(telemetry.isComputeMonitoringEnabled()).willReturn(false);
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        given(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).willReturn("2.21.0-b10000");
        telemetry.setMonitoring(monitoring);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertFalse(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertFalse(result.getMeteringContext().isEnabled());
        assertFalse(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(0)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    public void testMonitoringIsTurnedOffIfEntitlementIsGrantedButSaltVersionIsUnkown() {
        // GIVEN
        given(telemetry.isAnyDataBusBasedFeatureEnablred()).willReturn(false);
        given(telemetry.isComputeMonitoringEnabled()).willReturn(false);
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        given(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).willReturn(null);
        telemetry.setMonitoring(monitoring);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertFalse(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertFalse(result.getMeteringContext().isEnabled());
        assertFalse(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(0)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    public void testCreateTelemetryContextLoggingWithoutCloudStorage() {
        // GIVEN
        given(telemetry.getLogging()).willReturn(logging);
        given(telemetry.isCloudStorageLoggingEnabled()).willReturn(true);
        given(telemetry.isClusterLogsCollectionEnabled()).willReturn(false);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertFalse(result.getLogShipperContext().isEnabled());
    }

    @Test
    public void testCreateTelemetryContextWithDefaults() {
        // GIVEN
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertEquals(FluentClusterType.DATAHUB, result.getClusterType());
    }

    private void initMocks() {
        MockitoAnnotations.openMocks(this);
        AltusCredential altusCredential = new AltusCredential("myAccessKey", "mySecretKey".toCharArray());
        DataBusCredential dataBusCredential = new DataBusCredential();
        dataBusCredential.setAccessKey("myAccessKey");
        dataBusCredential.setPrivateKey("mySecretKey");
        MonitoringCredential monitoringCredential = new MonitoringCredential();
        monitoringCredential.setAccessKey("myAccessKey");
        monitoringCredential.setPrivateKey("mySecretKey");
        given(componentConfigProviderService.getTelemetry(anyLong())).willReturn(telemetry);
        given(telemetry.getDatabusEndpoint()).willReturn("https://dbus-dev.com");
        given(altusMachineUserService.isAnyDataBusBasedFeatureSupported(any(Telemetry.class))).willReturn(true);
        given(altusMachineUserService.isAnyMonitoringFeatureSupported(any(Telemetry.class))).willReturn(true);
        given(altusMachineUserService.storeDataBusCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class)))
                .willReturn(dataBusCredential);
        given(altusMachineUserService.generateDatabusMachineUserForFluent(any(Stack.class), any(Telemetry.class), any(CdpAccessKeyType.class)))
                .willReturn(Optional.of(altusCredential));
        given(altusMachineUserService.generateMonitoringMachineUser(any(Stack.class), any(Telemetry.class), any(CdpAccessKeyType.class)))
                .willReturn(Optional.of(altusCredential));
        given(altusMachineUserService.storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class)))
                .willReturn(monitoringCredential);
        given(entitlementService.useDataBusCNameEndpointEnabled(anyString())).willReturn(false);
        given(entitlementService.isDatahubDatabusEndpointValidationEnabled(anyString())).willReturn(true);
        given(entitlementService.nodestatusSaltPingEnabled(anyString())).willReturn(true);
        given(dataBusEndpointProvider.getDataBusEndpoint(anyString(), anyBoolean())).willReturn("https://dbus-dev.com");
        given(dataBusEndpointProvider.getDatabusS3Endpoint(anyString())).willReturn("https://cloudera-dbus-dev.amazonaws.com");
        given(vmLogsService.getVmLogs()).willReturn(new ArrayList<>());
        given(altusMachineUserService.getCdpAccessKeyType(any())).willReturn(CdpAccessKeyType.ECDSA);
    }

    private StackDto createStack() {
        return createStack(StackType.WORKLOAD);
    }

    private StackDto createStack(StackType stackType) {
        StackDto stackDto = spy(StackDto.class);
        Stack stack = new Stack();
        stack.setName("my-stack-name");
        stack.setType(stackType);
        stack.setCloudPlatform("AWS");
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        cluster.setName("cl1");
        cluster.setCloudbreakClusterManagerMonitoringUser("myUsr");
        cluster.setCloudbreakClusterManagerMonitoringPassword("myPass");
        cluster.setCdpNodeStatusMonitorUser("nodeUser");
        cluster.setCdpNodeStatusMonitorPassword("nodePassword");
        stack.setCluster(cluster);
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        User creator = new User();
        creator.setUserCrn("crn:cdp:iam:us-west-1:accountId:user:name");
        stack.setCreator(creator);
        stack.setResourceCrn("crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getCluster()).thenReturn(cluster);
        return stackDto;
    }
}