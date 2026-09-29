package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.azure.resourcemanager.compute.fluent.models.ResourceSkuInner;
import com.azure.resourcemanager.compute.models.ComputeUsage;
import com.azure.resourcemanager.compute.models.ComputeUsageUnit;
import com.azure.resourcemanager.compute.models.ResourceSkuRestrictionInfo;
import com.azure.resourcemanager.compute.models.ResourceSkuRestrictions;
import com.azure.resourcemanager.compute.models.ResourceSkuRestrictionsType;
import com.azure.resourcemanager.compute.models.UsageName;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.VerticalScaleProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;

public class AzureComputeCapacityCheckTest {

    private CloudProviderEndpointResolver endpointResolver;

    private AzureComputeCapacityCheck underTest;

    @BeforeMethod
    public void setUp() {
        endpointResolver = mock(CloudProviderEndpointResolver.class);

        underTest = new AzureComputeCapacityCheck();
        ReflectionTestUtils.setField(underTest, "endpointResolver", endpointResolver);
    }

    @Test
    public void testCheckSkippedWhenNotAzure() {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.AWS);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    public void testNormalizeAzureRegionName() {
        assertThat(AzureComputeCapacityCheck.normalizeAzureRegionName("West US 2")).isEqualTo("westus2");
        assertThat(AzureComputeCapacityCheck.normalizeAzureRegionName(null)).isNull();
    }

    @Test
    public void testFindRestrictedSizesFlagsLocationRestrictedSku() {
        ResourceSkuInner restrictedSku = mockSku("Standard_D8s_v5", "standardDSv5Family", restriction("westus2"));
        ResourceSkuInner cleanSku = mockSku("Standard_D2s_v5", "standardDSv5Family");

        List<String> restricted = AzureComputeCapacityCheck.findRestrictedSizes(
                List.of(restrictedSku, cleanSku), Set.of("Standard_D8s_v5", "Standard_D2s_v5"), "westus2");

        assertThat(restricted).containsExactly("Standard_D8s_v5");
    }

    @Test
    public void testFindRestrictedSizesIgnoresRestrictionInOtherRegion() {
        ResourceSkuInner sku = mockSku("Standard_D8s_v5", "standardDSv5Family", restriction("eastus"));

        List<String> restricted = AzureComputeCapacityCheck.findRestrictedSizes(List.of(sku), Set.of("Standard_D8s_v5"), "westus2");

        assertThat(restricted).isEmpty();
    }

    @Test
    public void testFindRestrictedSizesIgnoresSkuNotInRequestedSet() {
        ResourceSkuInner sku = mockSku("Standard_D8s_v5", "standardDSv5Family", restriction("westus2"));

        List<String> restricted = AzureComputeCapacityCheck.findRestrictedSizes(List.of(sku), Set.of("Standard_D2s_v5"), "westus2");

        assertThat(restricted).isEmpty();
    }

    @Test
    public void testFindQuotaExhaustedFamiliesFlagsLowHeadroom() {
        ResourceSkuInner sku = mockSku("Standard_D8s_v5", "standardDSv5Family");
        ComputeUsage usage = mockUsage("standardDSv5Family", 96, 92);

        List<String> exhausted = AzureComputeCapacityCheck.findQuotaExhaustedFamilies(
                List.of(sku), List.of(usage), Set.of("Standard_D8s_v5"), 8);

        assertThat(exhausted).containsExactly("standardDSv5Family");
    }

    @Test
    public void testFindQuotaExhaustedFamiliesPassesWithEnoughHeadroom() {
        ResourceSkuInner sku = mockSku("Standard_D8s_v5", "standardDSv5Family");
        ComputeUsage usage = mockUsage("standardDSv5Family", 96, 50);

        List<String> exhausted = AzureComputeCapacityCheck.findQuotaExhaustedFamilies(
                List.of(sku), List.of(usage), Set.of("Standard_D8s_v5"), 8);

        assertThat(exhausted).isEmpty();
    }

    @Test
    public void testFindQuotaExhaustedFamiliesIgnoresUnrelatedFamily() {
        ResourceSkuInner sku = mockSku("Standard_D8s_v5", "standardDSv5Family");
        ComputeUsage usage = mockUsage("standardEv5Family", 96, 95);

        List<String> exhausted = AzureComputeCapacityCheck.findQuotaExhaustedFamilies(
                List.of(sku), List.of(usage), Set.of("Standard_D8s_v5"), 8);

        assertThat(exhausted).isEmpty();
    }

    @Test
    public void testCollectConfiguredVmSizesDedupsAndDropsBlanks() {
        AzureProperties properties = new AzureProperties();
        VerticalScaleProperties.DataLakeProperties datalakeScale = new VerticalScaleProperties.DataLakeProperties();
        datalakeScale.setInstanceType("Standard_D8s_v5");
        VerticalScaleProperties.DataHubProperties datahubScale = new VerticalScaleProperties.DataHubProperties();
        datahubScale.setInstanceType("Standard_D8s_v5");
        VerticalScaleProperties.FreeIpaProperties freeipaScale = new VerticalScaleProperties.FreeIpaProperties();
        freeipaScale.setInstanceType("");
        properties.getVerticalScale().setDatalake(datalakeScale);
        properties.getVerticalScale().setDatahub(datahubScale);
        properties.getVerticalScale().setFreeipa(freeipaScale);
        properties.getInstance().getTypes().setDefault("Standard_D16s_v5");
        properties.getInstance().getTypes().setDatalake(java.util.Map.of("master", "Standard_E8s_v5"));
        properties.getStorageOptimizedInstance().setType(null);
        properties.getArm64Instance().setType("Standard_E4pds_v5");
        properties.getFreeipa().setInstanceType("Standard_D8s_v5");

        Set<String> sizes = AzureComputeCapacityCheck.collectConfiguredVmSizes(properties);

        assertThat(sizes).containsExactlyInAnyOrder("Standard_D8s_v5", "Standard_D16s_v5", "Standard_E8s_v5", "Standard_E4pds_v5");
    }

    private ResourceSkuInner mockSku(String name, String family, ResourceSkuRestrictions... restrictions) {
        ResourceSkuInner sku = mock(ResourceSkuInner.class);
        when(sku.name()).thenReturn(name);
        when(sku.family()).thenReturn(family);
        when(sku.restrictions()).thenReturn(List.of(restrictions));
        return sku;
    }

    private ResourceSkuRestrictions restriction(String location) {
        ResourceSkuRestrictionInfo restrictionInfo = mock(ResourceSkuRestrictionInfo.class);
        when(restrictionInfo.locations()).thenReturn(List.of(location));
        ResourceSkuRestrictions restriction = mock(ResourceSkuRestrictions.class);
        when(restriction.type()).thenReturn(ResourceSkuRestrictionsType.LOCATION);
        when(restriction.restrictionInfo()).thenReturn(restrictionInfo);
        return restriction;
    }

    private ComputeUsage mockUsage(String familyName, long limit, int currentValue) {
        UsageName usageName = mock(UsageName.class);
        when(usageName.value()).thenReturn(familyName);
        ComputeUsage usage = mock(ComputeUsage.class);
        when(usage.name()).thenReturn(usageName);
        when(usage.limit()).thenReturn(limit);
        when(usage.currentValue()).thenReturn(currentValue);
        when(usage.unit()).thenReturn(ComputeUsageUnit.COUNT);
        return usage;
    }
}
