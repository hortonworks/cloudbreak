package com.sequenceiq.cloudbreak.cluster.service.clustercomponent;

import static com.sequenceiq.cloudbreak.cluster.common.TestUtil.getEmptyJson;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
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
    private EntityManager entityManager;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private ClusterComponentConfigProvider underTest;

    @Test
    void testCleanUpDetachedEntriesWhenThereAreOrphanedEntriesThenDeletionShouldHappen() {
        Long clusterId = 1L;
        underTest.cleanUpDetachedEntries(clusterId);

        verify(mockClusterComponentHistoryRepository, times(1)).deleteByClusterId(clusterId);
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

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public ClusterComponentConfigProvider getUnderTest() {
        return underTest;
    }

}
