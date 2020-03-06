package com.sequenceiq.datalake.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

class SdxRepairSettingsTest {

    @Test
    void throwsExceptionWhenBothParametersAreSpecified() {
        SdxRepairRequest request = new SdxRepairRequest();
        request.setHostGroupName("hostgroup1");
        request.setHostGroupNames(List.of("hg1", "hg2"));
        assertThrows(BadRequestException.class, () -> SdxRepairSettings.from(request));
    }

    @Test
    void setsCorrectHostGroupWhenHostGroupNamesSpecified() {
        SdxRepairRequest request = new SdxRepairRequest();
        request.setHostGroupNames(List.of("hg1", "hg2"));
        SdxRepairSettings settings = SdxRepairSettings.from(request);
        assertEquals("hg1", settings.getHostGroupNames().get(0));
    }

    @Test
    void setsCorrectHostGroupWhenHostGroupNameSpecified() {
        SdxRepairRequest request = new SdxRepairRequest();
        request.setHostGroupName("hostgroup1");
        SdxRepairSettings settings = SdxRepairSettings.from(request);
        assertEquals("hostgroup1", settings.getHostGroupNames().get(0));
    }
}