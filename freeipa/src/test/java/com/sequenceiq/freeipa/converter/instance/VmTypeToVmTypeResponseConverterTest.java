package com.sequenceiq.freeipa.converter.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.VmTypeMetaJson;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.VmTypeResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.VolumeParameterConfigResponse;

public class VmTypeToVmTypeResponseConverterTest {

    private final VmTypeToVmTypeResponseConverter underTest = new VmTypeToVmTypeResponseConverter();

    @Test
    public void testConvert() {
        VmTypeMeta vmTypeMeta = VmTypeMeta.VmTypeMetaBuilder.builder()
                .withAutoAttachedConfig(1, 2, 3, 4)
                .withEphemeralConfig(2, 3, 4, 5)
                .withMagneticConfig(3, 4, 5, 6)
                .withSsdConfig(4, 5, 6, 7)
                .withSt1Config(5, 6, 7, 8)
                .withCpuAndMemory(100, 32)
                .withPrice(100.0)
                .withMaximumPersistentDisksSizeGb(5000L)
                .withVolumeEncryptionSupport(false)
                .withProperty("custom", "value")
                .create();
        VmType vmType = VmType.vmTypeWithMeta("large", vmTypeMeta, false);
        VmTypeResponse vmTypeResponse = underTest.convert(vmType);

        assertEquals("large", vmTypeResponse.getValue());
        VmTypeMetaJson vmTypeMetaJson = vmTypeResponse.getVmTypeMetaJson();

        Map<String, Object> properties = vmTypeMetaJson.getProperties();
        assertThat(properties.entrySet()).extracting(Map.Entry::getKey, Map.Entry::getValue)
                .containsOnly(tuple("Cpu", 100),
                        tuple("Memory", 32),
                        tuple("Price", "100.0"),
                        tuple("maximumPersistentDisksSizeGb", 5000L),
                        tuple("EncryptionSupported", false),
                        tuple("custom", "value"));

        List<VolumeParameterConfigResponse> configs = vmTypeMetaJson.getConfigs();
        assertThat(configs).hasSize(5);
        assertThat(configs).extracting(VolumeParameterConfigResponse::getVolumeParameterType,
                        VolumeParameterConfigResponse::getMinimumSize,
                        VolumeParameterConfigResponse::getMaximumSize,
                        VolumeParameterConfigResponse::getMinimumNumber,
                        VolumeParameterConfigResponse::getMaximumNumber)
                .containsOnly(tuple("AUTO_ATTACHED", 1, 2, 3, 4),
                        tuple("EPHEMERAL", 2, 3, 4, 5),
                        tuple("MAGNETIC", 3, 4, 5, 6),
                        tuple("SSD", 4, 5, 6, 7),
                        tuple("ST1", 5, 6, 7, 8));
    }

}