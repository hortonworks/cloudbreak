package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.DeploymentOperation;
import com.azure.resourcemanager.resources.models.TargetResource;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTestUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AzureInstanceTypeRetryExceptionMatcherTest {

    private static final String RG = "rg";

    private static final String DEPLOYMENT = "stack-1";

    private final AzureInstanceTypeRetryExceptionMatcher underTest = new AzureInstanceTypeRetryExceptionMatcher();

    @Test
    void isInstanceTypeNotSupportedReturnsTrueWhenTopLevelCodeIsSkuNotAvailable() {
        ManagementError topLevel = AzureTestUtils.managementError("SkuNotAvailable", "Sku is unavailable");
        ManagementException e = new ManagementException("Provisioning failed", null, topLevel);
        assertTrue(underTest.isInstanceTypeNotSupported(e));
    }

    @Test
    void isInstanceTypeNotSupportedReturnsTrueWhenNestedDetailCodeIsAllocationFailed() {
        ManagementError topLevel = AzureTestUtils.managementError("DeploymentFailed", "Multiple sub errors");
        ManagementError detail = AzureTestUtils.managementError("AllocationFailed", "Allocation failed in zone");
        AzureTestUtils.setDetails(topLevel, List.of(detail));
        ManagementException e = new ManagementException("Provisioning failed", null, topLevel);
        assertTrue(underTest.isInstanceTypeNotSupported(e));
    }

    @Test
    void isInstanceTypeNotSupportedReturnsTrueForZonalAllocationFailed() {
        ManagementError topLevel = AzureTestUtils.managementError("ZonalAllocationFailed", "Cannot allocate in zone");
        ManagementException e = new ManagementException("Provisioning failed", null, topLevel);
        assertTrue(underTest.isInstanceTypeNotSupported(e));
    }

    @Test
    void isInstanceTypeNotSupportedReturnsFalseForUnrelatedCode() {
        ManagementError topLevel = AzureTestUtils.managementError("AuthorizationFailed", "Not authorized");
        ManagementException e = new ManagementException("Provisioning failed", null, topLevel);
        assertFalse(underTest.isInstanceTypeNotSupported(e));
    }

    @Test
    void isInstanceTypeNotSupportedReturnsFalseForNullException() {
        assertFalse(underTest.isInstanceTypeNotSupported(null));
    }

    @Test
    void isInstanceTypeNotSupportedReturnsFalseWhenValueIsNull() {
        ManagementException e = new ManagementException("Provisioning failed", null);
        assertFalse(underTest.isInstanceTypeNotSupported(e));
    }

    @Test
    void findGroupsWithCapacityFailureMapsFailedVmOperationToOwningGroup() {
        AzureClient client = mock(AzureClient.class);
        DeploymentOperation failing = mockVmOperation("Failed", "stack-1master0", "Microsoft.Compute/virtualMachines",
                "{\"code\":\"SkuNotAvailable\",\"message\":\"sku unavailable\"}");
        DeploymentOperation succeeding = mockVmOperation("Succeeded", "stack-1worker0", "Microsoft.Compute/virtualMachines", null);
        AzureListResult<DeploymentOperation> listResult = mock(AzureListResult.class);
        when(listResult.getAll()).thenReturn(List.of(failing, succeeding));
        when(client.getTemplateDeploymentOperations(RG, DEPLOYMENT)).thenReturn(listResult);

        Set<String> groups = underTest.findGroupsWithCapacityFailure(RG, DEPLOYMENT, "stack-1",
                List.of("master", "worker"), client);

        assertEquals(Set.of("master"), groups);
    }

    @Test
    void findGroupsWithCapacityFailureIgnoresFailedNonVmOperations() {
        AzureClient client = mock(AzureClient.class);
        DeploymentOperation failingNic = mockVmOperation("Failed", "nic1", "Microsoft.Network/networkInterfaces",
                "{\"code\":\"SkuNotAvailable\",\"message\":\"sku unavailable\"}");
        AzureListResult<DeploymentOperation> listResult = mock(AzureListResult.class);
        when(listResult.getAll()).thenReturn(List.of(failingNic));
        when(client.getTemplateDeploymentOperations(RG, DEPLOYMENT)).thenReturn(listResult);

        Set<String> groups = underTest.findGroupsWithCapacityFailure(RG, DEPLOYMENT, "stack-1",
                List.of("master"), client);

        assertTrue(groups.isEmpty());
    }

    @Test
    void findGroupsWithCapacityFailureIgnoresFailedVmWithNonCapacityStatusMessage() {
        AzureClient client = mock(AzureClient.class);
        DeploymentOperation failing = mockVmOperation("Failed", "stack-1master0", "Microsoft.Compute/virtualMachines",
                "{\"code\":\"AuthorizationFailed\",\"message\":\"forbidden\"}");
        AzureListResult<DeploymentOperation> listResult = mock(AzureListResult.class);
        when(listResult.getAll()).thenReturn(List.of(failing));
        when(client.getTemplateDeploymentOperations(RG, DEPLOYMENT)).thenReturn(listResult);

        Set<String> groups = underTest.findGroupsWithCapacityFailure(RG, DEPLOYMENT, "stack-1",
                List.of("master"), client);

        assertTrue(groups.isEmpty());
    }

    @Test
    void findGroupsWithCapacityFailureReturnsEmptyOnClientException() {
        AzureClient client = mock(AzureClient.class);
        when(client.getTemplateDeploymentOperations(RG, DEPLOYMENT)).thenThrow(new RuntimeException("boom"));

        Set<String> groups = underTest.findGroupsWithCapacityFailure(RG, DEPLOYMENT, "stack-1",
                List.of("master"), client);

        assertTrue(groups.isEmpty());
    }

    @Test
    void isInstanceTypeNotSupportedReturnsTrueForTopLevelQuotaExceeded() {
        ManagementError topLevel = AzureTestUtils.managementError("QuotaExceeded", "quota exceeded");
        ManagementException e = new ManagementException("Provisioning failed", null, topLevel);
        assertTrue(underTest.isInstanceTypeNotSupported(e));
    }

    @Test
    void isInstanceTypeNotSupportedReturnsTrueForTwoLevelNestedQuotaExceeded() {
        ManagementError leaf = AzureTestUtils.managementError("QuotaExceeded",
                "Operation could not be completed as it results in exceeding approved standardNVSv3Family Cores quota.");
        ManagementError middle = AzureTestUtils.managementError("DeploymentFailed", "wrapping deployment error");
        AzureTestUtils.setDetails(middle, List.of(leaf));
        ManagementError top = AzureTestUtils.managementError("InvalidTemplateDeployment", "template invalid");
        AzureTestUtils.setDetails(top, List.of(middle));
        ManagementException e = new ManagementException("Preflight failed", null, top);
        assertTrue(underTest.isInstanceTypeNotSupported(e));
    }

    @Test
    void isInstanceTypeNotSupportedReturnsTrueForSubscriptionQuotaReached() {
        ManagementError topLevel = AzureTestUtils.managementError("SubscriptionQuotaReached", "subscription cap");
        ManagementException e = new ManagementException("Provisioning failed", null, topLevel);
        assertTrue(underTest.isInstanceTypeNotSupported(e));
    }

    @Test
    void isInstanceTypeNotSupportedReturnsTrueForOperationNotAllowed() {
        ManagementError topLevel = AzureTestUtils.managementError("OperationNotAllowed", "not allowed");
        ManagementException e = new ManagementException("Provisioning failed", null, topLevel);
        assertTrue(underTest.isInstanceTypeNotSupported(e));
    }

    @Test
    void isQuotaCodePresentReturnsTrueForNestedQuotaExceeded() {
        ManagementError leaf = AzureTestUtils.managementError("QuotaExceeded", "standardNVSv3Family exceeded");
        ManagementError top = AzureTestUtils.managementError("InvalidTemplateDeployment", "template invalid");
        AzureTestUtils.setDetails(top, List.of(leaf));
        ManagementException e = new ManagementException("Preflight failed", null, top);
        assertTrue(underTest.isQuotaCodePresent(e));
    }

    @Test
    void isQuotaCodePresentReturnsFalseForAllocationFailedOnly() {
        ManagementError topLevel = AzureTestUtils.managementError("AllocationFailed", "no capacity");
        ManagementException e = new ManagementException("Provisioning failed", null, topLevel);
        assertFalse(underTest.isQuotaCodePresent(e));
    }

    @Test
    void findGroupsWithQuotaFailureMatchesGroupByStandardNvSv3Family() {
        ManagementError leaf = AzureTestUtils.managementError("QuotaExceeded",
                "Operation could not be completed as it results in exceeding approved standardNVSv3Family Cores quota.");
        ManagementError top = AzureTestUtils.managementError("InvalidTemplateDeployment", "template invalid");
        AzureTestUtils.setDetails(top, List.of(leaf));
        ManagementException e = new ManagementException("Preflight failed", null, top);

        Map<String, String> flavorToFamily = Map.of(
                "Standard_NV12s_v3", "standardNVSv3Family",
                "Standard_D4s_v3", "standardDSv3Family");
        List<Group> groups = List.of(
                mockGroup("gpu", "Standard_NV12s_v3"),
                mockGroup("worker", "Standard_D4s_v3"));

        Set<String> failing = underTest.findGroupsWithQuotaFailure(e, flavorToFamily, groups);

        assertEquals(Set.of("gpu"), failing);
    }

    @Test
    void findGroupsWithQuotaFailureMatchesBothGroupsWhenSharedFamily() {
        ManagementError leaf = AzureTestUtils.managementError("QuotaExceeded",
                "exceeding approved standardDSv3Family Cores quota");
        ManagementException e = new ManagementException("Preflight failed", null, leaf);
        Map<String, String> flavorToFamily = Map.of(
                "Standard_D4s_v3", "standardDSv3Family",
                "Standard_D8s_v3", "standardDSv3Family");
        List<Group> groups = List.of(
                mockGroup("worker", "Standard_D4s_v3"),
                mockGroup("compute", "Standard_D8s_v3"));

        Set<String> failing = underTest.findGroupsWithQuotaFailure(e, flavorToFamily, groups);

        assertEquals(Set.of("worker", "compute"), failing);
    }

    @Test
    void findGroupsWithQuotaFailureReturnsEmptyWhenNoFamilyInMessage() {
        ManagementError leaf = AzureTestUtils.managementError("QuotaExceeded", "quota exceeded without a family token");
        ManagementException e = new ManagementException("Preflight failed", null, leaf);
        Map<String, String> flavorToFamily = Map.of("Standard_D4s_v3", "standardDSv3Family");
        List<Group> groups = List.of(mockGroup("worker", "Standard_D4s_v3"));

        Set<String> failing = underTest.findGroupsWithQuotaFailure(e, flavorToFamily, groups);

        assertTrue(failing.isEmpty());
    }

    @Test
    void findGroupsWithQuotaFailureReturnsEmptyWhenFlavorFamilyMapIsEmpty() {
        ManagementError leaf = AzureTestUtils.managementError("QuotaExceeded",
                "exceeding approved standardNVSv3Family Cores quota");
        ManagementException e = new ManagementException("Preflight failed", null, leaf);
        List<Group> groups = List.of(mockGroup("gpu", "Standard_NV12s_v3"));

        Set<String> failing = underTest.findGroupsWithQuotaFailure(e, Map.of(), groups);

        assertTrue(failing.isEmpty());
    }

    @Test
    void findGroupsWithQuotaFailureIsCaseInsensitiveOnFamilyMatch() {
        ManagementError leaf = AzureTestUtils.managementError("QuotaExceeded",
                "exceeding approved StandardNVSv3Family Cores quota");
        ManagementException e = new ManagementException("Preflight failed", null, leaf);
        Map<String, String> flavorToFamily = Map.of("Standard_NV12s_v3", "standardNVSv3Family");
        List<Group> groups = List.of(mockGroup("gpu", "Standard_NV12s_v3"));

        Set<String> failing = underTest.findGroupsWithQuotaFailure(e, flavorToFamily, groups);

        assertEquals(Set.of("gpu"), failing);
    }

    @Test
    void findGroupsWithQuotaFailureCollectsMessageAtTwoLevelsDeep() {
        ManagementError leaf = AzureTestUtils.managementError("QuotaExceeded",
                "exceeding approved standardNVSv3Family Cores quota");
        ManagementError middle = AzureTestUtils.managementError("DeploymentFailed", "wrapping");
        AzureTestUtils.setDetails(middle, List.of(leaf));
        ManagementError top = AzureTestUtils.managementError("InvalidTemplateDeployment", "template invalid");
        AzureTestUtils.setDetails(top, List.of(middle));
        ManagementException e = new ManagementException("Preflight failed", null, top);
        Map<String, String> flavorToFamily = Map.of("Standard_NV12s_v3", "standardNVSv3Family");
        List<Group> groups = List.of(mockGroup("gpu", "Standard_NV12s_v3"));

        Set<String> failing = underTest.findGroupsWithQuotaFailure(e, flavorToFamily, groups);

        assertEquals(Set.of("gpu"), failing);
    }

    private Group mockGroup(String name, String flavor) {
        Group group = mock(Group.class);
        when(group.getName()).thenReturn(name);
        InstanceTemplate template = mock(InstanceTemplate.class);
        when(template.getFlavor()).thenReturn(flavor);
        when(group.getReferenceInstanceTemplate()).thenReturn(template);
        return group;
    }

    private DeploymentOperation mockVmOperation(String provisioningState, String resourceName, String resourceType, String statusMessage) {
        DeploymentOperation op = mock(DeploymentOperation.class);
        when(op.provisioningState()).thenReturn(provisioningState);
        TargetResource target = mock(TargetResource.class);
        when(target.resourceName()).thenReturn(resourceName);
        when(target.resourceType()).thenReturn(resourceType);
        when(op.targetResource()).thenReturn(target);
        if (statusMessage != null) {
            when(op.statusMessage()).thenReturn(statusMessage);
        }
        return op;
    }
}
