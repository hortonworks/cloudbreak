package com.sequenceiq.environment.platformresource.v1.converter;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VolumeParameterConfigResponse;

class VmTypeToVmTypeV1ResponseConverterTest {

    private static final String VM_TYPE = "vmType";

    private static final String AVAILABILITY_ZONES = "AvailabilityZones";

    private VmTypeToVmTypeV1ResponseConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new VmTypeToVmTypeV1ResponseConverter();
    }

    @Test
    void convertTest() {
        VmTypeMeta meta = VmTypeMetaBuilder.builder()
                .withAutoAttachedConfig(1, 2, 3, 4)
                .withEphemeralConfig(5, 6, 7, 8)
                .withMagneticConfig(9, 10, 11, 12)
                .withSsdConfig(13, 14, 15, 16)
                .withSt1Config(17, 18, 19, 20)
                .withCpuAndMemory(100, 200)
                .withAvailabilityZones(List.of("az-1", "az-2"))
                .create();
        VmType vmType = VmType.vmTypeWithMeta(VM_TYPE, meta, null);

        VmTypeResponse vmTypeResponse = underTest.convert(vmType);

        assertThat(vmTypeResponse).isNotNull();
        assertThat(vmTypeResponse.getValue()).isEqualTo(VM_TYPE);

        VmTypeMetaJson vmTypeMetaJson = vmTypeResponse.getVmTypeMetaJson();
        assertThat(vmTypeMetaJson).isNotNull();
        assertThat(vmTypeMetaJson.getProperties())
                .isEqualTo(Map.ofEntries(entry("Cpu", 100), entry("Memory", 200), entry(AVAILABILITY_ZONES, List.of("az-1", "az-2"))));

        List<VolumeParameterConfigResponse> configs = vmTypeMetaJson.getConfigs();
        assertThat(configs).isNotNull();
        assertThat(configs).hasSize(5);
        validateVolumeParameterConfigResponse(configs, 0, 1, 2, 3, 4, "AUTO_ATTACHED");
        validateVolumeParameterConfigResponse(configs, 1, 5, 6, 7, 8, "EPHEMERAL");
        validateVolumeParameterConfigResponse(configs, 2, 9, 10, 11, 12, "MAGNETIC");
        validateVolumeParameterConfigResponse(configs, 3, 13, 14, 15, 16, "SSD");
        validateVolumeParameterConfigResponse(configs, 4, 17, 18, 19, 20, "ST1");
    }

    void validateVolumeParameterConfigResponse(List<VolumeParameterConfigResponse> configs, int index, int minimumSizeExpected, int maximumSizeExpected,
            int minimumNumberExpected, int maximumNumberExpected, String volumeParameterTypeExpected) {
        VolumeParameterConfigResponse config = configs.get(index);
        assertThat(config).isNotNull();
        assertThat(config.getVolumeParameterType()).isEqualTo(volumeParameterTypeExpected);
        assertThat(config.getMinimumSize()).isEqualTo(minimumSizeExpected);
        assertThat(config.getMaximumSize()).isEqualTo(maximumSizeExpected);
        assertThat(config.getMinimumNumber()).isEqualTo(minimumNumberExpected);
        assertThat(config.getMaximumNumber()).isEqualTo(maximumNumberExpected);
    }

    @Test
    void convertTestWhenEmpty() {
        VmTypeMeta meta = VmTypeMetaBuilder.builder().create();
        VmType vmType = VmType.vmTypeWithMeta(VM_TYPE, meta, null);

        VmTypeResponse vmTypeResponse = underTest.convert(vmType);

        validateEmpty(vmTypeResponse);
    }

    private void validateEmpty(VmTypeResponse vmTypeResponse) {
        assertThat(vmTypeResponse).isNotNull();
        assertThat(vmTypeResponse.getValue()).isEqualTo(VM_TYPE);

        VmTypeMetaJson vmTypeMetaJson = vmTypeResponse.getVmTypeMetaJson();
        assertThat(vmTypeMetaJson).isNotNull();
        assertThat(vmTypeMetaJson.getProperties()).isEqualTo(Map.ofEntries(entry(AVAILABILITY_ZONES, List.of())));

        List<VolumeParameterConfigResponse> configs = vmTypeMetaJson.getConfigs();
        assertThat(configs).isNotNull();
        assertThat(configs).isEmpty();
    }

    @Test
    void convertTestWhenNullAvailabilityZones() {
        VmTypeMeta meta = VmTypeMetaBuilder.builder()
                .withAvailabilityZones(null)
                .create();
        VmType vmType = VmType.vmTypeWithMeta(VM_TYPE, meta, null);

        VmTypeResponse vmTypeResponse = underTest.convert(vmType);

        validateEmpty(vmTypeResponse);
    }

}