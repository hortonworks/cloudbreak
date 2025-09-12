package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.conf.LimitConfiguration;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.util.CodUtil;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class ClusterSizeUpgradeValidatorTest {

    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final int ROLLING_UPGRADE_NODE_COUNT_LIMIT = 20;

    private static final int OS_UPGRADE_NODE_COUNT_LIMIT = 200;

    @InjectMocks
    private ClusterSizeUpgradeValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private LimitConfiguration limitConfiguration;

    @Mock
    private StackDto stack;

    @BeforeEach
    void before() {
        ReflectionTestUtils.setField(underTest, "maxNumberOfInstancesForRollingUpgrade", ROLLING_UPGRADE_NODE_COUNT_LIMIT);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheRollingUpgradeIsNotEnabledAndVmReplacementIsNotEnabled() {
        underTest.validate(createRequest(false, false));
        verifyNoInteractions(entitlementService);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheRollingUpgradeIsEnabledAndSkipRollingUpgradeValidationIsEnabled() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(true);

        doAs(ACTOR, () -> underTest.validate(createRequest(true, false)));

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(any());
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenRollingUpgradeIsEnabledAndTheActualNodeCountIsAcceptable() throws IOException {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        mockStackTags(Map.of());
        mockAvailableInstances(ROLLING_UPGRADE_NODE_COUNT_LIMIT);
        doAs(ACTOR, () -> underTest.validate(createRequest(true, false)));
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenRollingUpgradeIsEnabledAndCodCluster() throws IOException {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        mockStackTags(Map.of(ClusterTemplateApplicationTag.SERVICE_TYPE.key(), CodUtil.OPERATIONAL_DB));
        mockAvailableInstances(21);
        doAs(ACTOR, () -> underTest.validate(createRequest(true, false)));
    }

    @Test
    void testValidateShouldThrowExceptionWhenRollingUpgradeIsEnabledAndTheActualNodeCountIsTooHigh() throws IOException {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        mockStackTags(Map.of());
        mockAvailableInstances(21);
        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> doAs(ACTOR, () -> underTest.validate(createRequest(true, false))));

        assertEquals("Rolling upgrade is not permitted because this cluster has 21 nodes but the maximum number of allowed nodes is 20.",
                exception.getMessage());
    }

    @Test
    void testValidateShouldThrowExceptionWhenVmReplacementIsEnabledAndTheActualNodeCountIsTooHigh() {
        when(limitConfiguration.getUpgradeNodeCountLimit(any())).thenReturn(OS_UPGRADE_NODE_COUNT_LIMIT);
        mockAvailableInstances(201);
        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> doAs(ACTOR, () -> underTest.validate(createRequest(false, true))));

        assertEquals("There are 201 nodes in the cluster. OS upgrade is supported up to 200 nodes. "
                        + "Please downscale the cluster below the limit and retry the upgrade.", exception.getMessage());
    }

    @Test
    void testValidateShouldThrowExceptionWhenVmReplacementIsEnabledAndNodeCountIsTooHighDespiteSkipValidationIsEnabled() {
        when(limitConfiguration.getUpgradeNodeCountLimit(any())).thenReturn(OS_UPGRADE_NODE_COUNT_LIMIT);
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(true);
        mockAvailableInstances(201);
        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> doAs(ACTOR, () -> underTest.validate(createRequest(true, true))));

        assertEquals("There are 201 nodes in the cluster. OS upgrade is supported up to 200 nodes. "
                + "Please downscale the cluster below the limit and retry the upgrade.", exception.getMessage());
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenVmReplacementIsEnabledAndTheActualNodeCountIsAcceptable() {
        when(limitConfiguration.getUpgradeNodeCountLimit(any())).thenReturn(OS_UPGRADE_NODE_COUNT_LIMIT);
        mockAvailableInstances(OS_UPGRADE_NODE_COUNT_LIMIT);
        doAs(ACTOR, () -> underTest.validate(createRequest(false, true)));
    }

    @Test
    void testIsClusterSizeLargerThanAllowedForRollingUpgradeShouldReturnTrueWhenLarger() {
        assertTrue(underTest.isClusterSizeLargerThanAllowedForRollingUpgrade(21));
    }

    @Test
    void testIsClusterSizeLargerThanAllowedForRollingUpgradeShouldReturnFalseWhenNotLarger() {
        assertFalse(underTest.isClusterSizeLargerThanAllowedForRollingUpgrade(20));
    }

    private void mockAvailableInstances(long numberOfInstances) {
        when(stack.getFullNodeCount()).thenReturn(numberOfInstances);
    }

    private ServiceUpgradeValidationRequest createRequest(boolean rollingUpgradeEnabled, boolean replaceVms) {
        return new ServiceUpgradeValidationRequest(stack, false, rollingUpgradeEnabled, null, replaceVms);
    }

    private void mockStackTags(Map<String, String> applicationTags) throws IOException {
        StackView stackView = mock(StackView.class);
        Json json = mock(Json.class);
        when(stackView.getTags()).thenReturn(json);
        when(stack.getStack()).thenReturn(stackView);
        StackTags stackTags = mock(StackTags.class);
        when(stackTags.getApplicationTags()).thenReturn(applicationTags);
        when(json.get(StackTags.class)).thenReturn(stackTags);
    }

}