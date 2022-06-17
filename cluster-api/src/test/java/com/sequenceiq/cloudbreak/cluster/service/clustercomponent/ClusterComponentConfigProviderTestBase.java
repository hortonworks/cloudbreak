package com.sequenceiq.cloudbreak.cluster.service.clustercomponent;

import static com.sequenceiq.cloudbreak.cluster.common.TestUtil.getEmptyJson;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Set;

import org.hibernate.envers.AuditReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.repository.ClusterComponentHistoryRepository;
import com.sequenceiq.cloudbreak.repository.ClusterComponentRepository;
import com.sequenceiq.cloudbreak.repository.ClusterComponentViewRepository;

@ExtendWith(MockitoExtension.class)
public class ClusterComponentConfigProviderTestBase {

    protected static final Json MOCK_JSON = getEmptyJson();

    @Mock
    private ClusterComponentRepository mockComponentRepository;

    @Mock
    private ClusterComponentViewRepository mockComponentViewRepository;

    @Mock
    private ClusterComponentHistoryRepository mockClusterComponentHistoryRepository;

    @Mock
    private AuditReader mockAuditReader;

    @InjectMocks
    private ClusterComponentConfigProvider underTest;

    @Test
    void testCleanUpDetachedEntriesWhenThereAreOrphanedEntriesThenDeletionShouldHappen() {
        Set<Long> ids = Set.of(1L);
        underTest.cleanUpDetachedEntries(ids);

        verify(mockClusterComponentHistoryRepository, times(1)).deleteByClusterIdIsNullOrClusterIdIsIn(ids);
        verifyNoMoreInteractions(mockClusterComponentHistoryRepository);
    }

    public ClusterComponentRepository getMockComponentRepository() {
        return mockComponentRepository;
    }

    public ClusterComponentViewRepository getMockComponentViewRepository() {
        return mockComponentViewRepository;
    }

    public ClusterComponentHistoryRepository getMockClusterComponentHistoryRepository() {
        return mockClusterComponentHistoryRepository;
    }

    public AuditReader getMockAuditReader() {
        return mockAuditReader;
    }

    public ClusterComponentConfigProvider getUnderTest() {
        return underTest;
    }

}
