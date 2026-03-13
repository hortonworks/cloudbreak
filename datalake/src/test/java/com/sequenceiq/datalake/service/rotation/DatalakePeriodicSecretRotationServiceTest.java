package com.sequenceiq.datalake.service.rotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.SecretTypeListService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxStatusRepository;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.sdx.api.model.SdxSecretTypeResponse;

@ExtendWith(MockitoExtension.class)
class DatalakePeriodicSecretRotationServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:acc-123:cluster:dl";

    @Mock
    private SdxRotationService sdxRotationService;

    @Mock
    private List<SecretType> enabledSecretTypes;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxStatusRepository sdxStatusRepository;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private SecretTypeListService secretTypeListService;

    @Spy
    @InjectMocks
    private DatalakePeriodicSecretRotationService underTest;

    private SdxCluster cluster;

    private SdxStatusEntity sdxStatusEntity;

    @BeforeEach
    void setup() {
        cluster = new SdxCluster();
        cluster.setId(10L);
        cluster.setCrn(RESOURCE_CRN);
        cluster.setResourceCrn(RESOURCE_CRN);
        sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.RUNNING);
    }

    @Test
    void listJobResourcesDelegatesToRepository() {
        JobResource jr = mock(JobResource.class);
        when(sdxClusterRepository.findAllAliveView()).thenReturn(List.of(jr));
        List<JobResource> out = underTest.listJobResources();
        assertThat(out).hasSize(1).containsExactly(jr);
        verify(sdxClusterRepository).findAllAliveView();
    }

    @Test
    void isSchedulableTrueWhenRunningAndNoFlow() {
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(cluster));
        when(sdxStatusRepository.findFirstByDatalakeIdIsOrderByIdDesc(cluster.getId()))
                .thenReturn(sdxStatusEntity);
        when(flowLogService.isOtherFlowRunning(10L)).thenReturn(false);
        assertThat(underTest.isSchedulable(RESOURCE_CRN)).isTrue();
    }

    @Test
    void isSchedulableFalseWhenFlowRunning() {
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(cluster));
        when(sdxStatusRepository.findFirstByDatalakeIdIsOrderByIdDesc(cluster.getId()))
                .thenReturn(sdxStatusEntity);
        when(flowLogService.isOtherFlowRunning(10L)).thenReturn(true);
        assertThat(underTest.isSchedulable(RESOURCE_CRN)).isFalse();
    }

    @Test
    void isSchedulableFalseWhenNotRunning() {
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(cluster));
        sdxStatusEntity.setStatus(DatalakeStatusEnum.PROVISIONING_FAILED);
        when(sdxStatusRepository.findFirstByDatalakeIdIsOrderByIdDesc(cluster.getId()))
                .thenReturn(sdxStatusEntity);
        when(flowLogService.isOtherFlowRunning(10L)).thenReturn(false);
        assertThat(underTest.isSchedulable(RESOURCE_CRN)).isFalse();
    }

    @Test
    void isSchedulableThrowsWhenStatusNotFound() {
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(cluster));
        when(sdxStatusRepository.findFirstByDatalakeIdIsOrderByIdDesc(cluster.getId()))
                .thenReturn(null);
        assertThatThrownBy(() -> underTest.isSchedulable(RESOURCE_CRN))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void isSchedulableThrowsWhenClusterNotFound() {
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.isSchedulable(RESOURCE_CRN))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Datalake stack with resource crn")
                .hasMessageContaining("not found or deleted");
    }

    @Test
    void isSchedulableThrowsWhenClusterIsNull() {
        doReturn(null).when(underTest).getResourceByCrn(RESOURCE_CRN);

        assertThatThrownBy(() -> underTest.isSchedulable(RESOURCE_CRN))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void listRotatableSecretNamesMapsNames() {
        SdxSecretTypeResponse a = new SdxSecretTypeResponse("SALT_PASSWORD", "n", "d", 0L);
        SdxSecretTypeResponse b = new SdxSecretTypeResponse("COMPUTE_MONITORING_CREDENTIALS", "n2", "d2", 0L);
        when(secretTypeListService.listRotatableSecretType(eq(RESOURCE_CRN), any())).thenReturn(List.of(a, b));
        List<String> names = underTest.listRotatableSecretNames(RESOURCE_CRN);
        assertThat(names).containsExactlyInAnyOrder(
                a.getSecretType(), b.getSecretType());
    }

    @Test
    void enabledSecretTypesReturnsInjected() {
        assertThat(underTest.enabledSecretTypes()).isSameAs(enabledSecretTypes);
    }

    @Test
    void triggerRotationDelegatesToSdxRotationService() {
        List<String> due = List.of("SALT_PASSWORD", "COMPUTE_MONITORING_CREDENTIALS");
        underTest.triggerRotation(RESOURCE_CRN, due);
        verify(sdxRotationService).triggerSecretRotation(eq(RESOURCE_CRN), eq(due), eq(null), eq(null));
    }

    @Test
    void getResourceCreationDateDelegatesToRepository() {
        // Service truncates to milliseconds (ofEpochMilli), so use a millisecond-precision instant
        long createdMs = System.currentTimeMillis() - Duration.ofDays(12).toMillis();
        Instant created = Instant.ofEpochMilli(createdMs);
        when(sdxClusterRepository.getCreatedByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.of(created.toEpochMilli()));
        assertThat(underTest.getResourceCreationDate(RESOURCE_CRN)).isEqualTo(created);
        verify(sdxClusterRepository).getCreatedByResourceCrn(eq(RESOURCE_CRN));
    }

    @Test
    void getResourceCreationDateThrowsWhenMissing() {
        when(sdxClusterRepository.getCreatedByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> underTest.getResourceCreationDate(RESOURCE_CRN))
                .isInstanceOf(NotFoundException.class);
    }
}

