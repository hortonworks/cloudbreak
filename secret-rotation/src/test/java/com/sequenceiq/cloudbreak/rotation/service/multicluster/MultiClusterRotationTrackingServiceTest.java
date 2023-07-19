package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import static com.sequenceiq.cloudbreak.rotation.common.TestMultiSecretType.MULTI_TEST;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.CHILD;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.PARENT_FINAL;
import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.PARENT_INITIAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.repository.MultiClusterRotationResourceRepository;

@ExtendWith(MockitoExtension.class)
public class MultiClusterRotationTrackingServiceTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    @Mock
    private MultiClusterRotationResourceRepository repository;

    @InjectMocks
    private MultiClusterRotationTrackingService underTest;

    @Test
    void testSwitchParentIfNotInitial() {
        underTest.switchParentToFinalPhase(new MultiClusterRotationResource(null, MULTI_TEST, CHILD));

        verify(repository, times(0)).save(any());
    }

    @Test
    void testSwitchParent() {
        underTest.switchParentToFinalPhase(new MultiClusterRotationResource(null, MULTI_TEST, PARENT_INITIAL));

        ArgumentCaptor<MultiClusterRotationResource> resourceCaptor = ArgumentCaptor.forClass(MultiClusterRotationResource.class);
        verify(repository).save(resourceCaptor.capture());
        assertEquals(PARENT_FINAL, resourceCaptor.getValue().getType());
    }

    @Test
    void testMarkResources() {
        underTest.markResources(new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), MULTI_TEST));

        verify(repository, times(2)).save(any());
    }
}
