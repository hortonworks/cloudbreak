package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class AzureComputeCapacityCheckTest {

    @Mock
    private CloudProviderEndpointResolver endpointResolver;

    @InjectMocks
    private AzureComputeCapacityCheck underTest;

    @Test
    void testCheckSkippedWhenNotAzure() {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.AWS);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    void testNormalizeAzureRegionName() {
        assertThat(AzureComputeCapacityCheck.normalizeAzureRegionName("West US 2")).isEqualTo("westus2");
        assertThat(AzureComputeCapacityCheck.normalizeAzureRegionName(null)).isNull();
    }

    @Test
    void testFindRestrictedSizesFlagsLocationRestrictedSku() {
        ResourceSkuInner restrictedSku = mockSku("Standard_D8s_v5", "standardDSv5Family", restriction("westus2"));
        ResourceSkuInner cleanSku = mockSku("Standard_D2s_v5", "standardDSv5Family");

        List<String> restricted = AzureComputeCapacityCheck.findRestrictedSizes(
                List.of(restrictedSku, cleanSku), Set.of("Standard_D8s_v5", "Standard_D2s_v5"), "westus2");

        assertThat(restricted).containsExactly("Standard_D8s_v5");
    }

    @Test
    void testFindRestrictedSizesIgnoresRestrictionInOtherRegion() {
        ResourceSkuInner sku = mockSku("Standard_D8s_v5", "standardDSv5Family", restriction("eastus"));

        List<String> restricted = AzureComputeCapacityCheck.findRestrictedSizes(List.of(sku), Set.of("Standard_D8s_v5"), "westus2");

        assertThat(restricted).isEmpty();
    }

    @Test
    void testFindRestrictedSizesIgnoresSkuNotInRequestedSet() {
        ResourceSkuInner sku = mockSku("Standard_D8s_v5", "standardDSv5Family", restriction("westus2"));

        List<String> restricted = AzureComputeCapacityCheck.findRestrictedSizes(List.of(sku), Set.of("Standard_D2s_v5"), "westus2");

        assertThat(restricted).isEmpty();
    }

    @Test
    void testFindQuotaExhaustedFamiliesFlagsLowHeadroom() {
        ResourceSkuInner sku = mockSku("Standard_D8s_v5", "standardDSv5Family");
        ComputeUsage usage = mockUsage("standardDSv5Family", 96, 92);

        List<String> exhausted = AzureComputeCapacityCheck.findQuotaExhaustedFamilies(
                List.of(sku), List.of(usage), Set.of("Standard_D8s_v5"), 8);

        assertThat(exhausted).containsExactly("standardDSv5Family");
    }

    @Test
    void testFindQuotaExhaustedFamiliesPassesWithEnoughHeadroom() {
        ResourceSkuInner sku = mockSku("Standard_D8s_v5", "standardDSv5Family");
        ComputeUsage usage = mockUsage("standardDSv5Family", 96, 50);

        List<String> exhausted = AzureComputeCapacityCheck.findQuotaExhaustedFamilies(
                List.of(sku), List.of(usage), Set.of("Standard_D8s_v5"), 8);

        assertThat(exhausted).isEmpty();
    }

    @Test
    void testFindQuotaExhaustedFamiliesIgnoresUnrelatedFamily() {
        ResourceSkuInner sku = mockSku("Standard_D8s_v5", "standardDSv5Family");
        ComputeUsage usage = mockUsage("standardEv5Family", 96, 95);

        List<String> exhausted = AzureComputeCapacityCheck.findQuotaExhaustedFamilies(
                List.of(sku), List.of(usage), Set.of("Standard_D8s_v5"), 8);

        assertThat(exhausted).isEmpty();
    }

    @Test
    void testCollectConfiguredVmSizesDedupsAndDropsBlanks() {
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
        lenient().when(sku.name()).thenReturn(name);
        lenient().when(sku.family()).thenReturn(family);
        lenient().when(sku.restrictions()).thenReturn(List.of(restrictions));
        return sku;
    }

    private ResourceSkuRestrictions restriction(String location) {
        ResourceSkuRestrictionInfo restrictionInfo = mock(ResourceSkuRestrictionInfo.class);
        lenient().when(restrictionInfo.locations()).thenReturn(List.of(location));
        ResourceSkuRestrictions restriction = mock(ResourceSkuRestrictions.class);
        lenient().when(restriction.type()).thenReturn(ResourceSkuRestrictionsType.LOCATION);
        lenient().when(restriction.restrictionInfo()).thenReturn(restrictionInfo);
        return restriction;
    }

    private ComputeUsage mockUsage(String familyName, long limit, int currentValue) {
        UsageName usageName = mock(UsageName.class);
        lenient().when(usageName.value()).thenReturn(familyName);
        ComputeUsage usage = mock(ComputeUsage.class);
        lenient().when(usage.name()).thenReturn(usageName);
        lenient().when(usage.limit()).thenReturn(limit);
        lenient().when(usage.currentValue()).thenReturn(currentValue);
        lenient().when(usage.unit()).thenReturn(ComputeUsageUnit.COUNT);
        return usage;
    }
}
