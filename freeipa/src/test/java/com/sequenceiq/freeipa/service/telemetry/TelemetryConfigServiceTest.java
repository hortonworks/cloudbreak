package com.sequenceiq.freeipa.service.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.CMLicenseParser;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.cloudbreak.telemetry.orchestrator.TelemetrySaltPillarDecorator;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class TelemetryConfigServiceTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private TelemetryConfigService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Mock
    private VmLogsService vmLogsService;

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private CMLicenseParser cmLicenseParser;

    @Mock
    private TelemetrySaltPillarDecorator telemetrySaltPillarDecorator;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @BeforeEach
    public void setUp() {
        underTest = new TelemetryConfigService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateTelemetryConfigs() {
        // GIVEN
        Stack stack = createStack();
        given(stackService.getStackById(STACK_ID)).willReturn(stack);
        given(telemetrySaltPillarDecorator.generatePillarConfigMap(any(Stack.class))).willReturn(new HashMap<>());
        // WHEN
        underTest.createTelemetryConfigs(STACK_ID, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        // THEN
        verify(stackService, times(1)).getStackById(anyLong());
        verify(telemetrySaltPillarDecorator, times(1)).generatePillarConfigMap(any(Stack.class));
    }

    @Test
    public void testCreateTelemetryContext() throws IOException {
        // GIVEN
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder()
                .setClouderaManagerLicenseKey("myLicense")
                .build();
        JsonCMLicense license = new JsonCMLicense();
        license.setName("myname");
        license.setUuid("myuuid");
        DataBusCredential dataBusCredential = new DataBusCredential();
        dataBusCredential.setAccessKey("accessKey");
        dataBusCredential.setPrivateKey("privateKey");
        MonitoringCredential monitoringCredential = new MonitoringCredential();
        monitoringCredential.setAccessKey("accessKey");
        monitoringCredential.setPrivateKey("privateKey");
        given(cmLicenseParser.parseLicense(anyString())).willReturn(Optional.of(license));
        given(umsClient.getAccountDetails(anyString(), any())).willReturn(account);
        given(dataBusEndpointProvider.getDataBusEndpoint(anyString(), anyBoolean())).willReturn("myendpoint");
        given(entitlementService.useDataBusCNameEndpointEnabled(anyString())).willReturn(false);
        given(entitlementService.isFreeIpaDatabusEndpointValidationEnabled(anyString())).willReturn(true);
        given(vmLogsService.getVmLogs()).willReturn(new ArrayList<>());
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        given(altusMachineUserService.getOrCreateDataBusCredentialIfNeeded(any(Stack.class), any(CdpAccessKeyType.class))).willReturn(dataBusCredential);
        given(altusMachineUserService.getOrCreateMonitoringCredentialIfNeeded(any(Stack.class), any(CdpAccessKeyType.class)))
                .willReturn(Optional.of(monitoringCredential));
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack(telemetry(true, true, true)));
        // THEN
        assertEquals(FluentClusterType.FREEIPA, result.getClusterType());
        assertEquals("myuuid", result.getPaywallConfigs().get("paywall_username"));
        assertTrue(result.getLogShipperContext().isEnabled());
        assertTrue(result.getDatabusContext().isEnabled());
        assertTrue(result.getMonitoringContext().isEnabled());
        assertFalse(result.getMeteringContext().isEnabled());
        verify(altusMachineUserService, times(1)).getOrCreateDataBusCredentialIfNeeded(any(Stack.class), any(CdpAccessKeyType.class));
        verify(altusMachineUserService, times(1)).getOrCreateMonitoringCredentialIfNeeded(any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    public void testCreateTelemetryContextWithMonitoringOnly() throws IOException {
        // GIVEN
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder()
                .setClouderaManagerLicenseKey("myLicense")
                .build();
        MonitoringCredential monitoringCredential = new MonitoringCredential();
        monitoringCredential.setAccessKey("accessKey");
        monitoringCredential.setPrivateKey("privateKey");
        given(umsClient.getAccountDetails(anyString(), any())).willReturn(account);
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        given(altusMachineUserService.getOrCreateMonitoringCredentialIfNeeded(any(Stack.class), any(CdpAccessKeyType.class)))
                .willReturn(Optional.of(monitoringCredential));
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack(telemetry(false, true, false)));
        // THEN
        assertFalse(result.getLogShipperContext().isEnabled());
        assertFalse(result.getDatabusContext().isEnabled());
        assertTrue(result.getMonitoringContext().isEnabled());
        verify(altusMachineUserService, times(1)).getOrCreateMonitoringCredentialIfNeeded(any(Stack.class), any(CdpAccessKeyType.class));
    }

    @Test
    public void testCreateTelemetryContextWithoutLoggingType() throws IOException {
        // GIVEN
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder()
                .setClouderaManagerLicenseKey("myLicense")
                .build();
        given(umsClient.getAccountDetails(anyString(), any())).willReturn(account);
        Telemetry telemetry = telemetry(true, false, false);
        telemetry.getLogging().setS3(null);
        // WHEN
        TelemetryContext result = underTest.createTelemetryContext(createStack(telemetry));
        // THEN
        assertFalse(result.getLogShipperContext().isEnabled());
    }

    private Stack createStack() {
        return createStack(null);
    }

    private Stack createStack(Telemetry telemetry) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setTelemetry(telemetry);
        stack.setAccountId("accountId");
        stack.setResourceCrn("crn:cdp:freeipa:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:11111-2222");
        return stack;
    }

    private Telemetry telemetry(boolean cloudLogging, boolean monitoringEnabled, boolean clusterDeploymentLogs) {
        Telemetry telemetry = new Telemetry();
        telemetry.setDatabusEndpoint("myendpoint");
        Features features = new Features();
        features.addClusterLogsCollection(clusterDeploymentLogs);
        if (monitoringEnabled) {
            Monitoring monitoring = new Monitoring();
            monitoring.setRemoteWriteUrl("remoteWriteUrl");
            telemetry.setMonitoring(monitoring);
        }
        if (cloudLogging) {
            Logging logging = new Logging();
            logging.setS3(new S3CloudStorageV1Parameters());
            telemetry.setLogging(logging);
        }
        telemetry.setFeatures(features);
        return telemetry;
    }

}
