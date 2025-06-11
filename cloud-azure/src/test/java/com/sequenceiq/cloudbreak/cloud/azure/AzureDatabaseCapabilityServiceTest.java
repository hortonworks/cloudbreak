package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.CloudParameterConst.DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.cloud.model.DatabaseAvailabiltyType.databaseAvailabiltyType;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.SAME_ZONE;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.ZONE_REDUNDANT;
import static com.sequenceiq.common.model.DatabaseCapabilityType.AZURE_FLEXIBLE;
import static com.sequenceiq.common.model.DatabaseCapabilityType.AZURE_SINGLE_SERVER;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
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
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerVersionCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.StorageEditionCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.StorageMbCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ZoneRedundantHaSupportedEnum;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureFlexibleServerClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRegionProvider;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureCoordinate;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureCoordinate.AzureCoordinateBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDBStorageCapabilities;
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
        ReflectionTestUtils.setField(azureDatabaseCapabilityService, "storageEdition", "ManagedDisk");
        ReflectionTestUtils.setField(azureDatabaseCapabilityService, "defaultFlexibleInstanceType", "Standard_E4ds_v4");
        ReflectionTestUtils.setField(azureDatabaseCapabilityService, "defaultSingleServerInstanceType", "MO_Gen5_4");
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

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService.databaseCapabilities(cloudCredential, null,
                Map.of(DATABASE_TYPE, AZURE_FLEXIBLE.name()));

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
    void testDatabaseCapabilitiesWhenSingleServerNoRegion() {
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

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService.databaseCapabilities(cloudCredential, null,
                Map.of(DATABASE_TYPE, AZURE_SINGLE_SERVER.name()));

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
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Label), "MO_Gen5_4");
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region1Name), "MO_Gen5_4");
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region2Label), "MO_Gen5_4");
        Assertions.assertEquals(capabilities.getRegionDefaultInstanceTypeMap().get(region2Name), "MO_Gen5_4");
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

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService
                .databaseCapabilities(cloudCredential, Region.region("westus"), Map.of(DATABASE_TYPE, AZURE_FLEXIBLE.name()));

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

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService
                .databaseCapabilities(cloudCredential, Region.region("westus"), Map.of(DATABASE_TYPE, AZURE_FLEXIBLE.name()));

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

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService
                .databaseCapabilities(cloudCredential, Region.region("westus"),  Map.of(DATABASE_TYPE, AZURE_FLEXIBLE.name()));

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
        ReflectionTestUtils.setField(azureDatabaseCapabilityService, "defaultFlexibleInstanceType", null);

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

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService
                .databaseCapabilities(cloudCredential, Region.region("westus"), Map.of(DATABASE_TYPE, AZURE_FLEXIBLE.name()));

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

    @Test
    void testDatabaseStorageCapabilities() {
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
        AzureCoordinate azureCoordinate = azureCoordinate("westus");
        when(azureRegionProvider.getAzureCoordinate(Region.region("westus"))).thenReturn(Optional.of(azureCoordinate));
        FlexibleServerCapability flexibleServerCapability = createFlexibleServerCapability(Map.of("MemoryOptimized",
                        Map.of("serverEdition1", List.of(128L, 256L),
                                "ManagedDisk", List.of(128L, 256L, 512L))));
        when(azureFlexibleServerClient.getFlexibleServerCapability(azureCoordinate)).thenReturn(Optional.of(flexibleServerCapability));

        Optional<PlatformDBStorageCapabilities> capabilities = azureDatabaseCapabilityService.databaseStorageCapabilities(
                cloudCredential, Region.region("westus"));

        Assertions.assertEquals(new TreeSet<>(List.of(128L, 256L, 512L)), capabilities.get().supportedStorageSizeInMb());
    }

    @Test
    void testDatabaseStorageCapabilitiesNoManagedDisk() {
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
        AzureCoordinate azureCoordinate = azureCoordinate("westus");
        when(azureRegionProvider.getAzureCoordinate(Region.region("westus"))).thenReturn(Optional.of(azureCoordinate));
        FlexibleServerCapability flexibleServerCapability = createFlexibleServerCapability(Map.of("MemoryOptimized",
                Map.of("serverEdition1", List.of(128L, 256L))));
        when(azureFlexibleServerClient.getFlexibleServerCapability(azureCoordinate)).thenReturn(Optional.of(flexibleServerCapability));

        Optional<PlatformDBStorageCapabilities> capabilities = azureDatabaseCapabilityService.databaseStorageCapabilities(
                cloudCredential, Region.region("westus"));

        Assertions.assertTrue(capabilities.get().supportedStorageSizeInMb().isEmpty());
    }

    @Test
    void testDatabaseStorageCapabilitiesNoCapability() {
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
        AzureCoordinate azureCoordinate = azureCoordinate("westus");
        when(azureRegionProvider.getAzureCoordinate(Region.region("westus"))).thenReturn(Optional.of(azureCoordinate));
        when(azureFlexibleServerClient.getFlexibleServerCapability(azureCoordinate)).thenReturn(Optional.empty());

        Optional<PlatformDBStorageCapabilities> capabilities = azureDatabaseCapabilityService.databaseStorageCapabilities(
                cloudCredential, Region.region("westus"));

        Assertions.assertTrue(capabilities.get().supportedStorageSizeInMb().isEmpty());
    }

    @Test
    void testDatabaseStorageCapabilitiesNoRegion() {
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureRegionProvider.getAzureCoordinate(Region.region("westus"))).thenReturn(Optional.empty());

        Optional<PlatformDBStorageCapabilities> capabilities = azureDatabaseCapabilityService.databaseStorageCapabilities(
                cloudCredential, Region.region("westus"));

        Assertions.assertTrue(capabilities.isEmpty());
    }

    @Test
    void testDatabaseCapabilitiesWithSupportedServerVersionsToUpgrade() {
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
        Map<Region, AzureCoordinate> regions = Map.of(
                Region.region("westus"), azureCoordinate("westus"),
                Region.region("westus2"), azureCoordinate("westus2"));
        when(azureRegionProvider.filterEnabledRegions(Region.region("westus"))).thenReturn(regions);

        FlexibleServerCapability flexibleServerCapability1 = createFlexibleServerCapabilityForVersionUpgrades(Map.of(
                "14", List.of("15", "16"),
                "15", List.of("16"),
                "16", List.of()
        ));
        FlexibleServerCapability flexibleServerCapability2 = createFlexibleServerCapabilityForVersionUpgrades(Map.of(
                "14", List.of("15", "16", "17"),
                "15", List.of("16", "17"),
                "16", List.of("17")
        ));
        Map<Region, Optional<FlexibleServerCapability>> flexibleServerCapabilityMap = Map.of(
                Region.region("westus"), Optional.of(flexibleServerCapability1),
                Region.region("westus2"), Optional.of(flexibleServerCapability2));
        when(azureFlexibleServerClient.getFlexibleServerCapabilityMap(anyMap())).thenReturn(flexibleServerCapabilityMap);

        PlatformDatabaseCapabilities capabilities = azureDatabaseCapabilityService
                .databaseCapabilities(cloudCredential, Region.region("westus"), Map.of(DATABASE_TYPE, AZURE_FLEXIBLE.name()));

        Map<Region, Map<String, List<String>>> supportedServerVersionsToUpgrade = capabilities.getSupportedServerVersionsToUpgrade();
        Assertions.assertNotNull(supportedServerVersionsToUpgrade);
        Map<String, List<String>> westusServerVersions = supportedServerVersionsToUpgrade.get(Region.region("westus"));
        Assertions.assertNotNull(westusServerVersions);
        Assertions.assertTrue(westusServerVersions.get("14").contains("16"));
        Assertions.assertFalse(westusServerVersions.get("14").contains("14"));
        Assertions.assertFalse(westusServerVersions.get("14").contains("17"));
        Assertions.assertTrue(westusServerVersions.get("15").contains("16"));
        Assertions.assertFalse(westusServerVersions.get("15").contains("17"));
        Assertions.assertFalse(westusServerVersions.get("15").contains("14"));
        Assertions.assertTrue(westusServerVersions.get("16").isEmpty());
        Assertions.assertNull(westusServerVersions.get("17"));
    }

    private AzureCoordinate azureCoordinate(String name) {
        return AzureCoordinateBuilder.builder()
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

    private FlexibleServerCapability createFlexibleServerCapability(Map<String, Map<String, List<Long>>> storageSizes) {
        FlexibleServerCapability flexibleServerCapability = mock(FlexibleServerCapability.class);
        List<FlexibleServerEditionCapability> flexibleServerEditionCapabilities = storageSizes.entrySet().stream()
                .map(entry -> createFlexibleServerEditionCapabilityWithStorage(entry.getKey(), entry.getValue()))
                .toList();
        lenient().when(flexibleServerCapability.supportedServerEditions()).thenReturn(flexibleServerEditionCapabilities);
        return flexibleServerCapability;
    }

    private FlexibleServerEditionCapability createFlexibleServerEditionCapabilityWithStorage(
            String serverEdition, Map<String, List<Long>> storageSizes) {
        FlexibleServerEditionCapability flexibleServerEditionCapability = mock(FlexibleServerEditionCapability.class);
        lenient().when(flexibleServerEditionCapability.name()).thenReturn(serverEdition);
        List<StorageEditionCapability> storageEditionCapabilities = storageSizes.entrySet().stream()
                .map(entry -> createStorageEditionCapability(entry.getKey(), entry.getValue()))
                .toList();
        lenient().when(flexibleServerEditionCapability.supportedStorageEditions()).thenReturn(storageEditionCapabilities);
        return flexibleServerEditionCapability;
    }

    private StorageEditionCapability createStorageEditionCapability(String storageEdition, List<Long> sizes) {
        StorageEditionCapability storageEditionCapability = mock(StorageEditionCapability.class);
        lenient().when(storageEditionCapability.name()).thenReturn(storageEdition);
        List<StorageMbCapability> storageMbCapabilities = sizes.stream()
                .map(size -> {
                    StorageMbCapability storageMbCapability = mock(StorageMbCapability.class);
                    lenient().when(storageMbCapability.storageSizeMb()).thenReturn(size);
                    return storageMbCapability;
                })
                .toList();
        lenient().when(storageEditionCapability.supportedStorageMb()).thenReturn(storageMbCapabilities);
        return storageEditionCapability;
    }

    private FlexibleServerCapability createFlexibleServerCapabilityForVersionUpgrades(Map<String, List<String>> serverVersionUpgrades) {
        FlexibleServerCapability flexibleServerCapability = mock(FlexibleServerCapability.class);
        List<ServerVersionCapability> flexibleServerVersionCapabilities = serverVersionUpgrades.entrySet().stream()
                .map(AzureDatabaseCapabilityServiceTest::createServerVersionCapability)
                .toList();
        lenient().when(flexibleServerCapability.supportedServerVersions()).thenReturn(flexibleServerVersionCapabilities);
        return flexibleServerCapability;
    }

    @NotNull
    private static ServerVersionCapability createServerVersionCapability(Map.Entry<String, List<String>> entry) {
        ServerVersionCapability serverVersionCapability = mock(ServerVersionCapability.class);
        lenient().when(serverVersionCapability.name()).thenReturn(entry.getKey());
        lenient().when(serverVersionCapability.supportedVersionsToUpgrade()).thenReturn(entry.getValue());
        return serverVersionCapability;
    }
}
