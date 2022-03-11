package com.sequenceiq.datalake.service.sdx.attach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

public class SdxDetachServiceTest {
    private static final Long TEST_CLUSTER_ID = 1L;

    private static final String TEST_CLUSTER_NAME = "test";

    private static final String NEW_TEST_CLUSTER_NAME = "test-2";

    private static final String TEST_CLUSTER_CRN = "testCRN";

    private static final String NEW_TEST_CLUSTER_CRN = "newCRN";

    private static final String TEST_CLUSTER_ENV_CRN = "envCrn";

    private SdxCluster testCluster;

    @Mock
    private SdxService mockSdxService;

    @Mock
    private RegionAwareCrnGenerator mockRegionAwareCrnGenerator;

    @Mock
    private SdxDetachNameGenerator mockSdxDetachNameGenerator;

    @Mock
    private SdxStatusService mockSdxStatusService;

    @Mock
    private StackV4Endpoint mockStackV4Endpoint;

    @Mock
    private DatabaseServerV4Endpoint mockRedbeamsServerEndpoint;

    @InjectMocks
    private SdxAttachDetachUtils mockSdxAttachDetachUtils = spy(SdxAttachDetachUtils.class);

    @InjectMocks
    private SdxDetachService sdxDetachService;

    @BeforeEach
    void setUp() {
        testCluster = new SdxCluster();
        testCluster.setEnvCrn(TEST_CLUSTER_ENV_CRN);
        testCluster.setId(TEST_CLUSTER_ID);
        testCluster.setClusterName(TEST_CLUSTER_NAME);
        testCluster.setCrn(TEST_CLUSTER_CRN);
        testCluster.setDetached(false);
        openMocks(this);
    }

    @Test
    void testDetachCluster() {
        when(mockSdxService.getById(eq(TEST_CLUSTER_ID))).thenReturn(testCluster);
        when(mockRegionAwareCrnGenerator.generateCrnStringWithUuid(any(), any())).thenReturn(NEW_TEST_CLUSTER_CRN);
        when(mockSdxDetachNameGenerator.generateDetachedClusterName(any())).thenReturn(NEW_TEST_CLUSTER_NAME);
        when(mockSdxService.save(any())).thenReturn(testCluster);

        SdxCluster detachClusterResult = sdxDetachService.detachCluster(testCluster.getId());

        assertEquals(detachClusterResult.getCrn(), NEW_TEST_CLUSTER_CRN);
        assertEquals(detachClusterResult.getOriginalCrn(), TEST_CLUSTER_CRN);
        assertEquals(detachClusterResult.getClusterName(), NEW_TEST_CLUSTER_NAME);
        assertTrue(detachClusterResult.isDetached());

        verify(mockSdxStatusService).setStatusForDatalakeAndNotify(
                eq(DatalakeStatusEnum.STOPPED), eq("Datalake detach in progress."), eq(TEST_CLUSTER_ID)
        );
    }

    @Test
    void testDetachStack() throws Exception {
        testCluster.setClusterName(NEW_TEST_CLUSTER_NAME);
        testCluster.setCrn(NEW_TEST_CLUSTER_CRN);
        sdxDetachService.detachStack(testCluster, TEST_CLUSTER_NAME);
        verify(mockStackV4Endpoint).updateNameAndCrn(
                eq(0L), eq(TEST_CLUSTER_NAME), any(), eq(NEW_TEST_CLUSTER_NAME), eq(NEW_TEST_CLUSTER_CRN)
        );
    }

    @Test
    void testDetachExternalDatabase() throws Exception {
        testCluster.setOriginalCrn(TEST_CLUSTER_CRN);
        testCluster.setCrn(NEW_TEST_CLUSTER_CRN);
        sdxDetachService.detachExternalDatabase(testCluster);
        verify(mockRedbeamsServerEndpoint).updateClusterCrn(
                eq(TEST_CLUSTER_ENV_CRN), eq(TEST_CLUSTER_CRN), eq(NEW_TEST_CLUSTER_CRN), any()
        );
    }

    @Test
    void testMarkAsDetached() {
        sdxDetachService.markAsDetached(TEST_CLUSTER_ID);
        verify(mockSdxStatusService).setStatusForDatalakeAndNotify(
                eq(DatalakeStatusEnum.STOPPED), eq("Datalake is detached."), eq(TEST_CLUSTER_ID)
        );
    }
}
