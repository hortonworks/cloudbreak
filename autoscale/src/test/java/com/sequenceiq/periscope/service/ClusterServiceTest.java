package com.sequenceiq.periscope.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.repository.ClusterRepository;

@ExtendWith(MockitoExtension.class)
public class ClusterServiceTest {
    @Mock
    ClusterRepository clusterRepository;

    @InjectMocks
    ClusterService clusterService;

    @Test
    void testUpdateClusterDeleted() {
        clusterService.updateClusterDeleted(1L, ClusterState.DELETED, 1);
        verify(clusterRepository).updateClusterDeleted(1L, ClusterState.DELETED, 1);
    }

}
