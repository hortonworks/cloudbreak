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
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeResponse;

class DatabaseVmTypeToDatabaseVmTypeResponseConverterTest {

    private static final String VM_TYPE_NAME = "db.m5.large";

    private DatabaseVmTypeToDatabaseVmTypeResponseConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new DatabaseVmTypeToDatabaseVmTypeResponseConverter();
    }

    @Test
    void convertShouldMapValueAndProperties() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withCpuAndMemory(4, 16.0f)
                .withArchitecture(Architecture.X86_64)
                .withAvailabilityZones(List.of("us-east-1a", "us-east-1b"))
                .create();
        DatabaseVmType source = databaseVmType(VM_TYPE_NAME, meta);

        DatabaseVmTypeResponse result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo(VM_TYPE_NAME);
        DatabaseVmTypeMetaJson metaJson = result.getDatabaseVmTypeMetaJson();
        assertThat(metaJson).isNotNull();
        assertThat(metaJson.getProperties()).containsEntry(DatabaseVmTypeMeta.CPU, 4);
        assertThat(metaJson.getProperties()).containsEntry(DatabaseVmTypeMeta.MEMORY, 16.0f);
        assertThat(metaJson.getProperties()).containsEntry(DatabaseVmTypeMeta.ARCHITECTURE, Architecture.X86_64);
    }

    @Test
    void convertShouldHandleMinimalMeta() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withMemory(8.0f)
                .create();
        DatabaseVmType source = databaseVmType("db.t3.medium", meta);

        DatabaseVmTypeResponse result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("db.t3.medium");
        assertThat(result.getDatabaseVmTypeMetaJson().getProperties())
                .containsEntry(DatabaseVmTypeMeta.MEMORY, 8.0f)
                .doesNotContainKey(DatabaseVmTypeMeta.CPU);
    }

    @Test
    void convertShouldHandleEmptyProperties() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder().create();
        DatabaseVmType source = databaseVmType("db.custom", meta);

        DatabaseVmTypeResponse result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("db.custom");
        assertThat(result.getDatabaseVmTypeMetaJson()).isNotNull();
        assertThat(result.getDatabaseVmTypeMetaJson().getProperties()).isNotEmpty();
    }

    @Test
    void convertShouldMapPriceProperty() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withCpuAndMemory(2, 4.0f)
                .withPrice(0.123)
                .create();
        DatabaseVmType source = databaseVmType("db.t3.small", meta);

        DatabaseVmTypeResponse result = underTest.convert(source);

        assertThat(result.getDatabaseVmTypeMetaJson().getProperties())
                .containsEntry(DatabaseVmTypeMeta.PRICE, "0.123");
    }
}
