package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.util.TestConstants.DISABLE_VARIANT_CHANGE;
import static com.sequenceiq.cloudbreak.util.TestConstants.DO_NOT_KEEP_VARIANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
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
    @MockitoSettings(strictness = Strictness.LENIENT)
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
        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(false);
        String actual = underTest.calculateUpgradeVariant(stack, USER_CRN, DO_NOT_KEEP_VARIANT);
        assertEquals("AWS", actual);
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationEnabledAndVariantIsAws() {
        stack.setPlatformVariant("AWS");
        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        String actual = underTest.calculateUpgradeVariant(stack, USER_CRN, DO_NOT_KEEP_VARIANT);
        assertEquals("AWS_NATIVE", actual);
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationEnabledAndVariantIsAwsButVariantChangeIsUnWelcomed() {
        stack.setPlatformVariant("AWS");
        String actual = underTest.calculateUpgradeVariant(stack, USER_CRN, DISABLE_VARIANT_CHANGE);
        assertEquals("AWS", actual);
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationEnabledWhenVariantIsNotAWS() {
        stack.setPlatformVariant("GCP");
        String actual = underTest.calculateUpgradeVariant(stack, USER_CRN, DO_NOT_KEEP_VARIANT);
        assertEquals("GCP", actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenOriginalVariantIsAwsAndTriggeredVariantIsAwsNativeAndMigrationEnabled() {
        stack.setPlatformVariant("AWS");
        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(true);
        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS_NATIVE");
        assertTrue(actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenOriginalVariantIsAwsAndTriggeredVariantIsAwsNativeAndMigrationDisabled() {
        stack.setPlatformVariant("AWS");
        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(false);
        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS_NATIVE");
        assertFalse(actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenOriginalVariantIsAwsAndTriggeredVariantIsAwsAndMigrationDisabled() {
        stack.setPlatformVariant("AWS");
        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS");
        assertFalse(actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenOriginalVariantIsAwsNativeAndTriggeredVariantIsAwsNativeAndMigrationDisabled() {
        stack.setPlatformVariant("AWS_NATIVE");
        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, "AWS_NATIVE");
        assertFalse(actual);
    }

    @Test
    public void testAwsVariantMigrationIsFeasibleWhenOriginalVariantIsAwsNativeAndTriggeredVariantIsNullAndMigrationDisabled() {
        stack.setPlatformVariant("AWS_NATIVE");
        boolean actual = underTest.awsVariantMigrationIsFeasible(stack, null);
        assertFalse(actual);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testAllNodesSelectedForRepairWhenTheFqdnsToRepairIsEmpty(Set<String> fqdnsToRepair) {
        StackDto stackDto = mock(StackDto.class);

        boolean actual = underTest.allNodesSelectedForRepair(stackDto, fqdnsToRepair);

        assertFalse(actual);
    }

    @Test
    public void testAllNodesSelectedForRepairWhenTheFqdnsToRepairNotContainsAllTheFqdnsOfTheStack() {
        StackDto stackDto = mock(StackDto.class);
        List<InstanceMetadataView> instanceMetadataViews = List.of(
                getInstanceMetadatatWithFqdn("master"),
                getInstanceMetadatatWithFqdn("gateway")
        );
        when(stackDto.getAllNotTerminatedInstanceMetaData()).thenReturn(instanceMetadataViews);

        boolean actual = underTest.allNodesSelectedForRepair(stackDto, Set.of("master"));

        assertFalse(actual);
    }

    @Test
    public void testAllNodesSelectedForRepairWhenTheFqdnsToRepairContainsAllTheFqdnsOfTheStack() {
        StackDto stackDto = mock(StackDto.class);
        List<InstanceMetadataView> instanceMetadataViews = List.of(
                getInstanceMetadatatWithFqdn("master"),
                getInstanceMetadatatWithFqdn("gateway")
        );
        when(stackDto.getAllNotTerminatedInstanceMetaData()).thenReturn(instanceMetadataViews);

        boolean actual = underTest.allNodesSelectedForRepair(stackDto, Set.of("master", "gateway"));

        assertTrue(actual);
    }

    @Test
    public void testCalculateUpgradeVariantWhenTheFqdnsToRepairNotContainsAllTheFqdnsOfTheStack() {
        StackDto stackDto = mock(StackDto.class, RETURNS_DEEP_STUBS);
        InstanceMetaData masterInstanceMetadata = getInstanceMetadatatWithFqdn("master");
        InstanceMetaData gatewayInstanceMetadata = getInstanceMetadatatWithFqdn("gateway");
        List<InstanceMetadataView> instanceMetadataViews = List.of(masterInstanceMetadata, gatewayInstanceMetadata);
        when(stackDto.getAllNotTerminatedInstanceMetaData()).thenReturn(instanceMetadataViews);
        Map<HostGroupName, Set<InstanceMetaData>> success = Map.of(
                HostGroupName.hostGroupName("gateway"), Set.of(gatewayInstanceMetadata)
        );
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> mapRepairValidationResult = Result.success(success);

        String calculateUpgradeVariant = underTest.calculateUpgradeVariant(stackDto, USER_CRN, false, mapRepairValidationResult);

        assertNull(calculateUpgradeVariant);
    }

    @Test
    public void testCalculateUpgradeVariantWhenTheFqdnsToRepairContainsAllTheFqdnsOfTheStackButNotEntitled() {
        StackDto stackDto = mock(StackDto.class, RETURNS_DEEP_STUBS);
        InstanceMetaData masterInstanceMetadata = getInstanceMetadatatWithFqdn("master");
        InstanceMetaData gatewayInstanceMetadata = getInstanceMetadatatWithFqdn("gateway");
        List<InstanceMetadataView> instanceMetadataViews = List.of(masterInstanceMetadata, gatewayInstanceMetadata);
        when(stackDto.getAllNotTerminatedInstanceMetaData()).thenReturn(instanceMetadataViews);
        Map<HostGroupName, Set<InstanceMetaData>> success = Map.of(
                HostGroupName.hostGroupName("master"), Set.of(masterInstanceMetadata),
                HostGroupName.hostGroupName("gateway"), Set.of(gatewayInstanceMetadata)
        );
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> mapRepairValidationResult = Result.success(success);
        when(stackDto.getStack().getPlatformVariant()).thenReturn("AWS");
        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(false);

        String calculateUpgradeVariant = underTest.calculateUpgradeVariant(stackDto, USER_CRN, false, mapRepairValidationResult);

        assertEquals("AWS",  calculateUpgradeVariant);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AZURE", "GCP", "MOCK"})
    public void testCalculateUpgradeVariantWhenTheFqdnsToRepairContainsAllTheFqdnsOfTheStackButNotAwsPlatformVariantTheOriginal(String platformVariant) {
        StackDto stackDto = mock(StackDto.class, RETURNS_DEEP_STUBS);
        InstanceMetaData masterInstanceMetadata = getInstanceMetadatatWithFqdn("master");
        InstanceMetaData gatewayInstanceMetadata = getInstanceMetadatatWithFqdn("gateway");
        List<InstanceMetadataView> instanceMetadataViews = List.of(masterInstanceMetadata, gatewayInstanceMetadata);
        when(stackDto.getAllNotTerminatedInstanceMetaData()).thenReturn(instanceMetadataViews);
        Map<HostGroupName, Set<InstanceMetaData>> success = Map.of(
                HostGroupName.hostGroupName("master"), Set.of(masterInstanceMetadata),
                HostGroupName.hostGroupName("gateway"), Set.of(gatewayInstanceMetadata)
        );
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> mapRepairValidationResult = Result.success(success);
        when(stackDto.getStack().getPlatformVariant()).thenReturn(platformVariant);

        String calculateUpgradeVariant = underTest.calculateUpgradeVariant(stackDto, USER_CRN, false, mapRepairValidationResult);

        assertEquals(platformVariant,  calculateUpgradeVariant);
    }

    @Test
    public void testCalculateUpgradeVariantWhenTheFqdnsToRepairContainsAllTheFqdnsOfTheStack() {
        StackDto stackDto = mock(StackDto.class, RETURNS_DEEP_STUBS);
        InstanceMetaData masterInstanceMetadata = getInstanceMetadatatWithFqdn("master");
        InstanceMetaData gatewayInstanceMetadata = getInstanceMetadatatWithFqdn("gateway");
        List<InstanceMetadataView> instanceMetadataViews = List.of(masterInstanceMetadata, gatewayInstanceMetadata);
        when(stackDto.getAllNotTerminatedInstanceMetaData()).thenReturn(instanceMetadataViews);
        Map<HostGroupName, Set<InstanceMetaData>> success = Map.of(
                HostGroupName.hostGroupName("master"), Set.of(masterInstanceMetadata),
                HostGroupName.hostGroupName("gateway"), Set.of(gatewayInstanceMetadata)
        );
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> mapRepairValidationResult = Result.success(success);
        when(stackDto.getStack().getPlatformVariant()).thenReturn("AWS");
        when(entitlementService.awsVariantMigrationEnabled(ACCOUNT_ID)).thenReturn(true);

        String calculateUpgradeVariant = underTest.calculateUpgradeVariant(stackDto, USER_CRN, false, mapRepairValidationResult);

        assertEquals("AWS_NATIVE",  calculateUpgradeVariant);
    }

    private InstanceMetaData getInstanceMetadatatWithFqdn(String discoveredFqdn) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(discoveredFqdn);
        return instanceMetaData;
    }
}
