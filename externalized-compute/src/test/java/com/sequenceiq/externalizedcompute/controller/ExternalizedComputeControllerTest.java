package com.sequenceiq.externalizedcompute.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterFlowManager;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterConverterService;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeControllerTest {

    private static final String CLUSTER_NAME = "cluster1";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Mock
    private ExternalizedComputeClusterFlowManager externalizedComputeClusterFlowManager;

    @Mock
    private ExternalizedComputeClusterConverterService externalizedComputeClusterConverterService;

    @InjectMocks
    private ExternalizedComputeController underTest;

    @Mock
    private ExternalizedComputeCluster externalizedComputeCluster;

    @Test
    public void testClusterDescribe() {
        when(externalizedComputeClusterService.getExternalizedComputeCluster(anyString(), anyString())).thenReturn(externalizedComputeCluster);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.describe(ENVIRONMENT_CRN, CLUSTER_NAME);
        });

        verify(externalizedComputeClusterService, times(1)).getExternalizedComputeCluster(eq(ENVIRONMENT_CRN), eq(CLUSTER_NAME));
        verify(externalizedComputeClusterConverterService, times(1)).convertToResponse(any());
    }

    @Test
    public void testClusterDelete() {
        when(externalizedComputeClusterService.getExternalizedComputeCluster(anyString(), anyString())).thenReturn(externalizedComputeCluster);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.delete(ENVIRONMENT_CRN, CLUSTER_NAME, false);
        });

        verify(externalizedComputeClusterService, times(1)).getExternalizedComputeCluster(eq(ENVIRONMENT_CRN), eq(CLUSTER_NAME));
        verify(externalizedComputeClusterFlowManager, times(1)).triggerExternalizedComputeClusterDeletion(any(), eq(false));
    }
}