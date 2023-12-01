package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import static com.sequenceiq.cloudbreak.rotation.MultiSecretType.DEMO_MULTI_SECRET;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;
import com.sequenceiq.cloudbreak.rotation.repository.MultiClusterRotationResourceRepository;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@ExtendWith(MockitoExtension.class)
public class MultiClusterRotationServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:envCrn1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    @Mock
    private MultiClusterRotationResourceRepository repository;

    @InjectMocks
    private MultiClusterRotationService underTest;

    @Test
    void testUpdateResourcesIfParentFirstPhase() throws IllegalAccessException {
        when(repository.findByResourceCrnAndSecretTypeAndType(any(), any(), any())).thenReturn(Optional.empty());
        InterServiceMultiClusterRotationService interServiceMultiClusterRotationService =
                mock(InterServiceMultiClusterRotationService.class);
        FieldUtils.writeField(underTest, "interServiceMultiClusterRotationTrackingService",
                Optional.of(interServiceMultiClusterRotationService), true);
        doNothing().when(interServiceMultiClusterRotationService).markChildren(any(), any());

        underTest.updateMultiRotationEntriesAfterRotate(
                new RotationMetadata(TestSecretType.TEST_2, null, null, ENV_CRN, Optional.of(DEMO_MULTI_SECRET), null));

        verify(repository).save(any());
        verify(interServiceMultiClusterRotationService).markChildren(any(), any());
    }

    @Test
    void testUpdateResourcesIfParentSecondPhase() {
        when(repository.findByResourceCrnAndSecretTypeAndType(any(), any(), any())).thenReturn(Optional.of(
                new MultiClusterRotationResource(ENV_CRN, DEMO_MULTI_SECRET, MultiClusterRotationResourceType.INITIATED_PARENT)));
        underTest.updateMultiRotationEntriesAfterFinalize(
                new RotationMetadata(TestSecretType.TEST_2, null, null, ENV_CRN, Optional.of(DEMO_MULTI_SECRET), null));

        verify(repository).delete(any());
    }

    @Test
    void testUpdateResourcesIfChild() {
        when(repository.findByResourceCrnAndSecretTypeAndType(any(), any(), any())).thenReturn(Optional.of(
                new MultiClusterRotationResource(DATAHUB_CRN, DEMO_MULTI_SECRET, MultiClusterRotationResourceType.PENDING_CHILD)));
        underTest.updateMultiRotationEntriesAfterFinalize(
                new RotationMetadata(TestSecretType.TEST_4, null, null, DATAHUB_CRN, Optional.of(DEMO_MULTI_SECRET), null));

        verify(repository).delete(any());
    }

    @Test
    void testMarkChildren() {
        underTest.markChildrenMultiRotationEntriesLocally(Set.of(DATAHUB_CRN, ENV_CRN), DEMO_MULTI_SECRET.value());

        verify(repository, times(2)).save(any());
    }
}
