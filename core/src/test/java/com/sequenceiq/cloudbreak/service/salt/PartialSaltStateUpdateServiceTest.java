package com.sequenceiq.cloudbreak.service.salt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.host.PartialStateUpdater;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@ExtendWith(MockitoExtension.class)
public class PartialSaltStateUpdateServiceTest {
    private static final Long STACK_ID = 1L;

    private static final List<String> SALT_BASE_FOLDERS = List.of("salt-common", "salt");

    private static final byte[] COMPRESSED_VALUE = new byte[0];

    @Mock
    private CompressUtil compressUtil;

    @Mock
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    @Mock
    private OrchestratorMetadata metadata;

    @Mock
    private PartialStateUpdater stateUpdateService;

    @InjectMocks
    private PartialSaltStateUpdateService underTest;

    @BeforeEach
    public void setUp() {
        underTest = new PartialSaltStateUpdateService();
        MockitoAnnotations.openMocks(this);

        when(orchestratorMetadataProvider.getSaltStateDefinitionBaseFolders()).thenReturn(SALT_BASE_FOLDERS);
    }

    @Test
    public void testPartialUpdateSaltStates() throws Exception {
        when(orchestratorMetadataProvider.getStoredStates(STACK_ID)).thenReturn(COMPRESSED_VALUE);
        when(compressUtil.generateCompressedOutputFromFolders(anyList(), anyList())).thenReturn(COMPRESSED_VALUE);
        when(compressUtil.compareCompressedContent(any(), any(), anyList())).thenReturn(false);
        when(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).thenReturn(metadata);
        doNothing().when(stateUpdateService).updatePartialSaltDefinition(any(), anyList(), anyList(), isNull());

        underTest.performSaltUpdate(STACK_ID, Collections.emptyList());

        verify(stateUpdateService, times(1)).updatePartialSaltDefinition(any(), anyList(), anyList(), isNull());
        verify(orchestratorMetadataProvider, times(1)).storeNewState(anyLong(), any());
    }

    @Test
    public void testPartialUpdateSaltStatesWithNotChangedState() throws Exception {
        when(orchestratorMetadataProvider.getStoredStates(STACK_ID)).thenReturn(COMPRESSED_VALUE);
        when(compressUtil.generateCompressedOutputFromFolders(anyList(), anyList())).thenReturn(COMPRESSED_VALUE);
        when(compressUtil.compareCompressedContent(any(), any(), anyList())).thenReturn(true);

        underTest.performSaltUpdate(STACK_ID, Collections.emptyList());

        verify(compressUtil, times(1)).compareCompressedContent(any(), any(), anyList());
        verify(stateUpdateService, times(0)).updatePartialSaltDefinition(any(), anyList(), anyList(), any());
        verify(orchestratorMetadataProvider, times(0)).storeNewState(anyLong(), any());
    }

    @Test
    public void testPartialUpdateSaltStatesWithoutExistingSaltState() throws Exception {
        when(orchestratorMetadataProvider.getStoredStates(STACK_ID)).thenReturn(null);
        when(compressUtil.generateCompressedOutputFromFolders(anyList(), anyList())).thenReturn(null);
        when(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).thenReturn(metadata);
        doNothing().when(stateUpdateService).updatePartialSaltDefinition(any(), anyList(), anyList(), any());

        underTest.performSaltUpdate(STACK_ID, Collections.emptyList());

        verify(stateUpdateService, times(1)).updatePartialSaltDefinition(isNull(), anyList(), anyList(), any());
        verify(orchestratorMetadataProvider, times(0)).storeNewState(anyLong(), any());
    }
}
