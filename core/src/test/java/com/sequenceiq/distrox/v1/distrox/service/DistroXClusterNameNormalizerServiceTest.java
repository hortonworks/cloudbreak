package com.sequenceiq.distrox.v1.distrox.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;

class DistroXClusterNameNormalizerServiceTest {

    private static DistroXClusterNameNormalizerService underTest;

    @BeforeAll
    static void beforeAll() {
        underTest = new DistroXClusterNameNormalizerService();
    }

    @Test
    void removeDeletedTimeStampWhenRegularName() {
        StackViewV4Response stackViewV4Response = generateStackViewV4Response();

        stackViewV4Response.setName("test-dl-name_1691415240478");
        underTest.removeDeletedTimeStampFromName(stackViewV4Response);
        assertEquals("test-dl-name", stackViewV4Response.getName());
    }

    @Test
    void removeDeletedTimeStampWhenWithUnderScoresFromName() {
        StackViewV4Response stackViewV4Response = generateStackViewV4Response();
        stackViewV4Response.setName("test_dl_name_12341_1691415240478");
        underTest.removeDeletedTimeStampFromName(stackViewV4Response);
        assertEquals("test_dl_name_12341", stackViewV4Response.getName());
    }

    @Test
    void removeDeletedTimeStampWhenDoubleTimeStampsInNameFromName() {
        StackViewV4Response stackViewV4Response = generateStackViewV4Response();
        stackViewV4Response.setName("test_dl_name_1234567891012_1691415240478");
        underTest.removeDeletedTimeStampFromName(stackViewV4Response);
        assertEquals("test_dl_name_1234567891012", stackViewV4Response.getName());
    }

    @NotNull
    private static StackViewV4Response generateStackViewV4Response() {
        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setStatus(Status.DELETE_COMPLETED);
        ClusterViewV4Response clsuter = new ClusterViewV4Response();
        stackViewV4Response.setCluster(clsuter);
        return stackViewV4Response;
    }
}