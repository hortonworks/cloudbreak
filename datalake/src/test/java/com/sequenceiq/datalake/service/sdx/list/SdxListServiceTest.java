package com.sequenceiq.datalake.service.sdx.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.SdxServiceTestBase;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

class SdxListServiceTest extends SdxServiceTestBase {

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private SdxService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("Test listSdxByEnvCrn when no cluster found which has DELETED status but has no deleted timestamp then the result list shouldn't be " +
            "touched or filtered")
    void testListSdxByEnvCrnWhenNoClusterFoundWichHasDeletedStatusButHasNoDeletedTimestampThenTheResultListShouldBeTheSameAsItComesFromTheRepository() {
        SdxCluster cluster = createClusterWithIdAndDeletedTimeStamp(1L);

        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNull(any(), any())).thenReturn(List.of(cluster));
        when(sdxStatusService.findAllSdxClusterIdWhichHasDeletedState()).thenReturn(Collections.emptySet());

        List<SdxCluster> result = underTest.listSdxByEnvCrn(getTestUserCrn(), getTestEnvironmentCrn());

        assertNotNull(result);
        assertEquals(1L, result.size());
        assertEquals(cluster, result.get(0));

        verify(sdxClusterRepository, times(1)).findByAccountIdAndEnvCrnAndDeletedIsNull(any(), any());
        verify(sdxStatusService, times(1)).findAllSdxClusterIdWhichHasDeletedState();
    }

    @Test
    @DisplayName("Test listSdxByEnvCrn when one cluster found which has DELETED status and has no deleted timestamp then it's going to be filtered out from " +
            "the result list.")
    void testListSdxByEnvCrnWhenOneClusterFoundWichHasDeletedStatusAndHasNoDeletedTimestampThenItShouldBeFilteredOutFromTheResultList() {
        SdxCluster actuallyRunningCluster = createClusterWithIdAndDeletedTimeStamp(1L);
        SdxCluster corruptedCluster = createClusterWithIdAndDeletedTimeStamp(2L);

        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNull(any(), any())).thenReturn(List.of(actuallyRunningCluster, corruptedCluster));
        when(sdxStatusService.findAllSdxClusterIdWhichHasDeletedState()).thenReturn(Set.of(corruptedCluster.getId()));

        List<SdxCluster> result = underTest.listSdxByEnvCrn(getTestUserCrn(), getTestEnvironmentCrn());

        assertNotNull(result);
        assertEquals(1L, result.size());
        assertEquals(actuallyRunningCluster, result.get(0));

        verify(sdxClusterRepository, times(1)).findByAccountIdAndEnvCrnAndDeletedIsNull(any(), any());
        verify(sdxStatusService, times(1)).findAllSdxClusterIdWhichHasDeletedState();
    }

    @Test
    @DisplayName("Test listSdx when the provided environment name is not null and no cluster found which has DELETED status but has no deleted " +
            "timestamp then the result list shouldn't be touched or filtered")
    void testListSdxWithNotEmptyEnvNameAndNoCorruptedCluster() {
        SdxCluster cluster = createClusterWithIdAndDeletedTimeStamp(1L);

        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(any(), eq(getTestEnvironmentName()))).thenReturn(List.of(cluster));
        when(sdxStatusService.findAllSdxClusterIdWhichHasDeletedState()).thenReturn(Collections.emptySet());

        List<SdxCluster> result = underTest.listSdx(getTestUserCrn(), getTestEnvironmentName());

        assertNotNull(result);
        assertEquals(1L, result.size());
        assertEquals(cluster, result.get(0));

        verify(sdxClusterRepository, times(1)).findByAccountIdAndEnvNameAndDeletedIsNull(any(), any());
        verify(sdxClusterRepository, times(1)).findByAccountIdAndEnvNameAndDeletedIsNull(any(), eq(getTestEnvironmentName()));
        verify(sdxClusterRepository, times(0)).findByAccountIdAndDeletedIsNull(any());
        verify(sdxStatusService, times(1)).findAllSdxClusterIdWhichHasDeletedState();
    }

    @Test
    @DisplayName("Test listSdx when the provided environment name is not null and one cluster found which has DELETED status but has no deleted " +
            "timestamp then it's going to be filtered out from the result list.")
    void testListSdxWithNotEmptyEnvNameAndOneCorruptedCluster() {
        SdxCluster actuallyRunningCluster = createClusterWithIdAndDeletedTimeStamp(1L);
        SdxCluster corruptedCluster = createClusterWithIdAndDeletedTimeStamp(2L);

        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(any(), eq(getTestEnvironmentName())))
                .thenReturn(List.of(actuallyRunningCluster, corruptedCluster));
        when(sdxStatusService.findAllSdxClusterIdWhichHasDeletedState()).thenReturn(Set.of(corruptedCluster.getId()));

        List<SdxCluster> result = underTest.listSdx(getTestUserCrn(), getTestEnvironmentName());

        assertNotNull(result);
        assertEquals(1L, result.size());
        assertEquals(actuallyRunningCluster, result.get(0));

        verify(sdxClusterRepository, times(1)).findByAccountIdAndEnvNameAndDeletedIsNull(any(), any());
        verify(sdxClusterRepository, times(1)).findByAccountIdAndEnvNameAndDeletedIsNull(any(), eq(getTestEnvironmentName()));
        verify(sdxClusterRepository, times(0)).findByAccountIdAndDeletedIsNull(any());
        verify(sdxStatusService, times(1)).findAllSdxClusterIdWhichHasDeletedState();
    }

    @Test
    @DisplayName("Test listSdx when the provided environment name is null and no cluster found which has DELETED status but has no deleted " +
            "timestamp then the result list shouldn't be touched or filtered")
    void testListSdxWithEmptyEnvNameAndNoCorruptedCluster() {
        SdxCluster cluster = createClusterWithIdAndDeletedTimeStamp(1L);

        when(sdxClusterRepository.findByAccountIdAndDeletedIsNull(any())).thenReturn(List.of(cluster));
        when(sdxStatusService.findAllSdxClusterIdWhichHasDeletedState()).thenReturn(Collections.emptySet());

        List<SdxCluster> result = underTest.listSdx(getTestUserCrn(), null);

        assertNotNull(result);
        assertEquals(1L, result.size());
        assertEquals(cluster, result.get(0));

        verify(sdxClusterRepository, times(0)).findByAccountIdAndEnvNameAndDeletedIsNull(any(), any());
        verify(sdxClusterRepository, times(1)).findByAccountIdAndDeletedIsNull(any());
        verify(sdxStatusService, times(1)).findAllSdxClusterIdWhichHasDeletedState();
    }

    @Test
    @DisplayName("Test listSdx when the provided environment name is null and one cluster found which has DELETED status but has no deleted " +
            "timestamp then it's going to be filtered out from the result list.")
    void testListSdxWithEmptyEnvNameAndOneCorruptedCluster() {
        SdxCluster actuallyRunningCluster = createClusterWithIdAndDeletedTimeStamp(1L);
        SdxCluster corruptedCluster = createClusterWithIdAndDeletedTimeStamp(2L);

        when(sdxClusterRepository.findByAccountIdAndDeletedIsNull(any())).thenReturn(List.of(actuallyRunningCluster, corruptedCluster));
        when(sdxStatusService.findAllSdxClusterIdWhichHasDeletedState()).thenReturn(Set.of(corruptedCluster.getId()));

        List<SdxCluster> result = underTest.listSdx(getTestUserCrn(), null);

        assertNotNull(result);
        assertEquals(1L, result.size());
        assertEquals(actuallyRunningCluster, result.get(0));

        verify(sdxClusterRepository, times(0)).findByAccountIdAndEnvNameAndDeletedIsNull(any(), any());
        verify(sdxClusterRepository, times(1)).findByAccountIdAndDeletedIsNull(any());
        verify(sdxStatusService, times(1)).findAllSdxClusterIdWhichHasDeletedState();
    }

    private SdxCluster createClusterWithIdAndDeletedTimeStamp(Long id) {
        SdxCluster cluster = new SdxCluster();
        cluster.setId(id);
        cluster.setDeleted(null);
        return cluster;
    }

}