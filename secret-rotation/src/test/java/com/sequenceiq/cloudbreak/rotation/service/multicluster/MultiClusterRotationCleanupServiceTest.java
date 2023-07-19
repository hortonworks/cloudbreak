package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import static com.sequenceiq.cloudbreak.rotation.common.TestMultiSecretType.MULTI_TEST;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.CHILD;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.PARENT_FINAL;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.PARENT_INITIAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;

@ExtendWith(MockitoExtension.class)
public class MultiClusterRotationCleanupServiceTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    @Mock
    private MultiClusterRotationTrackingService trackingService;

    @InjectMocks
    private MultiClusterRotationCleanupService underTest;

    @Test
    void testCleanupAfterRollbackIfParentInitialRotation() {
        when(trackingService.getResourceForSecretType(any(), any())).thenReturn(Optional.of(
                new MultiClusterRotationResource(DATALAKE_CRN, MULTI_TEST, PARENT_INITIAL)));
        doNothing().when(trackingService).clearAllForSecretType(any());

        underTest.cleanupAfterRollback(DATALAKE_CRN, getMetadata());

        verify(trackingService).clearAllForSecretType(any());
    }

    @Test
    void testCleanupAfterRollbackIfParentFinalRotation() {
        when(trackingService.getResourceForSecretType(any(), any())).thenReturn(Optional.of(
                new MultiClusterRotationResource(DATALAKE_CRN, MULTI_TEST, PARENT_FINAL)));

        underTest.cleanupAfterRollback(DATALAKE_CRN, getMetadata());

        verify(trackingService, times(0)).clearAllForSecretType(any());
    }

    @Test
    void testCleanupAfterFinalizeIfParentInitialRotation() {
        when(trackingService.getResourceForSecretType(any(), any())).thenReturn(Optional.of(
                new MultiClusterRotationResource(DATALAKE_CRN, MULTI_TEST, PARENT_INITIAL)));
        doNothing().when(trackingService).switchParentToFinalPhase(any());

        underTest.cleanupAfterFinalize(DATALAKE_CRN, getMetadata());

        verify(trackingService).switchParentToFinalPhase(any());
        verify(trackingService, times(0)).clearResourceForSecretType(any(), any());
    }

    @Test
    void testCleanupAfterFinalizeIfChildRotation() {
        when(trackingService.getResourceForSecretType(any(), any())).thenReturn(Optional.of(
                new MultiClusterRotationResource(DATAHUB_CRN, MULTI_TEST, CHILD)));
        doNothing().when(trackingService).clearResourceForSecretType(any(), any());

        underTest.cleanupAfterFinalize(DATAHUB_CRN, getMetadata());

        verify(trackingService, times(0)).switchParentToFinalPhase(any());
        verify(trackingService).clearResourceForSecretType(eq(DATAHUB_CRN), any());
    }

    @Test
    void testCleanupAfterFinalizeIfParentFinalRotation() {
        when(trackingService.getResourceForSecretType(any(), any())).thenReturn(Optional.of(
                new MultiClusterRotationResource(DATALAKE_CRN, MULTI_TEST, PARENT_FINAL)));
        doNothing().when(trackingService).clearResourceForSecretType(any(), any());

        underTest.cleanupAfterFinalize(DATALAKE_CRN, getMetadata());

        verify(trackingService, times(0)).switchParentToFinalPhase(any());
        verify(trackingService).clearResourceForSecretType(eq(DATALAKE_CRN), any());
    }

    private static MultiClusterRotationMetadata getMetadata() {
        return new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), MULTI_TEST);
    }
}
