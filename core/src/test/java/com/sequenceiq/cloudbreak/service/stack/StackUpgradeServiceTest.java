package com.sequenceiq.cloudbreak.service.stack;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class StackUpgradeServiceTest {

    private static final String ACCOUNT_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private static final String RESOURCE_CRN = CrnTestUtil.getDatalakeCrnBuilder()
            .setResource("aResource")
            .setAccountId(ACCOUNT_ID)
            .build()
            .toString();

    private static final Long STACK_ID = 3L;

    @InjectMocks
    private StackUpgradeService underTest;

    @Mock
    private EntitlementService entitlementService;

    private Stack stack;

    @BeforeEach
    public void setup() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setPlatformVariant("variant");
        stack.setResourceCrn(RESOURCE_CRN);
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationDisabled() {
        stack.setPlatformVariant("AWS");
        when(entitlementService.awsVariantMigrationEnable(ACCOUNT_ID)).thenReturn(false);
        String actual = underTest.calculateUpgradeVariant(stack, USER_CRN);
        Assertions.assertEquals("AWS", actual);
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationEnabledAndVariantIsAws() {
        stack.setPlatformVariant("AWS");
        when(entitlementService.awsVariantMigrationEnable(ACCOUNT_ID)).thenReturn(true);
        String actual = underTest.calculateUpgradeVariant(stack, USER_CRN);
        Assertions.assertEquals("AWS_NATIVE", actual);
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationEnabledWhenVariantIsNotAWS() {
        stack.setPlatformVariant("GCP");
        when(entitlementService.awsVariantMigrationEnable(ACCOUNT_ID)).thenReturn(true);
        String actual = underTest.calculateUpgradeVariant(stack, USER_CRN);
        Assertions.assertEquals("GCP", actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenOriginalVariantIsAwsAndTriggeredVariantIsAwsNativeAndMigrationEnabled() {
        stack.setPlatformVariant("AWS");
        when(entitlementService.awsVariantMigrationEnable(ACCOUNT_ID)).thenReturn(true);
        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS_NATIVE");
        Assertions.assertTrue(actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenOriginalVariantIsAwsAndTriggeredVariantIsAwsNativeAndMigrationDisabled() {
        stack.setPlatformVariant("AWS");
        when(entitlementService.awsVariantMigrationEnable(ACCOUNT_ID)).thenReturn(false);
        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS_NATIVE");
        Assertions.assertFalse(actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenOriginalVariantIsAwsAndTriggeredVariantIsAwsAndMigrationDisabled() {
        stack.setPlatformVariant("AWS");
        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS");
        Assertions.assertFalse(actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenOriginalVariantIsAwsNativeAndTriggeredVariantIsAwsNativeAndMigrationDisabled() {
        stack.setPlatformVariant("AWS_NATIVE");
        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS_NATIVE");
        Assertions.assertFalse(actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenOriginalVariantIsAwsNativeAndTriggeredVariantIsNullAndMigrationDisabled() {
        stack.setPlatformVariant("AWS_NATIVE");
        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, null);
        Assertions.assertFalse(actual);
    }
}
