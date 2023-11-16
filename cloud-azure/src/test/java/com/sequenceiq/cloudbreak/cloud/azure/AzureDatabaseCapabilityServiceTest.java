package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.DatabaseAvailabiltyType.databaseAvailabiltyType;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.SAME_ZONE;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.ZONE_REDUNDANT;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.resourcemanager.postgresqlflexibleserver.models.FlexibleServerCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.FlexibleServerEditionCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerSkuCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ZoneRedundantHaSupportedEnum;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureFlexibleServerClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRegionProvider;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureCoordinate;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
class AzureDatabaseCapabilityServiceTest {

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureRegionProvider azureRegionProvider;

    @InjectMocks
    private AzureDatabaseCapabilityService azureDatabaseCapabilityService;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private Region region;

    @Mock
    private AzureClient azureClient;

    @Mock
    private AzureFlexibleServerClient azureFlexibleServerClient;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(azureDatabaseCapabilityService, "instanceTypeRegex", "^Standard_E4d?s.*$");
        ReflectionTestUtils.setField(azureDatabaseCapabilityService, "serverEdition", "MemoryOptimized");
        ReflectionTestUtils.setField(azureDatabaseCapabilityService, "defaultInstanceType", "Standard_E4ds_v4");
    }

    @Test
    void testDatabaseCapabilitiesWhenNoRegion() {
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
        Map<Region, AzureCoordinate> regions = Map.of(
                Region.region("westus"), azureCoordinate("westus"),
                Region.region("westus2"), azureCoordinate("westus2"));
        when(azureRegionProvider.filterEnabledRegions((Region) null)).thenReturn(regions);
        FlexibleServerCapability flexibleServerCapability = createFlexibleServerCapability(ZoneRedundantHaSupportedEnum.DISABLED,
                Map.of("MemoryOptimized", List.of("instanceType1", "Standard_E4ds_v6", "Standard_E4ds_v4")));
        FlexibleServerCapability flexibleServerCapability1 = createFlexibleServerCapability(ZoneRedundantHaSupportedEnum.ENABLED,
                Map.of("MemoryOptimized", List.of("instanceType3", "instanceType4", "Standard_E4s_v1")));
        Map<Region, Optional<FlexibleServerCapability>> flexibleServerCapabilityMap = Map.of(
                Region.region("westus"), Optional.of(flexibleServerCapability),
                Region.region("westus2"), Optional.of(flexibleServerCapability1));
        when(azureFlexibleServerClient.getFlexibleServerCapabilityMap(regions)).thenReturn(flexibleServerCapabilityMap);

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService.databaseCapabilities(cloudCredential, null, new HashMap<>());

        com.azure.core.management.Region azureRegion1 = com.azure.core.management.Region.fromName("westus");
        com.azure.core.management.Region azureRegion2 = com.azure.core.management.Region.fromName("westus2");
        Region region1Name = Region.region(azureRegion1.name());
        Region region1Label = Region.region(azureRegion1.label());
        Region region2Name = Region.region(azureRegion2.name());
        Region region2Label = Region.region(azureRegion2.label());
        Assertions.assertTrue(capabilities.getEnabledRegions().get(databaseAvailabiltyType(SAME_ZONE.name())).contains(region1Label));
        Assertions.assertTrue(capabilities.getEnabledRegions().get(databaseAvailabiltyType(SAME_ZONE.name())).contains(region1Name));
        Assertions.assertTrue(capabilities.getEnabledRegions().get(databaseAvailabiltyType(SAME_ZONE.name())).contains(region2Label));
        Assertions.assertTrue(capabilities.getEnabledRegions().get(databaseAvailabiltyType(SAME_ZONE.name())).contains(region2Name));
        Assertions.assertFalse(capabilities.getEnabledRegions().get(databaseAvailabiltyType(ZONE_REDUNDANT.name())).contains(region1Label));
        Assertions.assertFalse(capabilities.getEnabledRegions().get(databaseAvailabiltyType(ZONE_REDUNDANT.name())).contains(region1Name));
        Assertions.assertTrue(capabilities.getEnabledRegions().get(databaseAvailabiltyType(ZONE_REDUNDANT.name())).contains(region2Label));
        Assertions.assertTrue(capabilities.getEnabledRegions().get(databaseAvailabiltyType(ZONE_REDUNDANT.name())).contains(region2Name));
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Label), "Standard_E4ds_v6");
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Name), "Standard_E4ds_v6");
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region2Label), "Standard_E4s_v1");
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region2Name), "Standard_E4s_v1");
    }

    @Test
    void testDatabaseCapabilitiesWhenRegionIsGiven() {
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
        Map<Region, AzureCoordinate> regions = Map.of(
                Region.region("westus"), azureCoordinate("westus"));
        when(azureRegionProvider.filterEnabledRegions(Region.region("westus"))).thenReturn(regions);
        FlexibleServerCapability flexibleServerCapability = createFlexibleServerCapability(ZoneRedundantHaSupportedEnum.DISABLED,
                Map.of(
                        "serverEdition1", List.of("instanceType11", "instanceType12"),
                        "MemoryOptimized", List.of("instanceType21", "instanceType22", "Standard_E4ds_v6")));
        FlexibleServerCapability flexibleServerCapability1 = createFlexibleServerCapability(ZoneRedundantHaSupportedEnum.ENABLED,
                Map.of(
                        "serverEdition1", List.of("instanceType13", "instanceType14"),
                        "serverEdition2", List.of("instanceType23", "instanceType24")));
        Map<Region, Optional<FlexibleServerCapability>> flexibleServerCapabilityMap = Map.of(
                Region.region("westus"), Optional.of(flexibleServerCapability),
                Region.region("westus2"), Optional.of(flexibleServerCapability1));
        when(azureFlexibleServerClient.getFlexibleServerCapabilityMap(anyMap())).thenReturn(flexibleServerCapabilityMap);

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService.databaseCapabilities(cloudCredential, Region.region("westus"), Map.of());

        com.azure.core.management.Region azureRegion1 = com.azure.core.management.Region.fromName("westus");
        com.azure.core.management.Region azureRegion2 = com.azure.core.management.Region.fromName("westus2");
        Region region1Name = Region.region(azureRegion1.name());
        Region region1Label = Region.region(azureRegion1.label());
        Region region2Name = Region.region(azureRegion2.name());
        Region region2Label = Region.region(azureRegion2.label());
        Assertions.assertTrue(capabilities.getEnabledRegions().get(databaseAvailabiltyType(SAME_ZONE.name())).contains(region1Label));
        Assertions.assertTrue(capabilities.getEnabledRegions().get(databaseAvailabiltyType(SAME_ZONE.name())).contains(region1Name));
        Assertions.assertFalse(capabilities.getEnabledRegions().get(databaseAvailabiltyType(SAME_ZONE.name())).contains(region2Label));
        Assertions.assertFalse(capabilities.getEnabledRegions().get(databaseAvailabiltyType(SAME_ZONE.name())).contains(region2Name));
        Assertions.assertTrue(capabilities.getEnabledRegions().get(databaseAvailabiltyType(ZONE_REDUNDANT.name())).isEmpty());
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Label), "Standard_E4ds_v6");
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Name), "Standard_E4ds_v6");
        Assertions.assertNull(capabilities.getRegionDefaultInstanceTypeMap().get(region2Label));
        Assertions.assertNull(capabilities.getRegionDefaultInstanceTypeMap().get(region2Name));
    }

    @Test
    void testDatabaseCapabilitiesWhenRegionIsGivenNoInstanceTypeMatch() {
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
        Map<Region, AzureCoordinate> regions = Map.of(
                Region.region("westus"), azureCoordinate("westus"));
        when(azureRegionProvider.filterEnabledRegions(Region.region("westus"))).thenReturn(regions);
        FlexibleServerCapability flexibleServerCapability = createFlexibleServerCapability(ZoneRedundantHaSupportedEnum.DISABLED,
                Map.of(
                        "serverEdition1", List.of("instanceType11", "instanceType12"),
                        "MemoryOptimized", List.of("instanceType21", "instanceType22", "instanceType23")));
        FlexibleServerCapability flexibleServerCapability1 = createFlexibleServerCapability(ZoneRedundantHaSupportedEnum.ENABLED,
                Map.of(
                        "serverEdition1", List.of("instanceType13", "instanceType14"),
                        "serverEdition2", List.of("instanceType23", "instanceType24")));
        Map<Region, Optional<FlexibleServerCapability>> flexibleServerCapabilityMap = Map.of(
                Region.region("westus"), Optional.of(flexibleServerCapability),
                Region.region("westus2"), Optional.of(flexibleServerCapability1));
        when(azureFlexibleServerClient.getFlexibleServerCapabilityMap(anyMap())).thenReturn(flexibleServerCapabilityMap);

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService.databaseCapabilities(cloudCredential, Region.region("westus"), Map.of());

        com.azure.core.management.Region azureRegion1 = com.azure.core.management.Region.fromName("westus");
        com.azure.core.management.Region azureRegion2 = com.azure.core.management.Region.fromName("westus2");
        Region region1Name = Region.region(azureRegion1.name());
        Region region1Label = Region.region(azureRegion1.label());
        Region region2Name = Region.region(azureRegion2.name());
        Region region2Label = Region.region(azureRegion2.label());
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Label), "Standard_E4ds_v4");
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Name), "Standard_E4ds_v4");
        Assertions.assertNull(capabilities.getRegionDefaultInstanceTypeMap().get(region2Label));
        Assertions.assertNull(capabilities.getRegionDefaultInstanceTypeMap().get(region2Name));
    }

    @Test
    void testDatabaseCapabilitiesWhenRegionIsGivenNoMemoryOptimizedServer() {
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
        Map<Region, AzureCoordinate> regions = Map.of(
                Region.region("westus"), azureCoordinate("westus"));
        when(azureRegionProvider.filterEnabledRegions(Region.region("westus"))).thenReturn(regions);
        FlexibleServerCapability flexibleServerCapability = createFlexibleServerCapability(ZoneRedundantHaSupportedEnum.DISABLED,
                Map.of(
                        "serverEdition1", List.of("instanceType11", "instanceType12"),
                        "serverEdition2", List.of("instanceType21", "instanceType22", "Standard_E4ds_v6")));
        FlexibleServerCapability flexibleServerCapability1 = createFlexibleServerCapability(ZoneRedundantHaSupportedEnum.ENABLED,
                Map.of(
                        "serverEdition1", List.of("instanceType13", "instanceType14"),
                        "serverEdition2", List.of("instanceType23", "instanceType24")));
        Map<Region, Optional<FlexibleServerCapability>> flexibleServerCapabilityMap = Map.of(
                Region.region("westus"), Optional.of(flexibleServerCapability),
                Region.region("westus2"), Optional.of(flexibleServerCapability1));
        when(azureFlexibleServerClient.getFlexibleServerCapabilityMap(anyMap())).thenReturn(flexibleServerCapabilityMap);

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService.databaseCapabilities(cloudCredential, Region.region("westus"), Map.of());

        com.azure.core.management.Region azureRegion1 = com.azure.core.management.Region.fromName("westus");
        com.azure.core.management.Region azureRegion2 = com.azure.core.management.Region.fromName("westus2");
        Region region1Name = Region.region(azureRegion1.name());
        Region region1Label = Region.region(azureRegion1.label());
        Region region2Name = Region.region(azureRegion2.name());
        Region region2Label = Region.region(azureRegion2.label());
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Label), "Standard_E4ds_v4");
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Name), "Standard_E4ds_v4");
        Assertions.assertNull(capabilities.getRegionDefaultInstanceTypeMap().get(region2Label));
        Assertions.assertNull(capabilities.getRegionDefaultInstanceTypeMap().get(region2Name));
    }

    @Test
    void testDatabaseCapabilitiesWhenRegionIsGivenNoConfigsGiven() {
        ReflectionTestUtils.setField(azureDatabaseCapabilityService, "instanceTypeRegex", null);
        ReflectionTestUtils.setField(azureDatabaseCapabilityService, "serverEdition", null);
        ReflectionTestUtils.setField(azureDatabaseCapabilityService, "defaultInstanceType", null);

        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
        Map<Region, AzureCoordinate> regions = Map.of(
                Region.region("westus"), azureCoordinate("westus"));
        when(azureRegionProvider.filterEnabledRegions(Region.region("westus"))).thenReturn(regions);
        FlexibleServerCapability flexibleServerCapability = createFlexibleServerCapability(ZoneRedundantHaSupportedEnum.DISABLED,
                Map.of(
                        "serverEdition1", List.of("instanceType11", "instanceType12"),
                        "serverEdition2", List.of("instanceType21", "instanceType22")));
        FlexibleServerCapability flexibleServerCapability1 = createFlexibleServerCapability(ZoneRedundantHaSupportedEnum.ENABLED,
                Map.of(
                        "serverEdition1", List.of("instanceType13", "instanceType14"),
                        "serverEdition2", List.of("instanceType23", "instanceType24")));
        Map<Region, Optional<FlexibleServerCapability>> flexibleServerCapabilityMap = Map.of(
                Region.region("westus"), Optional.of(flexibleServerCapability),
                Region.region("westus2"), Optional.of(flexibleServerCapability1));
        when(azureFlexibleServerClient.getFlexibleServerCapabilityMap(anyMap())).thenReturn(flexibleServerCapabilityMap);

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService.databaseCapabilities(cloudCredential, Region.region("westus"), Map.of());

        com.azure.core.management.Region azureRegion1 = com.azure.core.management.Region.fromName("westus");
        com.azure.core.management.Region azureRegion2 = com.azure.core.management.Region.fromName("westus2");
        Region region1Name = Region.region(azureRegion1.name());
        Region region1Label = Region.region(azureRegion1.label());
        Region region2Name = Region.region(azureRegion2.name());
        Region region2Label = Region.region(azureRegion2.label());
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Label), "instanceType22");
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Name), "instanceType22");
        Assertions.assertNull(capabilities.getRegionDefaultInstanceTypeMap().get(region2Label));
        Assertions.assertNull(capabilities.getRegionDefaultInstanceTypeMap().get(region2Name));
    }

    private AzureCoordinate azureCoordinate(String name) {
        return AzureCoordinate.AzureCoordinateBuilder.builder()
                .longitude("1")
                .latitude("1")
                .displayName(name)
                .key(name + "key")
                .k8sSupported(false)
                .entitlements(List.of())
                .build();
    }

    private FlexibleServerCapability createFlexibleServerCapability(ZoneRedundantHaSupportedEnum zoneRedundantHaSupportedEnum,
            Map<String, List<String>> instanceTypes) {
        FlexibleServerCapability flexibleServerCapability = mock(FlexibleServerCapability.class);
        lenient().when(flexibleServerCapability.zoneRedundantHaSupported()).thenReturn(zoneRedundantHaSupportedEnum);
        List<FlexibleServerEditionCapability> flexibleServerEditionCapabilities = instanceTypes.entrySet().stream()
                .map(entry -> createFlexibleServerEditionCapability(entry.getKey(), entry.getValue()))
                .toList();
        lenient().when(flexibleServerCapability.supportedServerEditions()).thenReturn(flexibleServerEditionCapabilities);
        return flexibleServerCapability;
    }

    private FlexibleServerEditionCapability createFlexibleServerEditionCapability(String name, List<String> instanceTypes) {
        FlexibleServerEditionCapability flexibleServerEditionCapability = mock(FlexibleServerEditionCapability.class);
        lenient().when(flexibleServerEditionCapability.name()).thenReturn(name);
        List<ServerSkuCapability> serverSkus = instanceTypes.stream()
                .map(instanceType -> {
                    ServerSkuCapability serverSku = mock(ServerSkuCapability.class);
                    lenient().when(serverSku.name()).thenReturn(instanceType);
                    return serverSku;
                })
                .toList();
        lenient().when(flexibleServerEditionCapability.supportedServerSkus()).thenReturn(serverSkus);
        return flexibleServerEditionCapability;
    }
}
