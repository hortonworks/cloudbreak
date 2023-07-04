package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeOptions;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStackHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.upgrade.UpgradeService;
import com.sequenceiq.freeipa.service.upgrade.ccm.UpgradeCcmOrchestratorService;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmServiceTest {

    private static final long STACK_ID = 2L;

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENV_CRN = "envCrn";

    private static final String CIDR = "someCidr";

    @Mock
    private StackService stackService;

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private FreeIpaStackHealthDetailsService healthService;

    @Mock
    private ImageService imageService;

    @Mock
    private UpgradeCcmOrchestratorService upgradeCcmOrchestratorService;

    @Mock
    private UpgradeService upgradeService;

    @InjectMocks
    private UpgradeCcmService underTest;

    @BeforeEach
    void setUp() {
        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setEnvironmentCrn(ENV_CRN);
        lenient().when(stackService.getStackById(STACK_ID)).thenReturn(stack);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = "AVAILABLE", mode = EXCLUDE)
    void testHealthCheckerUnhealthy(Status healthStatus) {
        HealthDetailsFreeIpaResponse healthDetails = new HealthDetailsFreeIpaResponse();
        healthDetails.setStatus(healthStatus);
        when(healthService.getHealthDetails(ENV_CRN, ACCOUNT_ID)).thenReturn(healthDetails);
        assertThatThrownBy(() -> underTest.registerClusterProxyAndCheckHealth(STACK_ID))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessage("One or more FreeIPA instance is not available. Need to roll back CCM upgrade to previous version.");
    }

    @Test
    void testHealthCheckerHealthy() {
        HealthDetailsFreeIpaResponse healthDetails = new HealthDetailsFreeIpaResponse();
        healthDetails.setStatus(Status.AVAILABLE);
        when(healthService.getHealthDetails(ENV_CRN, ACCOUNT_ID)).thenReturn(healthDetails);
        underTest.registerClusterProxyAndCheckHealth(STACK_ID);
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = "CCM", mode = EXCLUDE)
    void ccmV2ConnectivityIsNotCheckedButForCcmV1(Tunnel tunnel) {
        ReflectionTestUtils.setField(underTest, "ccmV2ServersCidr", CIDR);
        setupFreeIpaUpgradeCheck();
        underTest.checkPrerequsities(STACK_ID, tunnel);
        verify(upgradeCcmOrchestratorService, never()).checkCcmV2Connectivity(any(), any());
    }

    @Test
    void ccmV2ConnectivityIsNotCheckedForCcmV1IfNoCidr() {
        setupFreeIpaUpgradeCheck();
        underTest.checkPrerequsities(STACK_ID, Tunnel.CCM);
        verify(upgradeCcmOrchestratorService, never()).checkCcmV2Connectivity(any(), any());
    }

    @Test
    void ccmV2ConnectivityIsCheckedForCcmV1NullResult() {
        ReflectionTestUtils.setField(underTest, "ccmV2ServersCidr", CIDR);
        setupFreeIpaUpgradeCheck();
        underTest.checkPrerequsities(STACK_ID, Tunnel.CCM);
        verify(upgradeCcmOrchestratorService).checkCcmV2Connectivity(STACK_ID, CIDR);
    }

    @Test
    void ccmV2ConnectivityIsCheckedForCcmV1NoFailure() {
        ReflectionTestUtils.setField(underTest, "ccmV2ServersCidr", CIDR);
        setupFreeIpaUpgradeCheck();
        Map<String, String> resultMap = Map.of(
                "server1", """
                        {"result": "%s", "reason": "all ok"}
                        """.formatted(CheckResult.SUCCESSFUL),
                "server2", """
                        {"result": "%s", "reason": "all ok"}
                        """.formatted(CheckResult.SUCCESSFUL));
        when(upgradeCcmOrchestratorService.checkCcmV2Connectivity(STACK_ID, CIDR)).thenReturn(resultMap);
        underTest.checkPrerequsities(STACK_ID, Tunnel.CCM);
        verify(upgradeCcmOrchestratorService).checkCcmV2Connectivity(STACK_ID, CIDR);
    }

    @Test
    void ccmV2ConnectivityIsCheckedForCcmV1WithFailure() {
        ReflectionTestUtils.setField(underTest, "ccmV2ServersCidr", CIDR);
        setupFreeIpaUpgradeCheck();
        Map<String, String> resultMap = Map.of(
                "server1", """
                        {"result": "%s", "reason": "all ok"}
                        """.formatted(CheckResult.SUCCESSFUL),
                "server2", """
                        {"result": "%s", "reason": "cmon..."}
                        """.formatted(CheckResult.FAILED));
        when(upgradeCcmOrchestratorService.checkCcmV2Connectivity(STACK_ID, CIDR)).thenReturn(resultMap);
        assertThatThrownBy(() -> underTest.checkPrerequsities(STACK_ID, Tunnel.CCM))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessageContaining("cmon");
        verify(upgradeCcmOrchestratorService).checkCcmV2Connectivity(STACK_ID, CIDR);
    }

    private void setupFreeIpaUpgradeCheck() {
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setImageCatalogName("catalog");
        when(imageService.getByStackId(STACK_ID)).thenReturn(imageEntity);
        FreeIpaUpgradeOptions upgradeOptions = new FreeIpaUpgradeOptions();
        upgradeOptions.setImages(new ArrayList<>());
        when(upgradeService.collectUpgradeOptions(ACCOUNT_ID, ENV_CRN, "catalog", false)).thenReturn(upgradeOptions);
    }

}
