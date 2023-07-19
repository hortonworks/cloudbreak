package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_4;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.TestMultiSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;

@ExtendWith(MockitoExtension.class)
public class MultiClusterRotationValidationServiceTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    private static final String DATALAKE_CRN_2 = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster2";

    private static final String DATAHUB_CRN_2 = "crn:cdp:datahub:us-west-1:tenant:cluster:resource2";

    @Mock
    private RotationContextProvider parentContextProvider;

    @Mock
    private RotationContextProvider childContextProvider;

    @Mock
    private MultiClusterRotationService trackingService;

    @InjectMocks
    private MultiClusterRotationValidationService underTest;

    @BeforeEach
    void setup() throws IllegalAccessException {
        parentContextProvider = mock(RotationContextProvider.class);
        childContextProvider = mock(RotationContextProvider.class);
        FieldUtils.writeField(underTest, "rotationContextProviderMap",
                Map.of(TEST_2, parentContextProvider, TEST_4, childContextProvider), true);
    }

    @Test
    void testPrepareParentIfThereIsOtherMultiRotationOngoing() {
        when(trackingService.getMultiRotationEntriesForResource(any())).thenReturn(
                Set.of(new MultiClusterRotationResource(null, TestMultiSecretType.MULTI_TEST_2, null)));

        assertThrows(BadRequestException.class, () -> underTest.validateMultiRotationRequest(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getMultiRotationEntriesForResource(any());
        verifyNoInteractions(childContextProvider);
    }

    @Test
    void testPrepareParent() {
        when(trackingService.getMultiRotationEntriesForResource(any())).thenReturn(Set.of());

        underTest.validateMultiRotationRequest(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST);

        verify(trackingService).getMultiRotationEntriesForResource(any());
        verifyNoInteractions(childContextProvider);
    }

    @Test
    void testPrepareParentIfChildRotationStillNeeded() throws IllegalAccessException {
        when(trackingService.getMultiRotationEntriesForResource(any())).thenReturn(
                Set.of(new MultiClusterRotationResource(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST, MultiClusterRotationResourceType.INITIATED_PARENT)));
        InterServiceMultiClusterRotationService interServiceMultiClusterRotationService =
                mock(InterServiceMultiClusterRotationService.class);
        FieldUtils.writeField(underTest, "interServiceMultiClusterRotationTrackingService",
                Optional.of(interServiceMultiClusterRotationService), true);
        when(interServiceMultiClusterRotationService.checkOngoingChildrenMultiSecretRotations(any(), any())).thenReturn(Boolean.TRUE);

        assertThrows(BadRequestException.class, () -> underTest.validateMultiRotationRequest(DATALAKE_CRN, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getMultiRotationEntriesForResource(any());
        verifyNoInteractions(childContextProvider);
    }

    @Test
    void testPrepareChildIfThereIsNoOngoingRotationForType() {
        when(trackingService.getMultiRotationEntriesForResource(any())).thenReturn(Set.of());

        assertThrows(BadRequestException.class, () -> underTest.validateMultiRotationRequest(DATAHUB_CRN, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getMultiRotationEntriesForResource(any());
        verifyNoInteractions(parentContextProvider);
    }

    @Test
    void testPrepareChild() {
        when(trackingService.getMultiRotationEntriesForResource(any())).thenReturn(
                Set.of(new MultiClusterRotationResource(DATAHUB_CRN, TestMultiSecretType.MULTI_TEST, MultiClusterRotationResourceType.PENDING_CHILD)));

        underTest.validateMultiRotationRequest(DATAHUB_CRN, TestMultiSecretType.MULTI_TEST);

        verify(trackingService).getMultiRotationEntriesForResource(any());
        verifyNoInteractions(parentContextProvider);
    }

    @Test
    void testPrepareChildIfRotationNotNeeded() {
        when(trackingService.getMultiRotationEntriesForResource(any())).thenReturn(Set.of());

        assertThrows(BadRequestException.class, () -> underTest.validateMultiRotationRequest(DATAHUB_CRN, TestMultiSecretType.MULTI_TEST));

        verify(trackingService).getMultiRotationEntriesForResource(any());
        verifyNoInteractions(parentContextProvider);
    }
}
