package com.sequenceiq.environment.environment.flow.deletion.handler.distrox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.util.PollingConfig;

@ExtendWith(MockitoExtension.class)
class DistroXDeleteServiceTest {

    @Mock
    private DistroXV1Endpoint distroXEndpoint;

    @InjectMocks
    private DistroXDeleteService underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void deleteDistroXClustersForEnvironmentNoDistroXFound() {
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(0)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        Environment environment = new Environment();
        environment.setName("envName");
        StackViewV4Responses responses = new StackViewV4Responses(Set.of());
        when(distroXEndpoint.list(any(), any())).thenReturn(responses);
        underTest.deleteDistroXClustersForEnvironment(pollingConfig, environment);
        verifyNoMoreInteractions(distroXEndpoint);
    }

    @Test
    void deleteDistroXClustersForEnvironment() {
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(10)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        Environment environment = new Environment();
        environment.setName("envName");

        StackViewV4Response distrox1 = new StackViewV4Response();
        distrox1.setCrn("crn1");
        StackViewV4Response distrox2 = new StackViewV4Response();
        distrox2.setCrn("crn2");

        when(distroXEndpoint.list(any(), any())).thenReturn(
                new StackViewV4Responses(Set.of(distrox1, distrox2)),
                new StackViewV4Responses(Set.of(distrox2)),
                new StackViewV4Responses(Set.of()));

        underTest.deleteDistroXClustersForEnvironment(pollingConfig, environment);

        ArgumentCaptor<DistroXMultiDeleteV1Request> captor = ArgumentCaptor.forClass(DistroXMultiDeleteV1Request.class);
        verify(distroXEndpoint).deleteMultiple(captor.capture(), eq(true));
        DistroXMultiDeleteV1Request multiDeleteRequest = captor.getValue();
        assertThat(multiDeleteRequest.getCrns()).hasSameElementsAs(Set.of("crn1", "crn2"));
        verifyNoMoreInteractions(distroXEndpoint);
    }

    @Test
    void deleteDistroXClustersForEnvironmentFail() {
        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(0)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(10)
                .withTimeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        Environment environment = new Environment();
        environment.setName("envName");

        StackViewV4Response distrox1 = new StackViewV4Response();
        distrox1.setCrn("crn1");
        StackViewV4Response distrox2 = new StackViewV4Response();
        distrox2.setCrn("crn2");
        distrox2.setStatus(Status.DELETE_FAILED);

        when(distroXEndpoint.list(any(), any())).thenReturn(
                new StackViewV4Responses(Set.of(distrox1, distrox2)),
                new StackViewV4Responses(Set.of(distrox2)),
                new StackViewV4Responses(Set.of(distrox2)));

        assertThatThrownBy(() -> underTest.deleteDistroXClustersForEnvironment(pollingConfig, environment))
                .isInstanceOf(UserBreakException.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        ArgumentCaptor<DistroXMultiDeleteV1Request> captor = ArgumentCaptor.forClass(DistroXMultiDeleteV1Request.class);
        verify(distroXEndpoint).deleteMultiple(captor.capture(), eq(true));
        DistroXMultiDeleteV1Request multiDeleteRequest = captor.getValue();
        assertThat(multiDeleteRequest.getCrns()).hasSameElementsAs(Set.of("crn1", "crn2"));
        verifyNoMoreInteractions(distroXEndpoint);
    }

}
