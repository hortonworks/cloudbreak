package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.PSQL11;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.PYTHON312;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.PYTHON38;
import static com.sequenceiq.cloudbreak.service.upgrade.validation.service.HuePsycopg2UpgradeValidator.DETECT_BROKEN_PYTHON312_COMMAND;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.retry.RetryType;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.ServiceUpgradeValidationRequestTestUtils;

@ExtendWith(MockitoExtension.class)
class HuePsycopg2UpgradeValidatorTest {

    private static final String AFFECTED_TARGET_RUNTIME = "7.3.2.10000";

    // psql11 present, python3.12 NOT burnt into the image -> the at-risk profile that triggers the host probe.
    private static final Map<String, String> AT_RISK_PACKAGES = Map.of(PSQL11.getKey(), "11.22");

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private StackDto stack;

    @Mock
    private GatewayConfig primaryGatewayConfig;

    @InjectMocks
    private HuePsycopg2UpgradeValidator underTest;

    @ParameterizedTest
    @ValueSource(strings = {"7.3.2", "7.3.2-1.cdh7.3.2.p100.123", "7.3.2-1.cdh7.3.2.p9999.123", "7.3.1-1.cdh7.3.1.p20000.123"})
    void shouldSkipValidationWhenTargetCdhVersionIsBelowThreshold(String cdhParcelVersion) {
        ClusterUpgradeProperties clusterUpgradeProperties =
                ClusterUpgradePropertiesTestUtils.withTargetParcelAndCurrentPackages("7.3.2", cdhParcelVersion, AT_RISK_PACKAGES);

        underTest.validate(ServiceUpgradeValidationRequestTestUtils.of(stack, clusterUpgradeProperties));

        verifyNoInteractions(gatewayConfigService, hostOrchestrator);
    }

    @Test
    void shouldSkipValidationWhenTargetCdhParcelVersionIsUnparseable() {
        ClusterUpgradeProperties clusterUpgradeProperties =
                ClusterUpgradePropertiesTestUtils.withTargetParcelAndCurrentPackages("7.3.2", "not-a-cdh-version", AT_RISK_PACKAGES);

        underTest.validate(ServiceUpgradeValidationRequestTestUtils.of(stack, clusterUpgradeProperties));

        verifyNoInteractions(gatewayConfigService, hostOrchestrator);
    }

    @Test
    void shouldProbeHostsWhenTargetCdhVersionIsNotApplicable() throws CloudbreakOrchestratorFailedException {
        mockHostResults(Map.of("host1", ""));

        assertDoesNotThrow(() -> underTest.validate(atRiskRequest(AFFECTED_TARGET_RUNTIME)));

        verify(hostOrchestrator).runCommandOnAllHosts(eq(primaryGatewayConfig), eq(DETECT_BROKEN_PYTHON312_COMMAND),
                eq(RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES));
    }

    @Test
    void shouldProbeHostsWhenPackageManifestIsMissing() throws CloudbreakOrchestratorFailedException {
        mockHostResults(Map.of("host1", "", "host2", ""));

        assertDoesNotThrow(() -> underTest.validate(requestWithCurrentPackages(AFFECTED_TARGET_RUNTIME, Map.of())));
    }

    @Test
    void shouldSkipValidationWhenManifestIsPopulatedButPostgres11IsNotPresent() {
        underTest.validate(requestWithCurrentPackages(AFFECTED_TARGET_RUNTIME, Map.of(PYTHON38.getKey(), "3.8.13")));

        verifyNoInteractions(gatewayConfigService, hostOrchestrator);
    }

