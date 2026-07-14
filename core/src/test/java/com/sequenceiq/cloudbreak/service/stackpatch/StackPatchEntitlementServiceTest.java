package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.ExistingStackPatcherConfig;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.StackPatchTypeConfig;

@ExtendWith(MockitoExtension.class)
class StackPatchEntitlementServiceTest {

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:uuid";

    @Mock
    private ExistingStackPatcherConfig existingStackPatcherConfig;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ExistingStackPatchService patchService;

    @InjectMocks
    private StackPatchEntitlementService underTest;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setResourceCrn(STACK_CRN);
        lenient().when(patchService.getStackPatchType()).thenReturn(StackPatchType.AWS_GP2_TO_GP3_MIGRATION);
    }

    @Test
    void shouldAllowWhenPatchConfigsIsNull() {
        when(existingStackPatcherConfig.getPatchConfigs()).thenReturn(null);

        assertTrue(underTest.isEntitled(stack, patchService.getStackPatchType()));
    }

    @Test
    void shouldAllowWhenNoEntitlementConfigured() {
        when(existingStackPatcherConfig.getPatchConfigs()).thenReturn(Map.of());

        assertTrue(underTest.isEntitled(stack, patchService.getStackPatchType()));
    }

    @Test
    void shouldAllowWhenPatchConfigPresentButEntitlementIsBlank() {
        StackPatchTypeConfig patchConfig = new StackPatchTypeConfig();
        when(existingStackPatcherConfig.getPatchConfigs()).thenReturn(Map.of(StackPatchType.AWS_GP2_TO_GP3_MIGRATION, patchConfig));

        assertTrue(underTest.isEntitled(stack, patchService.getStackPatchType()));
        verify(entitlementService, never()).isEntitledFor(anyString(), any());
    }

    @Test
    void shouldAllowWhenEntitlementConfiguredAndEnabled() {
        StackPatchTypeConfig patchConfig = new StackPatchTypeConfig();
        patchConfig.setEntitlement("GP2_TO_GP3_MIGRATION");
        when(existingStackPatcherConfig.getPatchConfigs()).thenReturn(Map.of(StackPatchType.AWS_GP2_TO_GP3_MIGRATION, patchConfig));
        when(entitlementService.isEntitledFor(eq("tenant"), eq(Entitlement.GP2_TO_GP3_MIGRATION))).thenReturn(true);

        assertTrue(underTest.isEntitled(stack, patchService.getStackPatchType()));
    }

    @Test
    void shouldDenyWhenEntitlementConfiguredAndDisabled() {
        StackPatchTypeConfig patchConfig = new StackPatchTypeConfig();
        patchConfig.setEntitlement("GP2_TO_GP3_MIGRATION");
        when(existingStackPatcherConfig.getPatchConfigs()).thenReturn(Map.of(StackPatchType.AWS_GP2_TO_GP3_MIGRATION, patchConfig));
        when(entitlementService.isEntitledFor(eq("tenant"), eq(Entitlement.GP2_TO_GP3_MIGRATION))).thenReturn(false);

        assertFalse(underTest.isEntitled(stack, patchService.getStackPatchType()));
    }

    @Test
    void shouldDenyWhenEntitlementConfiguredWithUnknownValue() {
        StackPatchTypeConfig patchConfig = new StackPatchTypeConfig();
        patchConfig.setEntitlement("NOT_A_REAL_ENTITLEMENT");
        when(existingStackPatcherConfig.getPatchConfigs()).thenReturn(Map.of(StackPatchType.AWS_GP2_TO_GP3_MIGRATION, patchConfig));

        assertFalse(underTest.isEntitled(stack, patchService.getStackPatchType()));
    }
}
