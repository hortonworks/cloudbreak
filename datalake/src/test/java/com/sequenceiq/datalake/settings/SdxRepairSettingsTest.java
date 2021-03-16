package com.sequenceiq.datalake.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

class SdxRepairSettingsTest {

    @Test
    void throwsExceptionWhenNoParamsAreSpecified() {
        SdxRepairRequest request = new SdxRepairRequest();
        request.setHostGroupName("");
        request.setHostGroupNames(List.of());
        request.setNodesIds(List.of());
        BadRequestException exception = assertThrows(BadRequestException.class, () -> SdxRepairSettings.from(request));
        assertEquals("Please select the repairable host groups or nodes.", exception.getMessage());
    }

    @Test
    void throwsExceptionWhenHostGroupAndHostGroupsAreSpecified() {
        SdxRepairRequest request = new SdxRepairRequest();
        request.setHostGroupName("hostgroup1");
        request.setHostGroupNames(List.of("hg1", "hg2"));
        BadRequestException exception = assertThrows(BadRequestException.class, () -> SdxRepairSettings.from(request));
        assertEquals("Please select one host group ('hostGroupName'), multiple host groups ('hostGroupNames'), or nodes ('nodesIds').",
                exception.getMessage());
    }

    @Test
    void throwsExceptionWhenHostGroupAndNodeIdsAreSpecified() {
        SdxRepairRequest request = new SdxRepairRequest();
        request.setHostGroupName("hg1");
        request.setNodesIds(List.of("node1", "node2"));
        BadRequestException exception = assertThrows(BadRequestException.class, () -> SdxRepairSettings.from(request));
        assertEquals("Please select one host group ('hostGroupName'), multiple host groups ('hostGroupNames'), or nodes ('nodesIds').",
                exception.getMessage());
    }

    @Test
    void throwsExceptionWhenHostGroupsAndNodeIdsAreSpecified() {
        SdxRepairRequest request = new SdxRepairRequest();
        request.setHostGroupNames(List.of("hg1", "hg2"));
        request.setNodesIds(List.of("node1", "node2"));
        BadRequestException exception = assertThrows(BadRequestException.class, () -> SdxRepairSettings.from(request));
        assertEquals("Please select one host group ('hostGroupName'), multiple host groups ('hostGroupNames'), or nodes ('nodesIds').",
                exception.getMessage());
    }

    @Test
    void setsCorrectHostGroupWhenHostGroupNamesSpecified() {
        SdxRepairRequest request = new SdxRepairRequest();
        request.setHostGroupNames(List.of("hg1", "hg2"));
        SdxRepairSettings settings = SdxRepairSettings.from(request);
        assertEquals(List.of("hg1", "hg2"), settings.getHostGroupNames());
        assertEquals(List.of(), settings.getNodeIds());
    }

    @Test
    void setsCorrectHostGroupWhenHostGroupNameSpecified() {
        SdxRepairRequest request = new SdxRepairRequest();
        request.setHostGroupName("hostgroup1");
        SdxRepairSettings settings = SdxRepairSettings.from(request);
        assertEquals(List.of("hostgroup1"), settings.getHostGroupNames());
        assertEquals(List.of(), settings.getNodeIds());
    }

    @Test
    void setsCorrectNodeIdsWhenNodeIdsAreSpecified() {
        SdxRepairRequest request = new SdxRepairRequest();
        request.setNodesIds(List.of("node1", "node2"));
        SdxRepairSettings settings = SdxRepairSettings.from(request);
        assertEquals(List.of("node1", "node2"), settings.getNodeIds());
        assertEquals(List.of(), settings.getHostGroupNames());
    }
}