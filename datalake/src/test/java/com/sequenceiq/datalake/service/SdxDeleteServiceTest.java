package com.sequenceiq.datalake.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
class SdxDeleteServiceTest {

    private static final String ACCOUNT_ID = "accId";

    private static final String CLUSTER_NAME = "test-sdx-cluster";

    private static final Long SDX_ID = 2L;

    private static final String SDX_CRN = "crn";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    @Mock
    private FlowCancelService flowCancelService;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private DistroxService distroxService;

    @InjectMocks
    private SdxDeleteService underTest;

    @Test
    void testDeleteSdxWhenNameIsProvidedAndClusterDoesNotExistShouldThrowNotFoundException() {
        assertThrows(com.sequenceiq.cloudbreak.common.exception.NotFoundException.class,
                () -> underTest.deleteSdx(ACCOUNT_ID, CLUSTER_NAME, false));
        verify(sdxClusterRepository, times(1))
                .findByAccountIdAndClusterNameAndDeletedIsNull(eq(ACCOUNT_ID), eq(CLUSTER_NAME));
    }

    @Test
    void testDeleteSdxWhenNameIsProvidedShouldInitiateSdxDeletionFlow() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxReactorFlowManager.triggerSdxDeletion(any(SdxCluster.class), anyBoolean())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        mockCBCallForDistroXClusters(Sets.newHashSet());
        underTest.deleteSdx(ACCOUNT_ID, "sdx-cluster-name", true);
        verify(sdxReactorFlowManager, times(1)).triggerSdxDeletion(sdxCluster, true);
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
    }

    @Test
    void testDeleteSdxWhenSdxHasExternalDatabaseButCrnIsMissingShouldThrowNotFoundException() {
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.getSdxDatabase().setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.HA);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.deleteSdx(ACCOUNT_ID, "sdx-cluster-name", false));
        assertEquals(String.format("Can not find external database for Data Lake, but it was requested: %s. Please use force delete.",
                sdxCluster.getClusterName()), badRequestException.getMessage());
    }

    @Test
    void testDeleteSdxWhenSdxHasExternalDatabaseButCrnIsMissingShouldNotThrowNotFoundException() {
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.getSdxDatabase().setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.HA);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.PROVISIONING_FAILED);
        when(sdxReactorFlowManager.triggerSdxDeletion(eq(sdxCluster), anyBoolean())).thenReturn(new FlowIdentifier(FlowType.FLOW, "id"));
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);
        underTest.deleteSdx(ACCOUNT_ID, "sdx-cluster-name", false);
        verify(sdxClusterRepository, times(1)).save(sdxCluster);
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
        verify(sdxReactorFlowManager, times(1)).triggerSdxDeletion(sdxCluster, false);
        verify(flowCancelService, times(1)).cancelRunningFlows(SDX_ID);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"AVAILABLE", "BACKUP_IN_PROGRESS", "STOPPED", "BACKUP_FAILED"})
    void testDeleteSdxWhenSdxHasAttachedDataHubsShouldThrowBadRequest(Status dhStatus) {
        SdxCluster sdxCluster = getSdxCluster();
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.RUNNING);
        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setName("existingDistroXCluster");
        stackViewV4Response.setStatus(dhStatus);

        mockCBCallForDistroXClusters(Sets.newHashSet(stackViewV4Response));
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.deleteSdx(ACCOUNT_ID, "sdx-cluster-name", true));

        assertEquals("The following Data Hub(s) cluster(s) must be terminated before deletion of SDX cluster: [existingDistroXCluster].",
                badRequestException.getMessage());
    }

    private void mockCBCallForDistroXClusters(Set<StackViewV4Response> stackViews) {
        when(distroxService.getAttachedDistroXClusters(anyString())).thenReturn(stackViews);
    }

    private SdxCluster getSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setCrn(SDX_CRN);
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setEnvName("envir");
        sdxCluster.setClusterName("sdx-cluster-name");
        sdxCluster.setSdxDatabase(new SdxDatabase());
        return sdxCluster;
    }

}