package com.sequenceiq.datalake.service.sdx.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseInstanceTypeV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseInstanceTypesV4Response;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;

@ExtendWith(MockitoExtension.class)
class SdxDatabaseInstanceTypeServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc:environment:env-id";

    private static final String REGION = "us-east-1";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @InjectMocks
    private SdxDatabaseInstanceTypeService underTest;

    @Test
    void filtersByMatchingCpuAndMemory() {
        Set<DatabaseVmTypeResponse> vmTypes = Set.of(
                vmType("db.m5.large", 2, 8.0f, "x86_64"),
                vmType("db.m5.xlarge", 4, 16.0f, "x86_64"),
                vmType("db.r5.large", 2, 16.0f, "x86_64"),
                vmType("db.m6g.large", 2, 8.0f, "arm64"),
                vmType("db.t3.medium", 2, 4.0f, "x86_64"),
                vmType("db.m5.2xlarge", 8, 32.0f, "x86_64")
        );

        List<DatabaseInstanceTypeV4> result = SdxDatabaseInstanceTypeService.filterByDefaultSpec(
                vmTypes, "db.m5.large", List.of());

        assertThat(result).extracting(DatabaseInstanceTypeV4::getName)
                .containsExactly("db.m5.large", "db.m6g.large");
        assertThat(result).filteredOn(DatabaseInstanceTypeV4::isDefaultType)
                .extracting(DatabaseInstanceTypeV4::getName)
                .containsExactly("db.m5.large");
        assertThat(result).allSatisfy(item -> {
            assertThat(item.getCpu()).isEqualTo(2);
            assertThat(item.getMemoryInGb()).isEqualTo(8.0f);
        });
    }

    @Test
    void returnsFallbackListWhenVmTypesEmpty() {
        List<DatabaseInstanceTypeV4> result = SdxDatabaseInstanceTypeService.filterByDefaultSpec(
                Set.of(), "db.m5.large", List.of("db.m5.xlarge", "db.r5.large"));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("db.m5.large");
        assertThat(result.get(0).isDefaultType()).isTrue();
        assertThat(result.get(1).getName()).isEqualTo("db.m5.xlarge");
        assertThat(result.get(1).isDefaultType()).isFalse();
    }

    @Test
    void returnsFallbackWhenDefaultTypeHasNoMetadata() {
        Set<DatabaseVmTypeResponse> vmTypes = Set.of(
                vmTypeWithoutMeta("db.m5.large"),
                vmType("db.m5.xlarge", 4, 16.0f, "x86_64")
        );

        List<DatabaseInstanceTypeV4> result = SdxDatabaseInstanceTypeService.filterByDefaultSpec(
                vmTypes, "db.m5.large", List.of("db.m5.xlarge"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("db.m5.large");
        assertThat(result.get(0).isDefaultType()).isTrue();
    }

    @Test
    void returnsFallbackWhenDefaultTypeNotInVmTypes() {
        Set<DatabaseVmTypeResponse> vmTypes = Set.of(
                vmType("db.m5.xlarge", 4, 16.0f, "x86_64")
        );

        List<DatabaseInstanceTypeV4> result = SdxDatabaseInstanceTypeService.filterByDefaultSpec(
                vmTypes, "db.m5.large", List.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("db.m5.large");
        assertThat(result.get(0).isDefaultType()).isTrue();
    }

    @Test
    void returnsEmptyWhenNoDefaultAndNoFallback() {
        List<DatabaseInstanceTypeV4> result = SdxDatabaseInstanceTypeService.filterByDefaultSpec(
                Set.of(), null, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void resultIsSortedByName() {
        Set<DatabaseVmTypeResponse> vmTypes = Set.of(
                vmType("db.r5.large", 2, 8.0f, "x86_64"),
                vmType("db.m5.large", 2, 8.0f, "x86_64"),
                vmType("db.c5.large", 2, 8.0f, "x86_64")
        );

        List<DatabaseInstanceTypeV4> result = SdxDatabaseInstanceTypeService.filterByDefaultSpec(
                vmTypes, "db.m5.large", List.of());

        assertThat(result).extracting(DatabaseInstanceTypeV4::getName)
                .containsExactly("db.c5.large", "db.m5.large", "db.r5.large");
    }

    @Test
    void listDatabaseInstanceTypesCallsEnvironmentService() {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setCloudPlatform("AWS");
        LocationResponse location = new LocationResponse();
        location.setName(REGION);
        env.setLocation(location);
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(env);

        PlatformDatabaseCapabilitiesResponse capabilities = new PlatformDatabaseCapabilitiesResponse(
                Map.of(), Map.of(REGION, "db.m5.large"), Map.of(REGION, List.of("db.m5.xlarge")),
                Map.of(), null, Map.of(REGION, Set.of(
                vmType("db.m5.large", 2, 8.0f, "x86_64"),
                vmType("db.m5d.large", 2, 8.0f, "x86_64")
        )));
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(eq(ENV_CRN), eq(REGION), eq("AWS"),
                isNull(), eq(DatabaseCapabilityType.DEFAULT), isNull()))
                .thenReturn(capabilities);

        DatabaseInstanceTypesV4Response response = underTest.listDatabaseInstanceTypes(ENV_CRN, null, null);

        assertThat(response.getDefaultInstanceType()).isEqualTo("db.m5.large");
        assertThat(response.getInstanceTypes()).hasSize(2);
    }

    @Test
    void azureDefaultsToFlexibleCapabilityType() {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setCloudPlatform("AZURE");
        LocationResponse location = new LocationResponse();
        location.setName("westus2");
        env.setLocation(location);
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(env);

        PlatformDatabaseCapabilitiesResponse capabilities = new PlatformDatabaseCapabilitiesResponse();
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(eq(ENV_CRN), eq("westus2"), eq("AZURE"),
                isNull(), eq(DatabaseCapabilityType.AZURE_FLEXIBLE), isNull()))
                .thenReturn(capabilities);

        underTest.listDatabaseInstanceTypes(ENV_CRN, null, null);
    }

    private static DatabaseVmTypeResponse vmType(String name, Integer cpu, Float memory, String architecture) {
        DatabaseVmTypeMetaJson meta = new DatabaseVmTypeMetaJson();
        Map<String, Object> props = new HashMap<>();
        props.put("Cpu", cpu);
        props.put("Memory", memory);
        if (architecture != null) {
            props.put("Architecture", architecture);
        }
        meta.setProperties(props);
        return new DatabaseVmTypeResponse(name, meta);
    }

    private static DatabaseVmTypeResponse vmTypeWithoutMeta(String name) {
        return new DatabaseVmTypeResponse(name);
    }
}
