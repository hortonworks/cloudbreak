package com.sequenceiq.externalizedcompute.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.CustomCheckUtil;
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

    @Mock
    private CustomCheckUtil customCheckUtil;

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @InjectMocks
    private ExternalizedComputeController underTest;

    @Mock
    private ExternalizedComputeCluster externalizedComputeCluster;

    @BeforeEach
    public void setUp() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(customCheckUtil).run(any());
        when(externalizedComputeCluster.getEnvironmentCrn()).thenReturn("envCrn");
        when(externalizedComputeClusterService.getExternalizedComputeCluster(anyString(), anyString())).thenReturn(externalizedComputeCluster);
    }

    @Test
    public void testClusterDescribe() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.describe(CLUSTER_NAME, ENVIRONMENT_CRN);
        });

        verify(commonPermissionCheckingUtils, timeout(1)).checkPermissionForUserOnResource(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT, USER_CRN, "envCrn");
    }

    @Test
    public void testClusterDelete() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.delete(CLUSTER_NAME, ENVIRONMENT_CRN);
        });

        verify(commonPermissionCheckingUtils, timeout(1)).checkPermissionForUserOnResource(AuthorizationResourceAction.DELETE_ENVIRONMENT, USER_CRN, "envCrn");
    }
}