package com.sequenceiq.environment.environment.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Responses;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class ClusterAvailabilityValidatorTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:e8d94be4-1b1b-4dc3-b791-5b37a72d2e98";

    private static final String DH_CRN_1 = "crn:cdp:datahub:us-west-1:1234:cluster:dh-1";

    private static final String DH_CRN_2 = "crn:cdp:datahub:us-west-1:1234:cluster:dh-2";

    private static final String DL_CRN = "crn:cdp:datalake:us-west-1:1234:datalake:dl-1";

    @Mock
    private DatahubService datahubService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private ClusterAvailabilityValidator underTest;

    @Test
    void validateAllClustersAvailableWhenNoClusters() {
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of());
        when(datahubService.getStatusesByEnvironmentCrn(ENV_CRN)).thenReturn(new StackStatusV4Responses(Set.of()));

        assertThatCode(() -> underTest.validateAllClustersAvailable(ENV_CRN))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAllClustersAvailableWhenAllAvailable() {
        SdxClusterResponse dl = datalake(DL_CRN, SdxClusterStatusResponse.RUNNING);
        StackStatusV4Response dh = datahub(DH_CRN_1, Status.AVAILABLE);
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(dl));
        when(datahubService.getStatusesByEnvironmentCrn(ENV_CRN)).thenReturn(new StackStatusV4Responses(Set.of(dh)));

        assertThatCode(() -> underTest.validateAllClustersAvailable(ENV_CRN))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAllClustersAvailableWhenDataLakeNotRunning() {
        SdxClusterResponse dl = datalake(DL_CRN, SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS);
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(dl));
        when(datahubService.getStatusesByEnvironmentCrn(ENV_CRN)).thenReturn(new StackStatusV4Responses(Set.of()));

        assertThatThrownBy(() -> underTest.validateAllClustersAvailable(ENV_CRN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(DL_CRN)
                .hasMessageContaining("DATALAKE_UPGRADE_IN_PROGRESS")
                .hasMessageContaining("Cross-realm trust setup cannot be triggered");
    }

    @Test
    void validateAllClustersAvailableWhenDataHubNotAvailable() {
        StackStatusV4Response dh = datahub(DH_CRN_1, Status.UPDATE_IN_PROGRESS);
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of());
        when(datahubService.getStatusesByEnvironmentCrn(ENV_CRN)).thenReturn(new StackStatusV4Responses(Set.of(dh)));

        assertThatThrownBy(() -> underTest.validateAllClustersAvailable(ENV_CRN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(DH_CRN_1)
                .hasMessageContaining("UPDATE_IN_PROGRESS")
                .hasMessageContaining("Cross-realm trust setup cannot be triggered");
    }

    @Test
    void validateAllClustersAvailableWhenBothDataLakeAndDataHubNotAvailable() {
        SdxClusterResponse dl = datalake(DL_CRN, SdxClusterStatusResponse.REPAIR_IN_PROGRESS);
        StackStatusV4Response dh = datahub(DH_CRN_1, Status.UPDATE_FAILED);
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(dl));
        when(datahubService.getStatusesByEnvironmentCrn(ENV_CRN)).thenReturn(new StackStatusV4Responses(Set.of(dh)));

        assertThatThrownBy(() -> underTest.validateAllClustersAvailable(ENV_CRN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(DL_CRN)
                .hasMessageContaining("REPAIR_IN_PROGRESS")
                .hasMessageContaining(DH_CRN_1)
                .hasMessageContaining("UPDATE_FAILED");
    }

    @Test
    void validateAllClustersAvailableWhenMultipleDataHubsAndSomeNotAvailable() {
        StackStatusV4Response dh1 = datahub(DH_CRN_1, Status.AVAILABLE);
        StackStatusV4Response dh2 = datahub(DH_CRN_2, Status.STOP_IN_PROGRESS);
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of());
        when(datahubService.getStatusesByEnvironmentCrn(ENV_CRN)).thenReturn(new StackStatusV4Responses(Set.of(dh1, dh2)));

        assertThatThrownBy(() -> underTest.validateAllClustersAvailable(ENV_CRN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(DH_CRN_2)
                .hasMessageContaining("STOP_IN_PROGRESS")
                .hasMessageNotContaining(DH_CRN_1);
    }

    private SdxClusterResponse datalake(String crn, SdxClusterStatusResponse status) {
        SdxClusterResponse response = new SdxClusterResponse();
        response.setCrn(crn);
        response.setStatus(status);
        return response;
    }

    private StackStatusV4Response datahub(String crn, Status status) {
        StackStatusV4Response response = new StackStatusV4Response();
        response.setCrn(crn);
        response.setStatus(status);
        return response;
    }
}

