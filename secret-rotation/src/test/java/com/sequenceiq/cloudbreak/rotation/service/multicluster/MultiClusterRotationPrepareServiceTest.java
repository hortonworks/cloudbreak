package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_4;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.common.MultiClusterRotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.TestMultiSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;

@ExtendWith(MockitoExtension.class)
public class MultiClusterRotationPrepareServiceTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    private static final String DATALAKE_CRN_2 = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster2";

    private static final String DATAHUB_CRN_2 = "crn:cdp:datahub:us-west-1:tenant:cluster:resource2";

    @Mock
    private MultiClusterRotationContextProvider parentContextProvider;

    @Mock
    private MultiClusterRotationContextProvider childContextProvider;

    @Mock
    private MultiClusterRotationTrackingService trackingService;

    @InjectMocks
    private MultiClusterRotationPrepareService underTest;

    @BeforeEach
    void setup() throws IllegalAccessException {
        parentContextProvider = mock(MultiClusterRotationContextProvider.class);
        childContextProvider = mock(MultiClusterRotationContextProvider.class);
        FieldUtils.writeField(underTest, "rotationContextProviderMap",
                Map.of(TEST_2, parentContextProvider, TEST_4, childContextProvider), true);
    }

    @Test
    void testPrepareParentIfThereIsOtherMultiRotationOngoing() {
        when(parentContextProvider.getMultiClusterRotationMetadata(any())).thenReturn(
                new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST));
        when(trackingService.getAll(any(), any())).thenReturn(
                Set.of(new MultiClusterRotationResource(null, TestMultiSecretType.MULTI_TEST_2, null)));

        assertThrows(BadRequestException.class, () -> underTest.prepare(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getAll(any(), any());
        verify(parentContextProvider).getMultiClusterRotationMetadata(eq(DATALAKE_CRN));
        verifyNoInteractions(childContextProvider);
    }

    @Test
    void testPrepareParent() {
        when(parentContextProvider.getMultiClusterRotationMetadata(any())).thenReturn(
                new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST));
        when(trackingService.getAll(any(), any())).thenReturn(Set.of());
        doNothing().when(trackingService).markResources(any());

        underTest.prepare(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST);

        verify(trackingService).getAll(any(), any());
        verify(parentContextProvider).getMultiClusterRotationMetadata(eq(DATALAKE_CRN));
        verify(trackingService).markResources(any());
        verifyNoInteractions(childContextProvider);
    }

    @Test
    void testPrepareParentIfChildRotationStillNeeded() {
        when(parentContextProvider.getMultiClusterRotationMetadata(any())).thenReturn(
                new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST));
        when(trackingService.getAll(any(), any())).thenReturn(
                Set.of(new MultiClusterRotationResource(DATAHUB_CRN, TestMultiSecretType.MULTI_TEST, MultiClusterRotationResourceType.CHILD)));

        assertThrows(BadRequestException.class, () -> underTest.prepare(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getAll(any(), any());
        verify(parentContextProvider).getMultiClusterRotationMetadata(eq(DATALAKE_CRN));
        verifyNoInteractions(childContextProvider);
    }

    @Test
    void testPrepareParentIfDbCorrupted() {
        when(parentContextProvider.getMultiClusterRotationMetadata(any())).thenReturn(
                new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST));
        when(trackingService.getAll(any(), any())).thenReturn(
                Set.of(new MultiClusterRotationResource(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST, MultiClusterRotationResourceType.PARENT_INITIAL)));

        assertThrows(CloudbreakServiceException.class, () -> underTest.prepare(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getAll(any(), any());
        verify(parentContextProvider).getMultiClusterRotationMetadata(eq(DATALAKE_CRN));
        verifyNoInteractions(childContextProvider);
    }

    @Test
    void testPrepareChildIfParentInitialOngoing() {
        when(childContextProvider.getMultiClusterRotationMetadata(any())).thenReturn(
                new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST));
        when(trackingService.getAll(any(), any())).thenReturn(
                Set.of(new MultiClusterRotationResource(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST, MultiClusterRotationResourceType.PARENT_INITIAL)));

        assertThrows(BadRequestException.class, () -> underTest.prepare(DATAHUB_CRN, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getAll(any(), any());
        verify(childContextProvider).getMultiClusterRotationMetadata(eq(DATAHUB_CRN));
        verifyNoInteractions(parentContextProvider);
    }

    @Test
    void testPrepareChildIfThereIsNoOngoingRotationForType() {
        when(childContextProvider.getMultiClusterRotationMetadata(any())).thenReturn(
                new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST));
        when(trackingService.getAll(any(), any())).thenReturn(Set.of());

        assertThrows(BadRequestException.class, () -> underTest.prepare(DATAHUB_CRN, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getAll(any(), any());
        verify(childContextProvider).getMultiClusterRotationMetadata(eq(DATAHUB_CRN));
        verifyNoInteractions(parentContextProvider);
    }

    @Test
    void testPrepareChild() {
        when(childContextProvider.getMultiClusterRotationMetadata(any())).thenReturn(
                new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST));
        when(trackingService.getAll(any(), any())).thenReturn(
                Set.of(new MultiClusterRotationResource(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST, MultiClusterRotationResourceType.PARENT_FINAL),
                        new MultiClusterRotationResource(DATAHUB_CRN, TestMultiSecretType.MULTI_TEST, MultiClusterRotationResourceType.CHILD)));

        underTest.prepare(DATAHUB_CRN, TestMultiSecretType.MULTI_TEST);

        verify(trackingService).getAll(any(), any());
        verify(childContextProvider).getMultiClusterRotationMetadata(eq(DATAHUB_CRN));
        verifyNoInteractions(parentContextProvider);
    }

    @Test
    void testPrepareChildIfRotationNotNeeded() {
        when(childContextProvider.getMultiClusterRotationMetadata(any())).thenReturn(
                new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST));
        when(trackingService.getAll(any(), any())).thenReturn(
                Set.of(new MultiClusterRotationResource(null, TestMultiSecretType.MULTI_TEST, MultiClusterRotationResourceType.PARENT_FINAL)));

        assertThrows(BadRequestException.class, () -> underTest.prepare(DATAHUB_CRN, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getAll(any(), any());
        verify(childContextProvider).getMultiClusterRotationMetadata(eq(DATAHUB_CRN));
        verifyNoInteractions(parentContextProvider);
    }

    @Test
    void testPrepareChildIfUnknownResource() {
        when(childContextProvider.getMultiClusterRotationMetadata(any())).thenReturn(
                new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST));
        when(trackingService.getAll(any(), any())).thenReturn(Set.of());

        assertThrows(CloudbreakServiceException.class, () -> underTest.prepare(DATAHUB_CRN_2, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getAll(any(), any());
        verify(childContextProvider).getMultiClusterRotationMetadata(eq(DATAHUB_CRN_2));
        verifyNoInteractions(parentContextProvider);
    }

    @Test
    void testPreparePArentIfUnknownResource() {
        when(parentContextProvider.getMultiClusterRotationMetadata(any())).thenReturn(
                new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST));
        when(trackingService.getAll(any(), any())).thenReturn(Set.of());

        assertThrows(CloudbreakServiceException.class, () -> underTest.prepare(DATALAKE_CRN_2, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getAll(any(), any());
        verify(parentContextProvider).getMultiClusterRotationMetadata(eq(DATALAKE_CRN_2));
        verifyNoInteractions(childContextProvider);
    }
}
