package com.sequenceiq.freeipa.service.telemetry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigService;
import com.sequenceiq.cloudbreak.telemetry.common.TelemetryCommonConfigView;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigService;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigService;
import com.sequenceiq.cloudbreak.telemetry.nodestatus.NodeStatusConfigService;
import com.sequenceiq.cloudbreak.telemetry.nodestatus.NodeStatusConfigView;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.VmLog;
import com.sequenceiq.freeipa.entity.Stack;
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
    private TelemetryCommonConfigService telemetryCommonConfigService;

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private CMLicenseParser cmLicenseParser;

    @Mock
    private FluentConfigService fluentConfigService;

    @Mock
    private NodeStatusConfigService nodeStatusConfigService;

    @Mock
    private MonitoringConfigService monitoringConfigService;

    @BeforeEach
    public void setUp() {
        underTest = new TelemetryConfigService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateTelemetryConfigs() throws Exception {
        // GIVEN
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder()
                .setClouderaManagerLicenseKey("myLicense")
                .build();
        JsonCMLicense license = new JsonCMLicense();
        license.setName("myname");
        license.setUuid("myuuid");
        Stack stack = createStack();
        given(stackService.getStackById(STACK_ID)).willReturn(stack);
        given(entitlementService.useDataBusCNameEndpointEnabled(anyString())).willReturn(false);
        given(dataBusEndpointProvider.getDataBusEndpoint(anyString(), anyBoolean())).willReturn("myendpoint");
        given(vmLogsService.getVmLogs()).willReturn(List.of(new VmLog()));
        given(telemetryCommonConfigService.createTelemetryCommonConfigs(any(), any(), any())).willReturn(new TelemetryCommonConfigView.Builder().build());
        given(cmLicenseParser.parseLicense(anyString())).willReturn(Optional.of(license));
        given(umsClient.getAccountDetails(anyString(), any())).willReturn(account);
        given(fluentConfigService.createFluentConfigs(any(), anyBoolean(), anyBoolean(), isNull(), any()))
                .willReturn(new FluentConfigView.Builder().build());
        given(nodeStatusConfigService.createNodeStatusConfig(isNull(), isNull(), anyBoolean())).willReturn(new NodeStatusConfigView.Builder().build());
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.createTelemetryConfigs(STACK_ID, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        // THEN
        verify(fluentConfigService, times(1)).createFluentConfigs(any(), anyBoolean(), anyBoolean(), isNull(), any());
        assertNotNull(result.get("telemetry").getProperties().get("cloudera-manager"));
        assertNotNull(result.get("telemetry").getProperties().get("telemetry"));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Telemetry telemetry = new Telemetry();
        telemetry.setDatabusEndpoint("myendpoint");
        stack.setTelemetry(telemetry);
        stack.setAccountId("accountId");
        stack.setResourceCrn("crn:cdp:freeipa:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:11111-2222");
        return stack;
    }

}
