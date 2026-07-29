package com.sequenceiq.environment.platformresource.v1.converter;

import static com.sequenceiq.cloudbreak.cloud.model.DatabaseVmType.databaseVmType;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseAvailabiltyType;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmType;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmTypeMeta.DatabaseVmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;

@ExtendWith(MockitoExtension.class)
class DatabaseCapabilitiesToPlatformDatabaseCapabilitiesResponseConverterTest {

    @Spy
    private DatabaseVmTypeToDatabaseVmTypeResponseConverter databaseVmTypeToDatabaseVmTypeResponseConverter;

    @InjectMocks
    private DatabaseCapabilitiesToPlatformDatabaseCapabilitiesResponseConverter converter;

    @Test
    void testConvert() {
        Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions = new HashMap<>();
        enabledRegions.put(DatabaseAvailabiltyType.databaseAvailabiltyType("big"), new ArrayList<>());
        enabledRegions.put(DatabaseAvailabiltyType.databaseAvailabiltyType("small"), new ArrayList<>());
        Map<Region, String> regionDefaultInstanceTypeMap = new HashMap<>();
        regionDefaultInstanceTypeMap.put(region("region1"), "big");
        regionDefaultInstanceTypeMap.put(region("region2"), "big");
        regionDefaultInstanceTypeMap.put(region("region3"), "big");
        PlatformDatabaseCapabilities source = new PlatformDatabaseCapabilities(
                enabledRegions,
                regionDefaultInstanceTypeMap,
                new HashMap<>(),
                "17"
        );
        PlatformDatabaseCapabilitiesResponse response = converter.convert(source);

        assertThat(source.getEnabledRegions().size()).isEqualTo(response.getIncludedRegions().size());
        assertThat(source.getRegionDefaultInstanceTypeMap().size()).isEqualTo(response.getRegionDefaultInstances().size());
        assertThat(source.getLatestDatabaseEngineVersion()).isEqualTo(response.getLatestDatabaseEngineVersion());
        assertThat(response.getDatabaseVmTypes()).isEmpty();
    }

    @Test
    void testConvertWithRegionAvailableInstanceTypes() {
        Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions = new HashMap<>();
        Map<Region, String> regionDefaultInstanceTypeMap = new HashMap<>();
        regionDefaultInstanceTypeMap.put(region("us-east-1"), "db.m5.large");

        DatabaseVmType vm1 = databaseVmType("db.m5.large", DatabaseVmTypeMetaBuilder.builder().withCpuAndMemory(2, 8.0f).create());
        DatabaseVmType vm2 = databaseVmType("db.m5.xlarge", DatabaseVmTypeMetaBuilder.builder().withCpuAndMemory(4, 16.0f).create());
        Map<Region, Set<DatabaseVmType>> regionAvailableInstanceTypes = new HashMap<>();
        regionAvailableInstanceTypes.put(region("us-east-1"), Set.of(vm1, vm2));

        PlatformDatabaseCapabilities source = new PlatformDatabaseCapabilities(
                enabledRegions,
                regionDefaultInstanceTypeMap,
                new HashMap<>(),
                "16",
                regionAvailableInstanceTypes
        );

        PlatformDatabaseCapabilitiesResponse response = converter.convert(source);

        assertThat(response.getDatabaseVmTypes()).hasSize(1);
        assertThat(response.getDatabaseVmTypes()).containsKey("us-east-1");
        assertThat(response.getDatabaseVmTypes().get("us-east-1")).hasSize(2);
        assertThat(response.getRegionDefaultInstances()).containsEntry("us-east-1", "db.m5.large");
        assertThat(response.getLatestDatabaseEngineVersion()).isEqualTo("16");
    }

    @Test
    void testConvertWithEmptyRegionAvailableInstanceTypes() {
        Map<Region, Set<DatabaseVmType>> regionAvailableInstanceTypes = new HashMap<>();
        PlatformDatabaseCapabilities source = new PlatformDatabaseCapabilities(
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                null,
                regionAvailableInstanceTypes
        );

        PlatformDatabaseCapabilitiesResponse response = converter.convert(source);

        assertThat(response.getDatabaseVmTypes()).isEmpty();
        assertThat(response.getLatestDatabaseEngineVersion()).isNull();
    }
}
