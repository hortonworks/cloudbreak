package com.sequenceiq.cloudbreak.service.rotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.SecretTypeListService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretTypeResponse;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class DatahubPeriodicSecretRotationServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:acc-12345:cluster:my-dh";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env";

    @Mock
    private StackService stackService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private SecretTypeListService secretTypeListService;

    @Mock
    private List<SecretType> enabledSecretTypes;

    @Mock
    private StackRotationService stackRotationService;

    @InjectMocks
    private DatahubPeriodicSecretRotationService underTest;

    private Stack stack;

    private StackDto stackDto;

    @BeforeEach
    void setup() {
        stack = mock(Stack.class);
        lenient().when(stack.getId()).thenReturn(101L);
        lenient().when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        stackDto = mock(StackDto.class);
        lenient().when(stackDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        lenient().when(stackDto.getId()).thenReturn(101L);
        lenient().when(stackDto.getStatus()).thenReturn(Status.AVAILABLE);
    }

    @Test
    void listJobResourcesDelegatesToStackService() {
        JobResource jr = mock(JobResource.class);
        when(jr.getRemoteResourceId()).thenReturn(RESOURCE_CRN);
        when(stackService.getAllAliveDatahubs(anySet())).thenReturn(List.of(jr));

        List<JobResource> result = underTest.listJobResources();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRemoteResourceId()).isEqualTo(RESOURCE_CRN);
        verify(stackService).getAllAliveDatahubs(eq(Status.getUnschedulableStatuses()));
    }

    @Test
    void getMdcContextInfoProviderReturnsStackWhenPresent() {
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        assertThat(underTest.getMdcContextInfoProvider(RESOURCE_CRN)).isPresent();
    }

    @Test
    void isSchedulableTrueWhenAvailableAndNoFlowRunning() {
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(flowLogService.isOtherFlowRunning(101L)).thenReturn(false);
        assertThat(underTest.isSchedulable(RESOURCE_CRN)).isTrue();
    }

    @Test
    void isSchedulableFalseWhenFlowRunning() {
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(flowLogService.isOtherFlowRunning(101L)).thenReturn(true);
        assertThat(underTest.isSchedulable(RESOURCE_CRN)).isFalse();
    }

    @Test
    void isSchedulableFalseWhenNotAvailable() {
        when(stackDto.getStatus()).thenReturn(Status.STOPPED);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(flowLogService.isOtherFlowRunning(101L)).thenReturn(false);
        assertThat(underTest.isSchedulable(RESOURCE_CRN)).isFalse();
    }

    @Test
    void listRotatableSecretNamesReturnsSecretNames() {
        DistroXSecretTypeResponse r1 = new DistroXSecretTypeResponse("SALT_PASSWORD", "d", "n", 0L);
        DistroXSecretTypeResponse r2 = new DistroXSecretTypeResponse("COMPUTE_MONITORING_CREDENTIALS", "d2", "n2", 0L);
        when(secretTypeListService.listRotatableSecretType(eq(RESOURCE_CRN), any())).thenReturn(List.of(r1, r2));

        List<String> names = underTest.listRotatableSecretNames(RESOURCE_CRN);

        assertThat(names).containsExactlyInAnyOrder("SALT_PASSWORD", "COMPUTE_MONITORING_CREDENTIALS");
    }

    @Test
    void enabledSecretTypesReturnsInjected() {
        assertThat(underTest.enabledSecretTypes()).isSameAs(enabledSecretTypes);
    }

    @Test
    void triggerRotationDelegatesToStackRotationService() {
        List<String> due = List.of("SALT_PASSWORD", "COMPUTE_MONITORING_CREDENTIALS");
        underTest.triggerRotation(RESOURCE_CRN, due);
        verify(stackRotationService).rotateSecrets(eq(RESOURCE_CRN), eq(due), eq(null), eq(null));
    }

    @Test
    void getResourceCreationDateDelegates() {
        Instant created = Instant.now().minus(Duration.ofDays(7));
        when(stackService.getCreatedByResourceCrn(eq(RESOURCE_CRN))).thenReturn(created);
        assertThat(underTest.getResourceCreationDate(RESOURCE_CRN)).isEqualTo(created);
        verify(stackService).getCreatedByResourceCrn(eq(RESOURCE_CRN));
    }
}

