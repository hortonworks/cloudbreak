package com.sequenceiq.datalake.controller.sdx;

import static org.junit.Assert.assertThrows;
import static org.springframework.util.Assert.notNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

class SdxClusterConverterTest {

    @Test
    void sdxClusterStatusConverter() {
        for (SdxClusterStatusResponse s : SdxClusterStatusResponse.values()) {
            notNull(DatalakeStatusEnum.valueOf(s.name()), s.name());
        }
        for (DatalakeStatusEnum value : DatalakeStatusEnum.values()) {
            notNull(SdxClusterStatusResponse.valueOf(value.name()), value.name());
        }
        assertThrows("null Response conversion", NullPointerException.class, () -> SdxClusterStatusResponse.valueOf(null));
        assertThrows("null Status conversion", NullPointerException.class, () -> DatalakeStatusEnum.valueOf(null));

    }
}