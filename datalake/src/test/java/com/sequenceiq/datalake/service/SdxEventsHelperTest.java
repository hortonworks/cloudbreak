package com.sequenceiq.datalake.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxService;

@ExtendWith(MockitoExtension.class)
class SdxEventsHelperTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:" +
            "6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:datalake:" +
            "6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:cluster:" +
            "6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxEventsHelper sdxEventsHelper;

    @Test
    void testEnsureNonDeletedNonDetachedDatalakeExistsThrowsWhenEmpty() {
        when(sdxService.listSdxByEnvCrn(any())).thenReturn(List.of());

        assertThrows(NotFoundException.class,
                () -> sdxEventsHelper.ensureNonDeletedNonDetachedDatalakeExists(ENVIRONMENT_CRN));
    }

    @Test
    void testEnsureNonDeletedNonDetachedDatalakeExistsDoesNotThrowWhenPresent() {
        when(sdxService.listSdxByEnvCrn(any())).thenReturn(List.of(new SdxCluster()));

        sdxEventsHelper.ensureNonDeletedNonDetachedDatalakeExists(ENVIRONMENT_CRN);
    }

    @Test
    void testGetAvailableDatalakes() {
        SdxCluster cluster = new SdxCluster();
        when(sdxClusterRepository.findByAccountIdAndEnvCrn(
                "460c0d8f-ae8e-4dce-9cd7-2351762eb9ac", ENVIRONMENT_CRN))
                .thenReturn(List.of(cluster));

        List<SdxCluster> result = sdxEventsHelper.getAvailableAndDetachedDatalakes(ENVIRONMENT_CRN);

        assertEquals(1, result.size());
        assertEquals(cluster, result.get(0));
    }

    @Test
    void testGetAvailableDatalakesThrowsOnInvalidCrn() {
        assertThrows(BadRequestException.class,
                () -> sdxEventsHelper.getAvailableAndDetachedDatalakes("not-a-valid-crn"));
    }

    @Test
    void testGetCloudbreakCrnReturnsStackCrnWhenDifferentFromCrn() {
        SdxCluster cluster = new SdxCluster();
        cluster.setCrn(DATALAKE_CRN);
        cluster.setStackCrn(DATAHUB_CRN);

        assertEquals(DATAHUB_CRN, sdxEventsHelper.getCloudbreakCrn(cluster));
    }

    @Test
    void testGetCloudbreakCrnReturnsCrnWhenStackCrnEquals() {
        SdxCluster cluster = new SdxCluster();
        cluster.setCrn(DATALAKE_CRN);
        cluster.setStackCrn(DATALAKE_CRN);

        assertEquals(DATALAKE_CRN, sdxEventsHelper.getCloudbreakCrn(cluster));
    }

    @Test
    void testGetCloudbreakCrnReturnsCrnWhenStackCrnIsNull() {
        SdxCluster cluster = new SdxCluster();
        cluster.setCrn(DATALAKE_CRN);
        cluster.setStackCrn(null);

        assertEquals(DATALAKE_CRN, sdxEventsHelper.getCloudbreakCrn(cluster));
    }
}
