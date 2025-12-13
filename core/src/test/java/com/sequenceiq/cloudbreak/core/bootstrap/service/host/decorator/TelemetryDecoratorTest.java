package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider.CipherSuitesLimitType.BLACKBOX_EXPORTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.service.encryptionprofile.EncryptionProfileService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryFeatureService;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringUrlResolver;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class TelemetryDecoratorTest {

    private TelemetryDecorator underTest;

    @Mock
    private MonitoringUrlResolver monitoringUrlResolver;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @Mock
    private EncryptionProfileProvider encryptionProfileProvider;

    @Mock
    private VmLogsService vmLogsService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Spy
    private Telemetry telemetry;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private Logging logging;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @Mock
    private TelemetryFeatureService telemetryFeatureService;

    @Spy
    private Monitoring monitoring;

    @BeforeEach
    void setUp() throws CloudbreakImageNotFoundException {
        initMocks();
        underTest = new TelemetryDecorator(
                altusMachineUserService,
                vmLogsService,
                entitlementService,
                dataBusEndpointProvider,
                monitoringUrlResolver,
                componentConfigProviderService,
                clusterComponentConfigProvider,
                encryptionProfileProvider,
                "1.0.0",
                environmentService,
                encryptionProfileService,
                telemetryFeatureService);
    }

    @Test
    void testCreateTelemetryContext() {
        // GIVEN
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(true);
        when(telemetry.getLogging()).thenReturn(logging);
        when(telemetry.isCloudStorageLoggingEnabled()).thenReturn(true);
        when(logging.getS3()).thenReturn(new S3CloudStorageV1Parameters());
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(true);
        when(telemetry.getMonitoring()).thenReturn(monitoring);
        when(monitoring.getRemoteWriteUrl()).thenReturn("https://remotewrite:80/api/v1/write");
        when(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).thenReturn("2.65.0-b62");
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertEquals(FluentClusterType.DATAHUB, result.getClusterType());
        assertTrue(result.getDatabusContext().isEnabled());
        assertTrue(result.getLogShipperContext().isEnabled());
        assertTrue(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(1)).storeDataBusCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
        verify(altusMachineUserService, times(1)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    void testCreateTelemetryContextWithDatalakeForcesCreateOfDatabusCredential() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack(StackType.DATALAKE));
        // THEN
        assertEquals(FluentClusterType.DATALAKE, result.getClusterType());
        assertTrue(result.getDatabusContext().isEnabled());
        verify(altusMachineUserService, times(1)).generateDatabusMachineUserForFluent(any(), any(), any());
    }

    @Test
    void testCreateTelemetryContextWithDatahubDoesntForceCreateOfDatabusCredential() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack(StackType.WORKLOAD));
        // THEN
        assertEquals(FluentClusterType.DATAHUB, result.getClusterType());
        assertTrue(result.getDatabusContext().isEnabled());
        verify(altusMachineUserService, times(1)).generateDatabusMachineUserForFluent(any(), any(), any());
    }

    @Test
    void testCreateTelemetryContextWithMonitoringOnly() {
        // GIVEN
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(true);
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(true);
        when(telemetry.getMonitoring()).thenReturn(monitoring);
        when(monitoring.getRemoteWriteUrl()).thenReturn("https://remotewrite:80/api/v1/write");
        when(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).thenReturn("2.65.0-b62");
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertTrue(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertTrue(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(1)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    void testMonitoringIsTurnedOffIfEntitlementIsNotGranted() {
        // GIVEN
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(true);
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(false);
        telemetry.setMonitoring(monitoring);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertTrue(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertFalse(result.getMonitoringContext().isEnabled());
        assertNull(telemetry.getMonitoring().getRemoteWriteUrl());
        verify(altusMachineUserService, never()).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    void testMonitoringIsTurnedOnIfEntitlementIsGranted() {
        // GIVEN
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        EncryptionProfileResponse encryptionProfileResponse = new EncryptionProfileResponse();
        encryptionProfileResponse.setCipherSuites(
            Map.of(
                "TLSv1.2", List.of("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"),
                "TLSv1.3", List.of("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384")
            )
        );

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(encryptionProfileProvider.getTlsCipherSuitesIanaList(anyMap(), eq(BLACKBOX_EXPORTER)))
                .thenReturn(List.of("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"));
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(false);
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(true);
        when(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).thenReturn("2.66.0-b100");
        telemetry.setMonitoring(monitoring);
        when(monitoringUrlResolver.resolve(anyString(), anyBoolean())).thenReturn("http://nope/receive");
        when(monitoringUrlResolver.resolve(anyString(), anyBoolean())).thenReturn("http://nope/receive");
        when(encryptionProfileService.getEncryptionProfileByCrnOrDefault(any(), any()))
                .thenReturn(encryptionProfileResponse);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertTrue(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertTrue(result.getMonitoringContext().isEnabled());
        assertNotNull(telemetry.getMonitoring().getRemoteWriteUrl());
        assertTrue(result.getTlsCipherSuites().contains("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"));
        verify(altusMachineUserService, times(1)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    void testCreateTelemetryContextWithMonitoringOnlyAndMajorVersionChanged() {
        // GIVEN
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(true);
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(true);
        when(telemetry.getMonitoring()).thenReturn(monitoring);
        when(monitoring.getRemoteWriteUrl()).thenReturn("https://remotewrite:80/api/v1/write");
        when(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).thenReturn("3.65.0-b620000");
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertTrue(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertTrue(result.getMonitoringContext().isEnabled());
        assertNotNull(telemetry.getMonitoring().getRemoteWriteUrl());
        verify(altusMachineUserService, times(1)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    void testMonitoringIsTurnedOffIfEntitlementIsGrantedButSaltVersionIsTooOld() {
        // GIVEN
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(false);
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(true);
        when(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).thenReturn("2.65.0-b61");
        telemetry.setMonitoring(monitoring);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertTrue(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertFalse(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(0)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    void testMonitoringIsTurnedOffIfEntitlementIsGrantedButSaltVersionIsVeryOld() {
        // GIVEN
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(false);
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(true);
        when(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).thenReturn("2.21.0-b10000");
        telemetry.setMonitoring(monitoring);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertTrue(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertFalse(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(0)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    void testMonitoringIsTurnedOffIfEntitlementIsGrantedButSaltVersionIsUnkown() {
        // GIVEN
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(telemetry.isComputeMonitoringEnabled()).thenReturn(false);
        when(entitlementService.isComputeMonitoringEnabled(anyString())).thenReturn(true);
        when(clusterComponentConfigProvider.getSaltStateComponentCbVersion(2L)).thenReturn(null);
        telemetry.setMonitoring(monitoring);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertTrue(result.getDatabusContext().isEnabled());
        assertFalse(result.getLogShipperContext().isEnabled());
        assertFalse(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(0)).storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    void testCreateTelemetryContextLoggingWithoutCloudStorage() {
        // GIVEN
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        when(telemetry.getLogging()).thenReturn(logging);
        when(telemetry.isCloudStorageLoggingEnabled()).thenReturn(true);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertFalse(result.getLogShipperContext().isEnabled());
    }

    @Test
    void testCreateTelemetryContextWithDefaults() {
        // GIVEN
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(environmentService.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack());
        // THEN
        assertEquals(FluentClusterType.DATAHUB, result.getClusterType());
    }

    private void initMocks() throws CloudbreakImageNotFoundException {
        MockitoAnnotations.openMocks(this);
        AltusCredential altusCredential = new AltusCredential("myAccessKey", "mySecretKey".toCharArray());
        DataBusCredential dataBusCredential = new DataBusCredential();
        Image image = mock(Image.class);
        dataBusCredential.setAccessKey("myAccessKey");
        dataBusCredential.setPrivateKey("mySecretKey");
        MonitoringCredential monitoringCredential = new MonitoringCredential();
        monitoringCredential.setAccessKey("myAccessKey");
        monitoringCredential.setPrivateKey("mySecretKey");
        when(componentConfigProviderService.getTelemetry(anyLong())).thenReturn(telemetry);
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        when(telemetry.getDatabusEndpoint()).thenReturn("https://dbus-dev.com");
        lenient().when(altusMachineUserService.isAnyMonitoringFeatureSupported(any(Telemetry.class))).thenReturn(true);
        when(altusMachineUserService.storeDataBusCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class))).thenReturn(dataBusCredential);
        when(altusMachineUserService.generateDatabusMachineUserForFluent(any(Stack.class), any(Telemetry.class), any(CdpAccessKeyType.class)))
                .thenReturn(Optional.of(altusCredential));
        lenient().when(altusMachineUserService.generateMonitoringMachineUser(any(Stack.class), any(Telemetry.class), any(CdpAccessKeyType.class)))
                .thenReturn(Optional.of(altusCredential));
        lenient().when(altusMachineUserService.storeMonitoringCredential(any(Optional.class), any(Stack.class), any(CdpAccessKeyType.class)))
                .thenReturn(monitoringCredential);
        when(entitlementService.useDataBusCNameEndpointEnabled(anyString())).thenReturn(false);
        lenient().when(entitlementService.isDatahubDatabusEndpointValidationEnabled(anyString())).thenReturn(true);
        when(dataBusEndpointProvider.getDataBusEndpoint(anyString(), anyBoolean())).thenReturn("https://dbus-dev.com");
        when(dataBusEndpointProvider.getDatabusS3Endpoint(anyString(), anyString())).thenReturn("https://cloudera-dbus-dev.amazonaws.com");
        when(vmLogsService.getVmLogs()).thenReturn(new ArrayList<>());
        when(altusMachineUserService.getCdpAccessKeyType(any())).thenReturn(CdpAccessKeyType.ECDSA);
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
        cluster.setCdpNodeStatusMonitorPassword("nodePassword");
        stack.setCluster(cluster);
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        User creator = new User();
        creator.setUserCrn("crn:cdp:iam:us-west-1:accountId:user:name");
        stack.setCreator(creator);
        stack.setResourceCrn("crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        stack.setEnvironmentCrn("crn:cdp:cloudbreak:us-west-1:someone:environment:12345");
        stack.setRegion("region");
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getCluster()).thenReturn(cluster);
        return stackDto;
    }
}