package com.sequenceiq.environment.platformresource.v1.converter;

import static com.sequenceiq.cloudbreak.cloud.model.DatabaseVmType.databaseVmType;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmType;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmTypeMeta.DatabaseVmTypeMetaBuilder;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse;

class DatabaseVmTypeToVmTypeResponseConverterTest {

    private static final String VM_TYPE_NAME = "db.m5.xlarge";

    private DatabaseVmTypeToVmTypeResponseConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new DatabaseVmTypeToVmTypeResponseConverter();
    }

    @Test
    void convertShouldMapValueAndMetaProperties() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withCpuAndMemory(8, 32.0f)
                .withArchitecture(Architecture.ARM64)
                .withAvailabilityZones(List.of("eu-west-1a"))
                .create();
        DatabaseVmType source = databaseVmType(VM_TYPE_NAME, meta);

        VmTypeResponse result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo(VM_TYPE_NAME);
        VmTypeMetaJson metaJson = result.getVmTypeMetaJson();
        assertThat(metaJson).isNotNull();
        assertThat(metaJson.getProperties()).containsEntry(DatabaseVmTypeMeta.CPU, 8);
        assertThat(metaJson.getProperties()).containsEntry(DatabaseVmTypeMeta.MEMORY, 32.0f);
        assertThat(metaJson.getProperties()).containsEntry(DatabaseVmTypeMeta.ARCHITECTURE, Architecture.ARM64);
    }

    @Test
    void convertShouldHandleEmptyMeta() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder().create();
        DatabaseVmType source = databaseVmType("db.r5.large", meta);

        VmTypeResponse result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("db.r5.large");
        assertThat(result.getVmTypeMetaJson()).isNotNull();
        assertThat(result.getVmTypeMetaJson().getProperties()).isNotEmpty();
    }
}
