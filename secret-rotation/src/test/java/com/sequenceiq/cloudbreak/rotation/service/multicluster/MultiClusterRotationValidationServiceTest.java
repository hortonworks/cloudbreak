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
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;

@ExtendWith(MockitoExtension.class)
public class MultiClusterRotationValidationServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:envCrn1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

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
    void testPrepareParent() {
        when(trackingService.getMultiRotationEntriesForResource(any(), any())).thenReturn(Set.of());

        underTest.validateMultiRotationRequest(ENV_CRN, TEST_2);

        verify(trackingService).getMultiRotationEntriesForResource(any(), any());
        verifyNoInteractions(childContextProvider);
    }

    @Test
    void testPrepareParentIfChildRotationStillNeeded() throws IllegalAccessException {
        when(trackingService.getMultiRotationEntriesForResource(any(), any())).thenReturn(
                Set.of(new MultiClusterRotationResource(ENV_CRN, MultiSecretType.DEMO_MULTI_SECRET, MultiClusterRotationResourceType.INITIATED_PARENT)));
        InterServiceMultiClusterRotationService interServiceMultiClusterRotationService =
                mock(InterServiceMultiClusterRotationService.class);
        FieldUtils.writeField(underTest, "interServiceMultiClusterRotationTrackingService",
                Optional.of(interServiceMultiClusterRotationService), true);
        when(interServiceMultiClusterRotationService.checkOngoingChildrenMultiSecretRotations(any(), any())).thenReturn(Boolean.TRUE);

        assertThrows(BadRequestException.class, () -> underTest.validateMultiRotationRequest(ENV_CRN, TEST_2));

        verify(trackingService).getMultiRotationEntriesForResource(any(), any());
        verifyNoInteractions(childContextProvider);
    }

    @Test
    void testPrepareChildIfThereIsNoOngoingRotationForType() {
        when(trackingService.getMultiRotationEntriesForResource(any(), any())).thenReturn(Set.of());

        assertThrows(BadRequestException.class, () -> underTest.validateMultiRotationRequest(DATAHUB_CRN, TEST_2));

        verify(trackingService).getMultiRotationEntriesForResource(any(), any());
        verifyNoInteractions(parentContextProvider);
    }

    @Test
    void testPrepareChild() {
        when(trackingService.getMultiRotationEntriesForResource(any(), any())).thenReturn(
                Set.of(new MultiClusterRotationResource(DATAHUB_CRN, MultiSecretType.DEMO_MULTI_SECRET, MultiClusterRotationResourceType.PENDING_CHILD)));

        underTest.validateMultiRotationRequest(DATAHUB_CRN, TEST_2);

        verify(trackingService).getMultiRotationEntriesForResource(any(), any());
        verifyNoInteractions(parentContextProvider);
    }

    @Test
    void testPrepareChildIfRotationNotNeeded() {
        when(trackingService.getMultiRotationEntriesForResource(any(), any())).thenReturn(Set.of());

        assertThrows(BadRequestException.class, () -> underTest.validateMultiRotationRequest(DATAHUB_CRN, TEST_2));

        verify(trackingService).getMultiRotationEntriesForResource(any(), any());
        verifyNoInteractions(parentContextProvider);
    }
}
