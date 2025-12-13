package com.sequenceiq.environment.environment.flow.deletion.handler.datahub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.util.PollingConfig;

@ExtendWith(MockitoExtension.class)
class DatahubDeletionServiceTest {

    private static final String ENV_CRN = "envCrn";

    private final DatahubService datahubService = mock(DatahubService.class);

    private final DatahubDeletionService underTest = new DatahubDeletionService(datahubService);

    @Test
    void deleteDatahubClustersForEnvironmentNoDatahubFound() {
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(0)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        Environment environment = new Environment();
        environment.setResourceCrn(ENV_CRN);
        StackViewV4Responses responses = new StackViewV4Responses(Set.of());
        when(datahubService.list(anyString())).thenReturn(responses);

        underTest.deleteDatahubClustersForEnvironment(pollingConfig, environment, true);

        verify(datahubService, never()).deleteMultiple(anyString(), any(), anyBoolean());
    }

    @Test
    void deleteDatahubClustersForEnvironment() {
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(10)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        Environment environment = new Environment();
        environment.setResourceCrn(ENV_CRN);

        StackViewV4Response distrox1 = new StackViewV4Response();
        distrox1.setCrn("crn1");
        StackViewV4Response distrox2 = new StackViewV4Response();
        distrox2.setCrn("crn2");

        when(datahubService.list(anyString())).thenReturn(
                new StackViewV4Responses(Set.of(distrox1, distrox2)),
                new StackViewV4Responses(Set.of(distrox2)),
                new StackViewV4Responses(Set.of()));

        underTest.deleteDatahubClustersForEnvironment(pollingConfig, environment, true);

        ArgumentCaptor<DistroXMultiDeleteV1Request> captor = ArgumentCaptor.forClass(DistroXMultiDeleteV1Request.class);
        verify(datahubService).deleteMultiple(anyString(), captor.capture(), eq(true));
        DistroXMultiDeleteV1Request multiDeleteRequest = captor.getValue();
        assertThat(multiDeleteRequest.getCrns()).hasSameElementsAs(Set.of("crn1", "crn2"));
    }

    @Test
    void deleteDatahubClustersForEnvironmentFail() {
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(10)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        Environment environment = new Environment();
        environment.setResourceCrn(ENV_CRN);

        StackViewV4Response datahub1 = new StackViewV4Response();
        datahub1.setCrn("crn1");
        StackViewV4Response datahub2 = new StackViewV4Response();
        datahub2.setCrn("crn2");
        datahub2.setStatus(Status.DELETE_FAILED);

        when(datahubService.list(anyString())).thenReturn(
                new StackViewV4Responses(Set.of(datahub1, datahub2)),
                new StackViewV4Responses(Set.of(datahub2)),
                new StackViewV4Responses(Set.of(datahub2)));

        assertThatThrownBy(() -> underTest.deleteDatahubClustersForEnvironment(pollingConfig, environment, true))
                .isInstanceOf(UserBreakException.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        ArgumentCaptor<DistroXMultiDeleteV1Request> captor = ArgumentCaptor.forClass(DistroXMultiDeleteV1Request.class);
        verify(datahubService).deleteMultiple(anyString(), captor.capture(), eq(true));
        DistroXMultiDeleteV1Request multiDeleteRequest = captor.getValue();
        assertThat(multiDeleteRequest.getCrns()).hasSameElementsAs(Set.of("crn1", "crn2"));
    }

}
