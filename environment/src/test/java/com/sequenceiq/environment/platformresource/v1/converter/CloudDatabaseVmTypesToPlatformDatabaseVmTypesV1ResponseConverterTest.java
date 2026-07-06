package com.sequenceiq.environment.platformresource.v1.converter;

import static com.sequenceiq.cloudbreak.cloud.model.DatabaseVmType.databaseVmType;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudDatabaseVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmType;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmTypeMeta.DatabaseVmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVirtualMachinesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse;

@ExtendWith(MockitoExtension.class)
class CloudDatabaseVmTypesToPlatformDatabaseVmTypesV1ResponseConverterTest {

    private static final String REGION_NAME = "us-east-1";

    private static final String DEFAULT_VM = "db.m5.large";

    @Spy
    private DatabaseVmTypeToVmTypeResponseConverter databaseVmTypeToVmTypeResponseConverter;

    @InjectMocks
    private CloudDatabaseVmTypesToPlatformDatabaseVmTypesV1ResponseConverter underTest;

    @Test
    void convertShouldMapRegionsAndVmTypes() {
        Region region = region(REGION_NAME);
        DatabaseVmType vm1 = databaseVmType("db.m5.large", DatabaseVmTypeMetaBuilder.builder().withCpuAndMemory(2, 8.0f).create());
        DatabaseVmType vm2 = databaseVmType("db.m5.xlarge", DatabaseVmTypeMetaBuilder.builder().withCpuAndMemory(4, 16.0f).create());

        Map<Region, Set<DatabaseVmType>> vmResponses = new HashMap<>();
        vmResponses.put(region, Set.of(vm1, vm2));
        Map<Region, String> defaults = new HashMap<>();
        defaults.put(region, DEFAULT_VM);
        CloudDatabaseVmTypes source = new CloudDatabaseVmTypes(vmResponses, defaults);

        PlatformDatabaseVmtypesResponse result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getDatabaseVmTypes()).containsKey(REGION_NAME);
        DatabaseVirtualMachinesResponse regionResponse = result.getDatabaseVmTypes().get(REGION_NAME);
        assertThat(regionResponse.getVirtualMachines()).hasSize(2);
        assertThat(regionResponse.getVirtualMachines())
                .extracting(VmTypeResponse::getValue)
                .containsExactlyInAnyOrder("db.m5.large", "db.m5.xlarge");
        assertThat(regionResponse.getDefaultVirtualMachine().getValue()).isEqualTo(DEFAULT_VM);
    }

    @Test
    void convertShouldHandleEmptyInput() {
        CloudDatabaseVmTypes source = new CloudDatabaseVmTypes(new HashMap<>(), new HashMap<>());

        PlatformDatabaseVmtypesResponse result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getDatabaseVmTypes()).isEmpty();
    }

    @Test
    void convertShouldHandleMultipleRegions() {
        Region region1 = region("us-east-1");
        Region region2 = region("eu-west-1");
        DatabaseVmType vm1 = databaseVmType("db.m5.large", DatabaseVmTypeMetaBuilder.builder().withCpuAndMemory(2, 8.0f).create());
        DatabaseVmType vm2 = databaseVmType("db.r5.large", DatabaseVmTypeMetaBuilder.builder().withCpuAndMemory(2, 16.0f).create());

        Map<Region, Set<DatabaseVmType>> vmResponses = new HashMap<>();
        vmResponses.put(region1, Set.of(vm1));
        vmResponses.put(region2, Set.of(vm2));
        Map<Region, String> defaults = new HashMap<>();
        defaults.put(region1, "db.m5.large");
        defaults.put(region2, "db.r5.large");
        CloudDatabaseVmTypes source = new CloudDatabaseVmTypes(vmResponses, defaults);

        PlatformDatabaseVmtypesResponse result = underTest.convert(source);

        assertThat(result.getDatabaseVmTypes()).hasSize(2);
        assertThat(result.getDatabaseVmTypes()).containsKeys("us-east-1", "eu-west-1");
    }
}
