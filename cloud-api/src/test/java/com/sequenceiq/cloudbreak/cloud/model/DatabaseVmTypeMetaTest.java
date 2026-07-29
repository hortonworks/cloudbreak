package com.sequenceiq.cloudbreak.cloud.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmTypeMeta.DatabaseVmTypeMetaBuilder;
import com.sequenceiq.common.model.Architecture;

class DatabaseVmTypeMetaTest {

    @Test
    void builderShouldSetCpuAndMemoryFromIntegerAndFloat() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withCpuAndMemory(4, 16.0f)
                .create();

        assertThat(meta.getCPU()).isEqualTo(4);
        assertThat(meta.getMemoryInGb()).isEqualTo(16.0f);
    }

    @Test
    void builderShouldSetCpuAndMemoryFromIntPrimitives() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withCpuAndMemory(8, 32f)
                .create();

        assertThat(meta.getCPU()).isEqualTo(8);
        assertThat(meta.getMemoryInGb()).isEqualTo(32.0f);
    }

    @Test
    void builderShouldSetMemoryOnly() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withMemory(14.0f)
                .create();

        assertThat(meta.getCPU()).isNull();
        assertThat(meta.getMemoryInGb()).isEqualTo(14.0f);
    }

    @Test
    void builderShouldSetArchitecture() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withArchitecture(Architecture.ARM64)
                .create();

        assertThat(meta.getArchitecture()).isEqualTo(Architecture.ARM64);
    }

    @Test
    void defaultArchitectureShouldBeX86() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder().create();

        assertThat(meta.getArchitecture()).isEqualTo(Architecture.X86_64);
    }

    @Test
    void builderShouldSetAvailabilityZones() {
        List<String> azs = List.of("az-1", "az-2", "az-3");
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withAvailabilityZones(azs)
                .create();

        assertThat(meta.getAvailabilityZones()).containsExactly("az-1", "az-2", "az-3");
    }

    @Test
    void builderShouldSetPrice() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withPrice(0.456)
                .create();

        assertThat(meta.getProperties()).containsEntry(DatabaseVmTypeMeta.PRICE, "0.456");
    }

    @Test
    void builderShouldSetCustomProperty() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withProperty("customKey", "customValue")
                .create();

        assertThat(meta.getProperties()).containsEntry("customKey", "customValue");
    }

    @Test
    void builderShouldHandleNullAvailabilityZones() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder()
                .withAvailabilityZones(null)
                .create();

        assertThat(meta.getAvailabilityZones()).isNull();
    }

    @Test
    void getCPUShouldReturnNullWhenNotSet() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder().create();

        assertThat(meta.getCPU()).isNull();
    }

    @Test
    void getMemoryInGbShouldReturnNullWhenNotSet() {
        DatabaseVmTypeMeta meta = DatabaseVmTypeMetaBuilder.builder().create();

        assertThat(meta.getMemoryInGb()).isNull();
    }
}