    @Test
    void shouldSkipValidationWhenPython312IsBurntIntoTheImage() {
        underTest.validate(requestWithCurrentPackages(AFFECTED_TARGET_RUNTIME, Map.of(PSQL11.getKey(), "11.22", PYTHON312.getKey(), "3.12.4")));

        verifyNoInteractions(gatewayConfigService, hostOrchestrator);
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.3.2.10000", "7.3.2.20000", "7.3.3", "7.4.0"})
    void shouldPassWhenNoHostHasBrokenPython312(String targetRuntimeVersion) throws CloudbreakOrchestratorFailedException {
        mockHostResults(Map.of("host1", "", "host2", ""));

        assertDoesNotThrow(() -> underTest.validate(atRiskRequest(targetRuntimeVersion)));
    }

    @Test
    void shouldResolvePatchVersionFromCdhParcelWhenRuntimeVersionLacksPatch() throws CloudbreakOrchestratorFailedException {
        mockHostResults(Map.of("host1", ""));
        ClusterUpgradeProperties clusterUpgradeProperties = ClusterUpgradePropertiesTestUtils.withTargetParcelAndCurrentPackages(
                "7.3.2", "7.3.2-1.cdh7.3.2.p20000.83281140", AT_RISK_PACKAGES);

        assertDoesNotThrow(() -> underTest.validate(ServiceUpgradeValidationRequestTestUtils.of(stack, clusterUpgradeProperties)));

        verify(hostOrchestrator).runCommandOnAllHosts(eq(primaryGatewayConfig), eq(DETECT_BROKEN_PYTHON312_COMMAND),
                eq(RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES));
    }

    @Test
    void shouldDenyWhenAHostHasBrokenPython312() throws CloudbreakOrchestratorFailedException {
        mockHostResults(Map.of("host1", "broken"));

        UpgradeValidationFailedException exception =
                assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(atRiskRequest(AFFECTED_TARGET_RUNTIME)));

        assertTrue(exception.getMessage().contains("Affected host(s): host1"), exception.getMessage());
    }

    @Test
    void shouldDenyAndNameOnlyTheOffendingHostWhenHostsAreMixed() throws CloudbreakOrchestratorFailedException {
        Map<String, String> resultByHost = new LinkedHashMap<>();
        resultByHost.put("healthyHost", "");
        resultByHost.put("brokenHost", "broken");
        mockHostResults(resultByHost);

        UpgradeValidationFailedException exception =
                assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(atRiskRequest(AFFECTED_TARGET_RUNTIME)));

        assertTrue(exception.getMessage().contains("brokenHost"), exception.getMessage());
        assertFalse(exception.getMessage().contains("healthyHost"), exception.getMessage());
    }

    @Test
    void shouldDenyWhenTheHostQueryFails() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(primaryGatewayConfig);
        when(hostOrchestrator.runCommandOnAllHosts(eq(primaryGatewayConfig), eq(DETECT_BROKEN_PYTHON312_COMMAND), eq(RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES)))
                .thenThrow(new CloudbreakOrchestratorFailedException("salt is unreachable"));

        UpgradeValidationFailedException exception =
                assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(atRiskRequest(AFFECTED_TARGET_RUNTIME)));

        assertTrue(exception.getMessage().contains("could not be verified"), exception.getMessage());
    }

    private void mockHostResults(Map<String, String> resultByHost) throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(primaryGatewayConfig);
        when(hostOrchestrator.runCommandOnAllHosts(eq(primaryGatewayConfig), eq(DETECT_BROKEN_PYTHON312_COMMAND), eq(RetryType.WITH_1_SEC_DELAY_MAX_3_TIMES)))
                .thenReturn(resultByHost);
    }

    private ServiceUpgradeValidationRequest atRiskRequest(String targetRuntimeVersion) {
        return requestWithCurrentPackages(targetRuntimeVersion, AT_RISK_PACKAGES);
    }

    private ServiceUpgradeValidationRequest requestWithCurrentPackages(String targetRuntimeVersion, Map<String, String> currentPackageVersions) {
        ClusterUpgradeProperties clusterUpgradeProperties =
                ClusterUpgradePropertiesTestUtils.withTargetRuntimeAndCurrentPackages(targetRuntimeVersion, currentPackageVersions);
        return ServiceUpgradeValidationRequestTestUtils.of(stack, clusterUpgradeProperties);
    }
}
