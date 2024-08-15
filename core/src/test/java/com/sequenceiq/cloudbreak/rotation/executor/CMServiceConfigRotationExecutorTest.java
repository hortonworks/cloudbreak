package com.sequenceiq.cloudbreak.rotation.executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.HashBasedTable;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class CMServiceConfigRotationExecutorTest {

    @Mock
    private SecretService secretService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDtoService stackService;

    @InjectMocks
    private CMServiceConfigRotationExecutor underTest;

    @Test
    public void testRotation() throws Exception {
        ClusterModificationService clusterModificationService = setup();

        underTest.rotate(new CMServiceConfigRotationContext("resource", HashBasedTable.create()));

        verify(clusterModificationService).updateConfig(any(), any());
    }

    @Test
    public void testRollback() throws Exception {
        ClusterModificationService clusterModificationService = setup();

        underTest.rollback(new CMServiceConfigRotationContext("resource", HashBasedTable.create()));

        verify(clusterModificationService).updateConfig(any(), any());
    }

    private ClusterModificationService setup() throws Exception {
        when(stackService.getByCrn(any())).thenReturn(new StackDto());
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        doNothing().when(clusterModificationService).updateConfig(any(), any());
        return clusterModificationService;
    }
}
